package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.Memory;
import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket;
import com.github.orbyfied.minem.util.BufferUtil;

import java.util.Arrays;

@Mapping(id = 0x08, phase = ProtocolPhases.PLAY, primaryName = "ClientboundPositionAndLook", dataClass = ClientboundPositionAndLookPacket.class)
public class ClientboundPositionAndLookPacket47 {

    public static void write(ClientboundPositionAndLookPacket packet, Packet c, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ClientboundPositionAndLookPacket packet, Packet c, UnsafeByteBuf buf) {
        packet.setX(buf.readDoubleReversed());
        packet.setY(buf.readDoubleReversed());
        packet.setZ(buf.readDoubleReversed());
        packet.setYaw(buf.readFloatReversed());
        packet.setPitch(buf.readFloatReversed());
        packet.setFlags(buf.readByte());
    }

}
