package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerPositionAndLookPacket;

@Mapping(id = 0x06, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerPositionAndLook", dataClass = ServerboundPlayerPositionAndLookPacket.class)
public class ServerboundPlayerPositionAndLookPacket47 {

    public static void read(ServerboundPlayerPositionAndLookPacket packet, Packet c, UnsafeByteBuf buf) {
        packet.setX(buf.readDouble());
        packet.setY(buf.readDouble());
        packet.setZ(buf.readDouble());
        packet.setYaw(buf.readFloat());
        packet.setPitch(buf.readFloat());
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerPositionAndLookPacket packet, Packet c, UnsafeByteBuf buf) {
        buf.writeDouble(packet.getX());
        buf.writeDouble(packet.getY());
        buf.writeDouble(packet.getZ());
        buf.writeFloat(packet.getYaw());
        buf.writeFloat(packet.getPitch());
        buf.writeBoolean(packet.isGrounded());
    }

}