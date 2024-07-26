package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerPositionPacket;

@Mapping(id = 0x04, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerPosition", dataClass = ServerboundPlayerPositionPacket.class)
public class ServerboundPlayerPositionPacket47 {

    public static void read(ServerboundPlayerPositionPacket packet, Packet c, UnsafeByteBuf buf) {
        packet.setX(buf.readDouble());
        packet.setY(buf.readDouble());
        packet.setZ(buf.readDouble());
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerPositionPacket packet, Packet c, UnsafeByteBuf buf) {
        buf.writeDoubleReversed(packet.getX());
        buf.writeDoubleReversed(packet.getY());
        buf.writeDoubleReversed(packet.getZ());
        buf.writeBoolean(packet.isGrounded());
    }

}
