package com.github.orbyfied.minem;

import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.protocol.*;
import com.github.orbyfied.minem.security.SymmetricEncryptionProfile;
import com.github.orbyfied.minem.util.ByteBuf;
import com.github.orbyfied.minem.util.Memory;
import com.github.orbyfied.minem.util.ProtocolIO;
import lombok.Getter;
import slatepowered.veru.misc.Throwables;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Represents a simple, modular (component based) Minecraft client.
 */
public class MinecraftClient extends ProtocolContext implements PacketSource, PacketSink {

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

    Socket socket;                                 // The socket connection
    Thread readerThread;                           // The connection reader thread

    ClientState state = ClientState.NOT_CONNECTED; // The current client state

    int compressionLevel = 2;                      // The compression level to use
    int compressionThreshold = -1;                 // The connection threshold agreed upon with the server, -1 if no compression
    SymmetricEncryptionProfile encryption;         // The current encryption configuration/engine

    final Deflater deflater = new Deflater();
    final Inflater inflater = new Inflater();

    // used by send(Packet) synchronized on deflater
    byte[] compressedBytes = new byte[1024 * 16];

    /* Client Events */
    final Chain<ClientStateSwitchHandler> onStateSwitch = new Chain<>(ClientStateSwitchHandler.class);

    /* Connection Events */
    final Chain<PacketHandler> onPacket = new Chain<>(PacketHandler.class);
    final Chain<PacketHandler> onPacketSink = new Chain<>(PacketHandler.class);
    final Chain<PacketHandler> onPacketReceived = new Chain<>(PacketHandler.class);
    AtomicInteger countReceived = new AtomicInteger();
    AtomicInteger countSent = new AtomicInteger();

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

    // Switch the current client state
    private void switchState(ClientState state) {
        var old = this.state;
        this.state = state;
        onStateSwitch.invoker().onStateSwitch(old, state);
    }

    // Reset the client state to default
    private void resetState() {
        switchState(ClientState.NOT_CONNECTED);
        this.compressionThreshold = -1;
        this.compressionLevel = 2;
        this.encryption = null;
    }

    private OutputStream socketOutput;
    private InputStream socketInput;
    private OutputStream encryptedStream;
    private InputStream decryptedStream;

    private OutputStream getOutputStream() {
        try {
            if (socketOutput == null) {
                socketOutput = socket.getOutputStream();
            }

            return encryption != null ?
                    (encryptedStream != null ? encryptedStream : (encryptedStream = encryption.encryptingOutputStream(socketOutput))) :
                    socketOutput;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            throw new AssertionError();
        }
    }

    private InputStream getInputStream() {
        try {
            if (socketInput == null) {
                socketInput = socket.getInputStream();
            }

            return encryption != null ?
                    (decryptedStream != null ? decryptedStream : (decryptedStream = encryption.decryptingInputStream(socketInput))) :
                    socketInput;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            throw new AssertionError();
        }
    }

    /**
     * Connect the client to the given address.
     *
     * @param address The address.
     * @return The future.
     */
    public CompletableFuture<MinecraftClient> connect(SocketAddress address) {
        try {
            // connect to the server
            socket = new Socket();
            socket.connect(address);

            // start packet reader thread
            if (readerThread == null) {
                readerThread = new Thread(this::runReadThread, "MinecraftClient.readerThread");
                readerThread.setDaemon(true);
            }

            readerThread.start();

            return CompletableFuture.completedFuture(this);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to connect MinecraftClient", ex);
        }
    }

    @Override
    public boolean isOpen() {
        return socket.isConnected();
    }

    @Override
    public void close() {
        try {
            socket.close();
            resetState();
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
        }
    }

    @Override
    public Chain<PacketHandler> onPacket() {
        return onPacket;
    }

    @Override
    public int countSent() {
        return countSent.get();
    }

    // get or create a byte buffer for writing packets
    private ByteBuf getWriteBuffer() {
        // todo: maybe pool these or something idk
        return ByteBuf.create(10);
    }

    @Override
    public void sendSync(Packet packet) {
        try {
            packet.set(Packet.OUTBOUND);

            // call events
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

            // write packet type and data
            buf.writeVarInt(packet.getNetworkId());
            packet.getMapping().writePacketData(packet, buf);
            int dataLength = buf.remainingWritten();

            if (compressionThreshold == -1 || dataLength < compressionThreshold) {
                if (compressionThreshold != -1) {
                    // write full packet length
                    int totalPacketSize = 1 + dataLength;                    // length of dataLength + dataLength
                    ProtocolIO.writeVarIntToStream(stream, totalPacketSize); // write the total packet size (VarInt)
                    stream.write(0);                                       // write dataLength (constant 0 bc uncompressed)
                } else {
                    // write length directly, then write the buffer
                    ProtocolIO.writeVarIntToStream(stream, dataLength);
                }

                buf.readTo(stream, dataLength); // write uncompressed data
            } else {
                synchronized (deflater) {
                    this.compressedBytes = Memory.ensureByteArrayCapacity(this.compressedBytes, buf.remainingWritten());

                    deflater.finish();
                    int compressedSize = deflater.deflate(compressedBytes);
                    deflater.end();

                    int dataLengthSize = ProtocolIO.lengthVarInt(dataLength);
                    ProtocolIO.writeVarIntToStream(stream, compressedSize + dataLengthSize);
                    ProtocolIO.writeVarIntToStream(stream, dataLength);
                    stream.write(compressedBytes);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("An exception occurred while sending packet\n" + DebugUtils.debugInfo(packet), ex);
        }
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

    // run() for the connection read thread
    private void runReadThread() {
        try {
            byte[] compressedPacket = new byte[1024 * 16];
            ByteBuf buf = ByteBuf.create(1024);

            while (!socket.isClosed() && socket.isConnected()) {
                InputStream stream = getInputStream();
                buf.clear();

                // check if compression is set, then decide
                // how to read packet and data length
                if (compressionThreshold != -1) {
                    int packetLength = ProtocolIO.readVarIntFromStream(stream);
                    Memory.ensureByteArrayCapacity(compressedPacket, packetLength);
                    int bytes = stream.read(compressedPacket);
                    assert packetLength == bytes;

                    int decompressedDataLength = ProtocolIO.readVarIntFromBytes(compressedPacket, 0);
                    synchronized (inflater) {
                        buf.ensureWriteCapacity(decompressedDataLength);
                        int vil = ProtocolIO.lengthVarInt(decompressedDataLength);
                        inflater.setInput(compressedPacket, vil, packetLength - vil);
                        inflater.inflate(buf.nioReference());
                        inflater.end();
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

                Packet packet = mapping.createPacketContainer(this);
                mapping.readPacketData(packet, buf);
                packet.set(Packet.INBOUND);

                // call event chains
                onPacketReceived.invoker().onPacket(packet);
                if (packet.check(Packet.CANCEL)) {
                    continue;
                }

                onPacket.invoker().onPacket(packet);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Event Handler: called when the client switches state
    public interface ClientStateSwitchHandler {
        void onStateSwitch(ClientState oldState, ClientState newState);
    }

}
