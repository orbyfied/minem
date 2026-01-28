package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ServerboundPlayerPositionPacket;

@Mapping(id = 0x04, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerPosition", dataClass = ServerboundPlayerPositionPacket.class)
public class ServerboundPlayerPositionPacket47 {

    public static void read(ServerboundPlayerPositionPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        packet.setX(buf.readDouble());
        packet.setY(buf.readDouble());
        packet.setZ(buf.readDouble());
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerPositionPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        buf.writeDoubleReversed(packet.getX());
        buf.writeDoubleReversed(packet.getY());
        buf.writeDoubleReversed(packet.getZ());
        buf.writeBoolean(packet.isGrounded());
    }

}
