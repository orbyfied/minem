package com.github.orbyfied.minem;

import com.github.orbyfied.minem.concurrent.FastThreadLocal;
import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.event.MultiChain;
import com.github.orbyfied.minem.exception.ClientConnectException;
import com.github.orbyfied.minem.exception.ClientReadException;
import com.github.orbyfied.minem.protocol.login.ClientboundLoginDisconnectPacket;
import com.github.orbyfied.minem.protocol.login.ClientboundSetCompressionPacket;
import com.github.orbyfied.minem.protocol.handshake.ServerboundHandshakePacket;
import com.github.orbyfied.minem.protocol.play.ClientboundDisconnectPacket;
import com.github.orbyfied.minem.protocol.play.ClientboundKeepAlivePacket;
import com.github.orbyfied.minem.protocol.play.ServerboundKeepAlivePacket;
import com.github.orbyfied.minem.protocol.*;
import com.github.orbyfied.minem.buffer.ByteBuf;
import com.github.orbyfied.minem.buffer.Memory;
import com.github.orbyfied.minem.io.ProtocolIO;
import com.github.orbyfied.minem.protocol.UnknownPacket;
import com.github.orbyfied.minem.util.ClientDebugUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import slatepowered.veru.misc.Throwables;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Represents a simple, modular (component based) Minecraft client.
 */
public class MinecraftClient extends ProtocolContext implements PacketSource, PacketSink {

    ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * The protocol to use.
     */
    @Getter
    Protocol protocol;

    /**
     * All registered components mapped by class, superclasses and interfaces.
     */
    final Map<Class<?>, ClientComponent> componentMap = new HashMap<>();

    /**
     * The list of all attached components.
     */
    final List<ClientComponent> componentList = new ArrayList<>();

    Socket socket;                                          // The socket connection
    Thread readerThread;                                    // The connection reader thread

    boolean isResetState = true;                            // Whether the current state of the client is clear
    volatile ClientState lastState = null;                  // The previous client state
    volatile ClientState state = ClientState.NOT_CONNECTED; // The current client state

    final AtomicBoolean active = new AtomicBoolean(false);
    final AtomicBoolean readActive = new AtomicBoolean(false);

    int compressionLevel = 2;         // The compression level to use
    int compressionThreshold = -1;    // The connection threshold agreed upon with the server, -1 if no compression
    volatile Cipher decryptionCipher; // The decryption cipher if encryption is enabled
    volatile Cipher encryptionCipher; // The encryption cipher if encryption is enabled

    final Deflater deflater = new Deflater();
    final Inflater inflater = new Inflater();

    // Used by send(Packet) synchronized on deflater
    byte[] compressedBytes = new byte[1024 * 16];

    // The pool of write buffers
    FastThreadLocal<ByteBuf> writeBufferPool = new FastThreadLocal<>();

    /* Client Events */
    final Chain<ClientStateSwitchHandler> onStateSwitch = new Chain<>(ClientStateSwitchHandler.class);
    final Chain<ClientDisconnectHandler> onDisconnect = new Chain<>(ClientDisconnectHandler.class);

    /* Connection Events */
    final Chain<PacketHandler> onPacket = new Chain<>(PacketHandler.class).integerFlagHandling();
    final Chain<PacketHandler> onPacketSink = new Chain<>(PacketHandler.class).integerFlagHandling();
    final Chain<PacketHandler> onPacketReceived = new Chain<>(PacketHandler.class).integerFlagHandling();
    AtomicInteger countReceived = new AtomicInteger();
    AtomicInteger countSent = new AtomicInteger();

    /* Multiplexed Connection Events */
    final MultiChain<String, PacketHandler> onTypedSent = new MultiChain<>(k -> new Chain<>(PacketHandler.class).integerFlagHandling());
    final MultiChain<String, PacketHandler> onTypedReceived = new MultiChain<>(k -> new Chain<>(PacketHandler.class).integerFlagHandling());

    private OutputStream socketOutput;
    private InputStream socketInput;
    private OutputStream encryptedStream;
    private InputStream decryptedStream;

    public MinecraftClient executor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    public MinecraftClient protocol(Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    // Register the given component for the given class and superclasses recursively.
    private void registerComponentClass(Class<?> kl, ClientComponent component) {
        componentMap.put(kl, component);
        if (kl.getSuperclass() != null && kl.getSuperclass() != Object.class) {
            registerComponentClass(kl.getSuperclass(), component);
        }

        for (Class<?> itf : kl.getInterfaces()) {
            registerComponentClass(itf, component);
        }
    }

    /**
     * Attach the given component to this client.
     *
     * @param component The component.
     * @return This.
     */
    public MinecraftClient with(ClientComponent component) {
        Objects.requireNonNull(component, "Component can not be null");
        if (component.attach0(this)) {
            componentList.add(component);
            registerComponentClass(component.getClass(), component);
        }

        return this;
    }

    /**
     * Find a component by the given type if present.
     *
     * @param cClass The runtime type.
     * @param <C> The type.
     * @return The component or null if absent.
     */
    @SuppressWarnings("unchecked")
    public <C extends ClientComponent> C find(Class<C> cClass) {
        return (C) componentMap.get(cClass);
    }

    // Switch the current client state
    public void switchState(ClientState state) {
        this.lastState = this.state;
        this.state = state;
        onStateSwitch.invoker().onStateSwitch(this.lastState, state);
    }

    public ClientState getState() {
        return state;
    }

    public ClientState getLastState() {
        return lastState;
    }

    /**
     * Connect the client to the given address.
     *
     * @param address The address.
     * @return The future.
     */
    public CompletableFuture<MinecraftClient> connect(InetSocketAddress address) {
        resetState();
        return CompletableFuture.supplyAsync(() -> {
            try {
                active.set(true);
                synchronized (this) {
                    // connect to the server
                    socket = new Socket();
                    socket.connect(address);

                    // start packet reader thread
                    if (readerThread == null) {
                        readerThread = new Thread(this::runReadThread, "MinecraftClient.readerThread");
                        readerThread.setDaemon(true);
                    }

                    readerThread.start();
                }

                // initiate handshake
                switchState(ClientState.HANDSHAKE);
                sendSync(createPacket("ServerboundHandshake",
                        new ServerboundHandshakePacket(protocol.getProtocolNumber(), address.getHostString(),
                                (short) address.getPort(), ServerboundHandshakePacket.NextState.LOGIN)));
                switchState(ClientState.LOGIN);

                updateReadActive(true);

                // now we wait for components to complete login...

                return this;
            } catch (Exception ex) {
                ClientConnectException clientConnectException = new ClientConnectException("Failed to connect to " + address, ex);
                disconnect(DisconnectReason.ERROR, clientConnectException);
                throw clientConnectException;
            }
        }, executor);
    }

    @Override
    public synchronized boolean isOpen() {
        return active.get();
    }

    @Override
    public synchronized boolean close() {
        if (!active.get()) {
            return false;
        }

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
        } finally {
            active.set(false);
            updateReadActive(false);

            writeBufferPool.forEach((integer, byteBuf) -> {
                byteBuf.free();
            });

            switchState(ClientState.NOT_CONNECTED);
        }

        return true;
    }

    public Packet createPacket(String name, Object data) {
        return protocol
                .forPhase(state.phase)
                .getPacketMapping(name)
                .createPacketContainer(this)
                .withData(data);
    }

    @Override
    public void sendSync(Packet packet) {
        try {
            packet.set(Packet.OUTBOUND);

            // call events
            var h = onTypedSent.orNull(packet.getMapping().getPrimaryName());
            if (h != null) {
                h.invoker().onPacket(packet);
                if (packet.check(Packet.CANCEL)) {
                    return;
                }
            }

            onPacketSink.invoker().onPacket(packet);
            if (packet.check(Packet.CANCEL)) {
                return;
            }

            onPacket.invoker().onPacket(packet);
            if (packet.check(Packet.CANCEL)) {
                return;
            }

            OutputStream stream = getOutputStream();
            ByteBuf buf = getWriteBuffer();
            buf.reset();

            // write packet type and data
            buf.writeVarInt(packet.getNetworkId());
            packet.getMapping().writePacketData(packet, buf);
            int dataLength = buf.remainingWritten();

            if (compressionThreshold == -1 || dataLength < compressionThreshold) {
                // uncompressed
                if (compressionThreshold != -1) {
                    // uncompressed, below threshold
                    // write full packet length
                    int totalPacketSize = 1 + dataLength;                    // length of dataLength + dataLength
                    ProtocolIO.writeVarIntToStream(stream, totalPacketSize); // write the total packet size (VarInt)
                    stream.write(0);                                       // write 0 for dataLength (0 bc uncompressed)
                } else {
                    // uncompressed, no compression set
                    // write length directly, then write the buffer
                    ProtocolIO.writeVarIntToStream(stream, dataLength);
                }

                int written = buf.readTo(stream, dataLength); // write uncompressed data
            } else {
                // compressed, set and above threshold
                synchronized (deflater) {
                    this.compressedBytes = Memory.ensureByteArrayCapacity(this.compressedBytes, buf.remainingWritten());

                    deflater.reset();
                    deflater.setInput(buf.nioReference());
                    int compressedSize = deflater.deflate(compressedBytes);
                    deflater.finish();

                    int dataLengthSize = ProtocolIO.lengthVarInt(dataLength);
                    ProtocolIO.writeVarIntToStream(stream, compressedSize + dataLengthSize);
                    ProtocolIO.writeVarIntToStream(stream, dataLength);
                    stream.write(compressedBytes);
                }
            }

            stream.flush();
        } catch (Exception ex) {
            throw new RuntimeException("An exception occurred while sending packet\n  " + ClientDebugUtils.debugInfo(packet), ex);
        }
    }

    // run() for the connection read thread
    private void runReadThread() {
        ByteBuf buf = ByteBuf.create(1024);
        Packet packet; // for release of any resources

        Packet unknownPacketContainer = UnknownPacket.CLIENTBOUND_MAPPING.createPacketContainer(this);
        UnknownPacket unknownPacket = new UnknownPacket();
        unknownPacketContainer.source(this);
        unknownPacketContainer.set(Packet.INBOUND);
        unknownPacketContainer.withData(unknownPacket);

        try {
            while (active.get()) {
                if (!readActive.get()) {
                    synchronized (readActive) {
                        readActive.wait();
                    }

                    continue;
                }

                byte[] compressedPacket = new byte[1024 * 16];

                while (!socket.isClosed() && socket.isConnected()) {
                    InputStream stream = getInputStream();
                    buf.reset();

                    // check if compression is set, then decide
                    // how to read packet and data length
                    if (compressionThreshold != -1) {
                        int packetLength = ProtocolIO.readVarIntFromStream(stream);
                        int decompressedDataLength = ProtocolIO.readVarIntFromStream(stream);
                        int viSizeDataLength = ProtocolIO.lengthVarInt(decompressedDataLength);
                        int compressedDataLength = packetLength - viSizeDataLength;

                        if (decompressedDataLength == 0) {
                            // uncompressed packet
                            buf.writeFrom(stream, compressedDataLength);
                        } else {
                            // compressed packet
                            compressedPacket = Memory.ensureByteArrayCapacity(compressedPacket, compressedDataLength);
                            int readDataLength = 0;
                            while (readDataLength < compressedDataLength) {
                                readDataLength += stream.read(compressedPacket, readDataLength, compressedDataLength - readDataLength);
                            }

//                            System.err.println("COMPRESSED READ | total length: " + packetLength + ", decompressedLengthSent: " + decompressedDataLength + ", " +
//                                    "viSizeDataLength: " + viSizeDataLength + ", readData(compressed): " + readDataLength + ", expectedCompressedDataLength: " +
//                                    compressedDataLength);

                            synchronized (inflater) {
                                buf.ensureWriteCapacity(decompressedDataLength);
                                inflater.reset();
                                inflater.setInput(compressedPacket, 0, compressedDataLength);
                                inflater.inflate(buf.nioReference());
                            }
                        }
                    } else {
                        int dataLength = ProtocolIO.readVarIntFromStream(stream);
                        buf.writeFrom(stream, dataLength);
                    }

                    ProtocolPhase phase = this.state.getPhase();
                    int packetID = buf.readVarInt();
                    PacketMapping mapping = protocol
                            .forPhase(phase)
                            .getClientboundPacketMapping(packetID);
                    if (mapping != null) {
                        packet = mapping.createPacketContainer(this);
                        mapping.readPacketData(packet, buf);

                        packet.source(this);
                        packet.set(Packet.INBOUND);
                    } else {
                        // create unknown packet
                        mapping = UnknownPacket.CLIENTBOUND_MAPPING;
                        packet = unknownPacketContainer;
                        unknownPacket.buffer(buf);
                        packet.networkId = packetID;
                        packet.phase = phase;
                        packet.clear(Packet.CANCEL);
                    }

                    // call event chains
                    var h = onTypedReceived.orNull(packet.getMapping().getPrimaryName());
                    if (h != null) {
                        h.invoker().onPacket(packet);
                        if (packet.check(Packet.CANCEL)) {
                            continue;
                        }
                    }

                    onPacketReceived.invoker().onPacket(packet);
                    if (packet.check(Packet.CANCEL)) {
                        continue;
                    }

                    onPacket.invoker().onPacket(packet);

                    // release buffer from packet after being handled
                    if (packet.isUnknown()) {
                        unknownPacket.buffer(null);
                    }
                }
            }
        } catch (InterruptedException ex) {
            disconnect(DisconnectReason.ERROR, ex);
        } catch (Exception ex) {
            disconnect(DisconnectReason.ERROR, new ClientReadException(ex));
        } finally {
            buf.free();

            unknownPacket.buffer(null);
        }
    }

    // Reset the client state to default
    private synchronized void resetState() {
        if (isResetState) {
            return;
        }

        active.set(false);
        readActive.set(false);

        this.state = ClientState.NOT_CONNECTED;
        this.compressionThreshold = -1;
        this.compressionLevel = 2;
        this.encryptionCipher = null;
        this.decryptionCipher = null;
        for (ClientComponent component : this.componentList) {
            component.resetState();
        }

        this.isResetState = true;
    }


    // Get the current output stream to use
    private OutputStream getOutputStream() {
        try {
            if (socketOutput == null) {
                socketOutput = socket.getOutputStream();
            }

            return encryptionCipher != null ?
                    (encryptedStream != null ? encryptedStream : (encryptedStream = new CipherOutputStream(socketOutput, encryptionCipher))) :
                    socketOutput;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            throw new AssertionError();
        }
    }

    // Get the current input stream to use
    private InputStream getInputStream() {
        try {
            if (socketInput == null) {
                socketInput = socket.getInputStream();
            }

            return decryptionCipher != null ?
                    (decryptedStream != null ? decryptedStream : (decryptedStream = new CipherInputStream(socketInput, decryptionCipher))) :
                    socketInput;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            throw new AssertionError();
        }
    }

    // Update the read active flag
    private void updateReadActive(boolean v) {
        synchronized (readActive) {
            readActive.set(v);
            readActive.notifyAll();
        }
    }

    /**
     * Register the given encryption cipher to this client.
     */
    public synchronized void withCiphers(Cipher encryption, Cipher decryption) {
        this.encryptionCipher = encryption;
        this.decryptionCipher = decryption;
        encryptedStream = null;
        decryptedStream = null;
    }

    @Override
    public Chain<PacketHandler> onPacketSink() {
        return onPacketSink;
    }

    @Override
    public int countReceived() {
        return countReceived.get();
    }

    @Override
    public Chain<PacketHandler> onPacketReceived() {
        return onPacketReceived;
    }

    public Chain<ClientStateSwitchHandler> onStateSwitch() {
        return onStateSwitch;
    }

    @Override
    public Chain<PacketHandler> onPacket() {
        return onPacket;
    }

    public MultiChain<String, PacketHandler> onTypedReceived() {
        return onTypedReceived;
    }

    public MultiChain<String, PacketHandler> onTypedSent() {
        return onTypedSent;
    }

    @Override
    public int countSent() {
        return countSent.get();
    }

    public Chain<ClientDisconnectHandler> onDisconnect() {
        return onDisconnect;
    }

    // get or create a byte buffer for writing packets
    private ByteBuf getWriteBuffer() {
        return writeBufferPool.getOrCompute(() -> ByteBuf.create(1024));
    }

    /**
     * Disconnects the client with the given reason, if connected.
     *
     * @param reason The reason.
     * @param details The detailed reason.
     * @return Whether it disconnected the client.
     */
    public synchronized boolean disconnect(DisconnectReason reason,
                                           Object details) {
        if (!close()) {
            return false;
        }

        try {
            onDisconnect.invoker().onDisconnect(this, reason, details);
        } catch (Exception ex) {
            System.err.println("Error while invoking disconnect handlers");
            ex.printStackTrace();
        }

        return true;
    }

    // Event Handler: called when the client switches state
    public interface ClientStateSwitchHandler {
        void onStateSwitch(ClientState oldState, ClientState newState);
    }

    // The reason for a disconnect
    @RequiredArgsConstructor
    @Getter
    public enum DisconnectReason {
        /** A client side error. */
        ERROR(Throwable.class),

        /** The remote host/server issued a disconnect with the given text component reason. */
        ISSUED(Component.class),

        /** Disconnect was forced by local code */
        FORCE(null)

        ;
        final Class<?> detailsType;
    }

    // Event Handler: called when the client disconnects
    public interface ClientDisconnectHandler {
        void onDisconnect(MinecraftClient client, DisconnectReason reason, Object details);
    }

    {
        /*
            Base Packet Handlers
         */

        onPacketReceived.addFirst(packetContainer -> {
            if (packetContainer.data() instanceof ClientboundLoginDisconnectPacket packet) {
                disconnect(DisconnectReason.ISSUED, LegacyComponentSerializer.legacySection().deserialize(packet.getReason()));
                close();
                return 0;
            }

            if (packetContainer.data() instanceof ClientboundDisconnectPacket packet) {
                disconnect(DisconnectReason.ISSUED, packet.getReason());
                close();
                return 0;
            }

            if (packetContainer.data() instanceof ClientboundSetCompressionPacket packet) {
                synchronized (deflater) {
                    this.compressionThreshold = packet.getThreshold();
                }
            }

            if (packetContainer.data() instanceof ClientboundKeepAlivePacket packet) {
                sendSync(createPacket("ServerboundKeepAlive", new ServerboundKeepAlivePacket(packet.getId())));
            }

            return 0;
        });
    }

}
