package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ServerboundChatPacket;

@Mapping(id = 0x01, phase = ProtocolPhases.PLAY, primaryName = "ServerboundChat", aliases = {"ServerboundChatMessage"}, dataClass = ServerboundChatPacket.class)
public final class ServerboundChatPacket47 {

    public static void write(ServerboundChatPacket packet, Packet container, UnsafeByteBuf buf) {
        buf.writeString(packet.getMessage());
    }

    public static void read(ServerboundChatPacket packet, Packet container, UnsafeByteBuf buf) {
        packet.setMessage(buf.readString());
    }

}
