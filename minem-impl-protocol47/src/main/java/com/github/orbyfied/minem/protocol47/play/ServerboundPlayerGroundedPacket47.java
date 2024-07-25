package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerGroundedPacket;

@Mapping(id = 0x03, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerGrounded", dataClass = ServerboundPlayerGroundedPacket.class)
public class ServerboundPlayerGroundedPacket47 {

    public static void read(ServerboundPlayerGroundedPacket packet, Packet c, UnsafeByteBuf buf) {
        packet.setGrounded(buf.readBoolean());
    }

    public static void write(ServerboundPlayerGroundedPacket packet, Packet c, UnsafeByteBuf buf) {
        buf.writeBoolean(packet.isGrounded());
    }

}