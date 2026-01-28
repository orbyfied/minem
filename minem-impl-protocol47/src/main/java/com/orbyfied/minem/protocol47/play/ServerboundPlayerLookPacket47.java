package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ServerboundPlayerLookPacket;

@Mapping(id = 0x05, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerLook", dataClass = ServerboundPlayerLookPacket.class)
public class ServerboundPlayerLookPacket47 {

    public static void read(ServerboundPlayerLookPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        packet.setYaw(buf.readFloat());
        packet.setPitch(buf.readFloat());
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerLookPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        buf.writeFloatReversed(packet.getYaw());
        buf.writeFloatReversed(packet.getPitch());
        buf.writeBoolean(packet.isGrounded());
    }

}
