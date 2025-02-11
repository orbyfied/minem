package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ClientboundChatMessagePacket;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

@Mapping(id = 0x02, phase = ProtocolPhases.PLAY, primaryName = "ClientboundChatMessage", dataClass = ClientboundChatMessagePacket.class)
public final class ClientboundChatMessagePacket47 {

    public static void read(ClientboundChatMessagePacket packet, Packet container, UnsafeByteBuf in) throws Exception {
        packet.setMessage(GsonComponentSerializer.gson().deserialize(in.readString()));
        packet.setPosition(in.readByte());
        System.out.println("[CHAT RECEIVED] " + PlainTextComponentSerializer.plainText().serialize(packet.getMessage()));
    }

    public static void write(ClientboundChatMessagePacket packet, Packet container, UnsafeByteBuf out) throws Exception {
        // todo
    }

}
