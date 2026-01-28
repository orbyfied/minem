package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ServerboundChatPacket;

@Mapping(id = 0x01, phase = ProtocolPhases.PLAY, primaryName = "ServerboundChat", aliases = {"ServerboundChatMessage"}, dataClass = ServerboundChatPacket.class)
public final class ServerboundChatPacket47 {

    public static void write(ServerboundChatPacket packet, PacketContainer container, UnsafeByteBuf buf) {
        buf.writeString(packet.getMessage());
    }

    public static void read(ServerboundChatPacket packet, PacketContainer container, UnsafeByteBuf buf) {
        packet.setMessage(buf.readString());
    }

}
