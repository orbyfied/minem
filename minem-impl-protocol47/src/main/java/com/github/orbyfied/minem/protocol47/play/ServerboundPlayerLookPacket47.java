package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerLookPacket;

@Mapping(id = 0x05, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerLook", dataClass = ServerboundPlayerLookPacket.class)
public class ServerboundPlayerLookPacket47 {

    public static void read(ServerboundPlayerLookPacket packet, Packet c, UnsafeByteBuf buf) {
        packet.setYaw(buf.readFloat());
        packet.setPitch(buf.readFloat());
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerLookPacket packet, Packet c, UnsafeByteBuf buf) {
        buf.writeFloat(packet.getYaw());
        buf.writeFloat(packet.getPitch());
        buf.writeBoolean(packet.isGrounded());
    }

}
