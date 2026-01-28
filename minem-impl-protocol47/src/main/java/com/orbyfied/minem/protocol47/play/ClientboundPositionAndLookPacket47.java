package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket;

@Mapping(id = 0x08, phase = ProtocolPhases.PLAY, primaryName = "ClientboundPositionAndLook", dataClass = ClientboundPositionAndLookPacket.class)
public class ClientboundPositionAndLookPacket47 {

    public static void write(ClientboundPositionAndLookPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ClientboundPositionAndLookPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        packet.setX(buf.readDoubleReversed());
        packet.setY(buf.readDoubleReversed());
        packet.setZ(buf.readDoubleReversed());
        packet.setYaw(buf.readFloatReversed());
        packet.setPitch(buf.readFloatReversed());
        packet.setFlags(buf.readByte());
    }

}
