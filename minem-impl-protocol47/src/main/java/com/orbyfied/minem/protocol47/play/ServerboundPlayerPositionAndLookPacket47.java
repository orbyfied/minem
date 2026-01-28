package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ServerboundPlayerPositionAndLookPacket;

@Mapping(id = 0x06, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerPositionAndLook", dataClass = ServerboundPlayerPositionAndLookPacket.class)
public class ServerboundPlayerPositionAndLookPacket47 {

    public static void read(ServerboundPlayerPositionAndLookPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        packet.setX(buf.readDouble());
        packet.setY(buf.readDouble());
        packet.setZ(buf.readDouble());
        packet.setYaw(buf.readFloat());
        packet.setPitch(buf.readFloat());
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerPositionAndLookPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        buf.writeDoubleReversed(packet.getX());
        buf.writeDoubleReversed(packet.getY());
        buf.writeDoubleReversed(packet.getZ());
        buf.writeFloatReversed(packet.getYaw());
        buf.writeFloatReversed(packet.getPitch());
        buf.writeBoolean(packet.isGrounded());
    }

}