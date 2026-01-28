package com.orbyfied.minem.network;

import com.orbyfied.minem.MinecraftClient;
import com.orbyfied.minem.buffer.Memory;
import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.client.DisconnectReason;
import com.orbyfied.minem.concurrent.FastThreadLocal;
import com.orbyfied.minem.event.Chain;
import com.orbyfied.minem.event.ExceptionEventHandler;
import com.orbyfied.minem.event.ExceptionEventSource;
import com.orbyfied.minem.exception.ClientReadException;
import com.orbyfied.minem.io.ProtocolIO;
import com.orbyfied.minem.protocol.*;
import com.orbyfied.minem.util.ClientDebugUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.veru.misc.Throwables;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * The client network channel used by {@link MinecraftClient} to
 * send and receive packets.
 */
@RequiredArgsConstructor
public class ProtocolConnection extends ProtocolContext implements PacketSink, PacketSource, ExceptionEventSource {

    final NetworkManager networkManager;
    final MinecraftClient client;

    @Getter
    protected Socket socket; // The actual socket connection

    final AtomicBoolean readActive = new AtomicBoolean(false);
    Thread readThread; // The connection reader thread

    private OutputStream socketOutput;
    private InputStream socketInput;
    private OutputStream encryptingStream;
    private InputStream decryptedStream;

    // Used by send(Packet) synchronized on deflater
    byte[] compressedSendingBytes = new byte[1024 * 16];

    // The pool of write buffers
    FastThreadLocal<UnsafeByteBuf> writeBufferPool = new FastThreadLocal<>();

    /* Compression */
    int compressionLevel = 2;         // The compression level to use
    int compressionThreshold = -1;    // The connection threshold agreed upon with the server, -1 if no compression
    volatile Cipher decryptionCipher; // The decryption cipher if encryption is enabled
    volatile Cipher encryptionCipher; // The encryption cipher if encryption is enabled

    final Deflater deflater = new Deflater();
    final Inflater inflater = new Inflater();

    /* Events */
    AtomicInteger countReceived = new AtomicInteger();
    AtomicInteger countSent = new AtomicInteger();

    @Override
    public Protocol getProtocol() {
        return client.getProtocol();
    }

    @Override
    public synchronized boolean isOpen() {
        return !socket.isClosed();
    }

    @Override
    public synchronized boolean close() {
        if (!isOpen()) {
            return false;
        }

        try {
            readActive.set(false);
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
        } finally {
            writeBufferPool.forEach((integer, byteBuf) -> {
                byteBuf.free();
            });
        }

        return true;
    }

    @Override
    public Chain<PacketHandler> onPacket() {
        return client.onPacket();
    }

    @Override
    public int countSent() {
        return countSent.get();
    }

    /**
     * Initialize and start the read thread.
     */
    public synchronized void start() {
        // create the read thread if needed
        if (readThread == null) {
            readThread = new Thread(this::runReadThread, "ProtocolConnectionBlockingWorker");
        }

        readThread.start();

        // set the read activity
        synchronized (readActive) {
            readActive.set(true);
            readActive.notifyAll();
        }
    }

    /**
     * Register the given encryption cipher to this connection.
     */
    public synchronized void withCiphers(Cipher encryption, Cipher decryption) {
        this.encryptionCipher = encryption;
        this.decryptionCipher = decryption;
        encryptingStream = null;
        decryptedStream = null;
    }

    /**
     * Synchronously serialize and send the given packet to the server.
     *
     * @param packet The packet.
     */
    @Override
    public void sendSync(PacketContainer packet) {
        try {
            packet.set(PacketContainer.OUTBOUND);

            // call events
            var h = client.onTypedSent().orNull(packet.getMapping());
            if (h != null) {
                h.invoker().onPacket(packet);
                if (packet.check(PacketContainer.CANCEL)) {
                    return;
                }
            }

            client.onPacketSink().invoker().onPacket(packet);
            if (packet.check(PacketContainer.CANCEL)) {
                return;
            }

            client.onPacket().invoker().onPacket(packet);
            if (packet.check(PacketContainer.CANCEL)) {
                return;
            }

            // prepare IO/buffers
            OutputStream stream = getOutputStream();
            UnsafeByteBuf buf = getWriteBuffer();
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
                    stream.write(0);                                      // write 0 for dataLength (0 bc uncompressed)
                } else {
                    // uncompressed, no compression set
                    // write length directly, then write the buffer
                    ProtocolIO.writeVarIntToStream(stream, dataLength);
                }

                int written = buf.readTo(stream, dataLength); // write uncompressed data
            } else {
                // compressed, set and above threshold
                synchronized (deflater) {
                    this.compressedSendingBytes = Memory.ensureByteArrayCapacity(this.compressedSendingBytes, buf.remainingWritten() * 2);

                    deflater.reset();
                    deflater.setInput(buf.nioReference());
                    int compressedSize = deflater.deflate(compressedSendingBytes);
                    deflater.finish();

                    int dataLengthSize = ProtocolIO.lengthVarInt(dataLength);
                    ProtocolIO.writeVarIntToStream(stream, compressedSize + dataLengthSize);
                    ProtocolIO.writeVarIntToStream(stream, dataLength);
                    stream.write(compressedSendingBytes);
                }
            }

            stream.flush();
        } catch (Exception ex) {
            RuntimeException ex2 = new RuntimeException("An exception occurred while sending packet\n  " + ClientDebugUtils.debugInfo(packet), ex);
            client.disconnect(DisconnectReason.ERROR, ex2);
            throw ex2;
        }
    }

    // run() for the connection read thread
    private void runReadThread() {
        UnsafeByteBuf buf = UnsafeByteBuf.createDirect(1024);
        byte[] compressedReadPacket = new byte[1024 * 16];

        /* Pooled Instances */
        PacketContainer unknownPacketContainer = UnknownPacket.CLIENTBOUND_MAPPING.createPacketContainerWithData(this);
        UnknownPacket unknownPacketData = new UnknownPacket();
        unknownPacketContainer.source(this);
        unknownPacketContainer.set(PacketContainer.INBOUND);
        unknownPacketContainer.withData(unknownPacketData);

        PacketContainer packet; // for release of any resources

        try {
            while (client.isActive()) {
                if (!readActive.get()) {
                    synchronized (readActive) {
                        readActive.wait();
                    }

                    continue;
                }

                while (!socket.isClosed() && socket.isConnected()) {
                    try {
                        InputStream stream = getInputStream();

                        // check if compression is set, then decide
                        // how to read packet and data length
                        if (compressionThreshold != -1) {
                            int packetLength = ProtocolIO.readVarIntFromStream(stream);
                            int decompressedDataLength = ProtocolIO.readVarIntFromStream(stream);

                            int viSizeDataLength = ProtocolIO.lengthVarInt(decompressedDataLength);
                            int compressedDataLength = packetLength - viSizeDataLength;

                            buf.reset();

                            if (decompressedDataLength == 0) {
                                // uncompressed packet
                                buf.writeFrom(stream, compressedDataLength);
                            } else {
                                // read compressed packet
                                compressedReadPacket = Memory.ensureByteArrayCapacity(compressedReadPacket, compressedDataLength);
                                int readDataLength = 0;
                                while (readDataLength < compressedDataLength) {
                                    readDataLength += stream.read(compressedReadPacket, readDataLength, compressedDataLength - readDataLength);
                                }

                                // decompress packet data
                                synchronized (inflater) {
                                    buf.ensureWriteCapacity(decompressedDataLength);
                                    inflater.reset();
                                    inflater.setInput(compressedReadPacket, 0, compressedDataLength);
                                    inflater.inflate(buf.nioReference());
                                }
                            }
                        } else {
                            int dataLength = ProtocolIO.readVarIntFromStream(stream);
                            buf.reset();
                            buf.writeFrom(stream, dataLength);
                        }

                        ProtocolPhase phase = client.getState().getPhase();
                        int packetID = buf.readVarInt();
                        PacketMapping mapping = getProtocol()
                                .forPhase(phase)
                                .getClientboundPacketMapping(packetID);
                        if (mapping != null) {
                            packet = mapping.createPacketContainerWithData(this);
                            mapping.readPacketData(packet, buf);
                        } else {
                            // create unknown packet
                            mapping = UnknownPacket.CLIENTBOUND_MAPPING;
                            packet = unknownPacketContainer;
                            unknownPacketData.buffer(buf);
                            packet.networkId = packetID;
                            packet.phase = phase;
                        }

                        packet.source(this);
                        packet.set(PacketContainer.INBOUND);
                        packet.clear(PacketContainer.CANCEL);

                        countReceived.incrementAndGet();

                        // invoke event chains
                        client.onPacketReceived().invoker().onPacket(packet);
                        if (packet.check(PacketContainer.CANCEL)) {
                            continue;
                        }

                        var h = client.onTypedReceived().orNull(packet.getMapping());
                        if (h != null) {
                            h.invoker().onPacket(packet);
                            if (packet.check(PacketContainer.CANCEL)) {
                                continue;
                            }
                        }

                        client.onPacket().invoker().onPacket(packet);

                        // release buffer from packet after being handled
                        if (packet.isUnknown()) {
                            unknownPacketData.buffer(null);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Throwables.sneakyThrow(ex); // todo error handling
                    }
                }
            }
        } catch (InterruptedException ex) {
            onException().invoker().onException(ex);
        } catch (Exception ex) {
            onException().invoker().onException(new ClientReadException(ex));
        } finally {
            buf.free();

            unknownPacketData.buffer(null);
        }
    }

    @Override
    public Chain<PacketHandler> onPacketSink() {
        return client.onPacketSink();
    }

    @Override
    public int countReceived() {
        return countReceived.get();
    }

    @Override
    public Chain<PacketHandler> onPacketReceived() {
        return client.onPacketReceived();
    }

    // Get the current output stream to use
    private OutputStream getOutputStream() {
        try {
            if (socketOutput == null) {
                socketOutput = socket.getOutputStream();
            }

            return encryptionCipher != null ?
                    (encryptingStream != null ? encryptingStream : (encryptingStream = new CipherOutputStream(socketOutput, encryptionCipher))) :
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

    // get or create a byte buffer for writing packets
    private UnsafeByteBuf getWriteBuffer() {
        return writeBufferPool.getOrCompute(() -> UnsafeByteBuf.createDirect(1024));
    }

    public void setCompressionThreshold(int threshold) {
        synchronized (deflater) {
            this.compressionThreshold = threshold;
        }
    }

    @Override
    public Chain<ExceptionEventHandler> onException() {
        return client.onException();
    }
}
