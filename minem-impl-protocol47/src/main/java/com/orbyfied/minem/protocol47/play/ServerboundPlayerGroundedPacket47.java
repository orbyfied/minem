package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ServerboundPlayerGroundedPacket;

@Mapping(id = 0x03, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerGrounded", dataClass = ServerboundPlayerGroundedPacket.class)
public class ServerboundPlayerGroundedPacket47 {

    public static void read(ServerboundPlayerGroundedPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerGroundedPacket packet, PacketContainer c, UnsafeByteBuf buf) {
        buf.writeBoolean(packet.isGrounded());
    }

}