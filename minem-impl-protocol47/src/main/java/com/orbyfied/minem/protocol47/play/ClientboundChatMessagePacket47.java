package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ClientboundChatMessagePacket;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

@Mapping(id = 0x02, phase = ProtocolPhases.PLAY, primaryName = "ClientboundChatMessage", dataClass = ClientboundChatMessagePacket.class)
public final class ClientboundChatMessagePacket47 {

    public static void read(ClientboundChatMessagePacket packet, PacketContainer container, UnsafeByteBuf in) throws Exception {
        packet.setMessage(GsonComponentSerializer.gson().deserialize(in.readString()));
        packet.setPosition(in.readByte());
    }

    public static void write(ClientboundChatMessagePacket packet, PacketContainer container, UnsafeByteBuf out) throws Exception {
        // todo
    }

}
