package com.github.orbyfied.minem;

import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.protocol.*;
import com.github.orbyfied.minem.util.ByteBuf;
import com.github.orbyfied.minem.util.ProtocolIO;
import lombok.Getter;
import slatepowered.veru.misc.Throwables;

import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a simple, modular (component based) Minecraft client.
 */
public class MinecraftClient extends ProtocolContext implements PacketSource, PacketSink {

    /**
     * The protocol to use.
     */
    @Getter
    Protocol protocol;

    Socket socket; // The socket connection

    /* Connection Events */
    final Chain<PacketHandler> onPacket = new Chain<>(PacketHandler.class);
    final Chain<PacketHandler> onPacketSink = new Chain<>(PacketHandler.class);
    final Chain<PacketHandler> onPacketReceived = new Chain<>(PacketHandler.class);
    AtomicInteger countReceived = new AtomicInteger();
    AtomicInteger countSent = new AtomicInteger();

    @Override
    public boolean isOpen() {
        return socket.isConnected();
    }

    @Override
    public void close() {
        try {
            socket.close();
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
        // todo: maybe pool these or smth idk
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

            // write length directly, then write the buffer
            ProtocolIO.writeVarIntToStream(stream, dataLength);
            buf.readTo(stream, dataLength);
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

}
