package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket;

@Mapping(id = 0x08, phase = ProtocolPhases.PLAY, primaryName = "ClientboundPositionAndLook", dataClass = ClientboundPositionAndLookPacket.class)
public class ClientboundPositionAndLookPacket47 {

    public static void write(ClientboundPositionAndLookPacket packet, Packet c, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ClientboundPositionAndLookPacket packet, Packet c, UnsafeByteBuf buf) {
        packet.setX(buf.readDouble());
        packet.setY(buf.readDouble());
        packet.setZ(buf.readDouble());
        packet.setYaw(buf.readFloat());
        packet.setPitch(buf.readFloat());
        packet.setFlags(buf.readByte());
    }

}
