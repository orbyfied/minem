package com.github.orbyfied.minem;

import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.protocol.*;
import com.github.orbyfied.minem.util.ByteBuf;
import com.github.orbyfied.minem.util.Memory;
import com.github.orbyfied.minem.util.ProtocolIO;
import lombok.Getter;
import slatepowered.veru.misc.Throwables;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

/**
 * Represents a simple, modular (component based) Minecraft client.
 */
public class MinecraftClient extends ProtocolContext implements PacketSource, PacketSink {

    /**
     * The protocol to use.
     */
    @Getter
    Protocol protocol;

    Socket socket;                                 // The socket connection
    Thread readerThread;                           // The connection reader thread

    ClientState state = ClientState.NOT_CONNECTED; // The current client state

    int compressionLevel = 2;                      // The compression level to use
    int compressionThreshold = -1;                 // The connection threshold agreed upon with the server, -1 if no compression

    final Deflater deflater = new Deflater();
    byte[] compressedBytes = new byte[1024 * 16];
    byte[] decompressedBytes = new byte[1024 * 16];
    byte[] varIntWriteBuffer = new byte[5];

    /* Client Events */
    final Chain<ClientStateSwitchHandler> onStateSwitch = new Chain<>(ClientStateSwitchHandler.class);

    /* Connection Events */
    final Chain<PacketHandler> onPacket = new Chain<>(PacketHandler.class);
    final Chain<PacketHandler> onPacketSink = new Chain<>(PacketHandler.class);
    final Chain<PacketHandler> onPacketReceived = new Chain<>(PacketHandler.class);
    AtomicInteger countReceived = new AtomicInteger();
    AtomicInteger countSent = new AtomicInteger();

    // Switch the current client state
    private void switchState(ClientState state) {
        var old = this.state;
        this.state = state;
        onStateSwitch.invoker().onStateSwitch(old, state);
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
            switchState(ClientState.NOT_CONNECTED);
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
            OutputStream stream = socket.getOutputStream();
            ByteBuf buf = getWriteBuffer();

            // write packet type and data
            buf.writeVarInt(packet.getId());
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
                    byte[] decompressedBytes = Memory.ensureByteArrayCapacity(this.decompressedBytes, buf.remainingWritten());
                    byte[] compressedBytes = Memory.ensureByteArrayCapacity(this.compressedBytes, decompressedBytes.length);
                    buf.getBytes(0, decompressedBytes, 0, decompressedBytes.length);

                    deflater.end();
                    deflater.setLevel(compressionLevel);
                    deflater.setInput(decompressedBytes);
                    int compressedSize = deflater.deflate(compressedBytes);
                    deflater.finish();

                    int dataLengthSize = ProtocolIO.writeVarIntToBytes(varIntWriteBuffer, dataLength);
                    ProtocolIO.writeVarIntToStream(stream, compressedSize + dataLengthSize);
                    stream.write(varIntWriteBuffer, 0, dataLengthSize);
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
            ByteBuf buf = ByteBuf.create(1024);
            while (!socket.isClosed() && socket.isConnected()) {
                InputStream stream = socket.getInputStream();
                buf.clear();

                // check if compression is set, then decide
                // how to read packet and data length
                if (compressionThreshold != -1) {
                    int packetLength = ProtocolIO.readVarIntFromStream(stream);
                } else {
                    int dataLength = ProtocolIO.readVarIntFromStream(stream);
                    buf.writeFrom(stream, dataLength);
                }

                ProtocolPhase phase = this.state.getPhase();
                int packetID = buf.readVarInt();
                PacketMapping mapping = protocol
                        .forPhase(phase)
                        .getPacketMapping(packetID);

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

    public interface ClientStateSwitchHandler {
        void onStateSwitch(ClientState oldState, ClientState newState);
    }

}
