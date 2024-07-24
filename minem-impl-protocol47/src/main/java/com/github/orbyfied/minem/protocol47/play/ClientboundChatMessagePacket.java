package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.ByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.common.ChatMessagePacket;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

@Mapping(id = 0x02, phase = ProtocolPhases.PLAY, primaryName = "ClientboundChatMessage")
public class ClientboundChatMessagePacket extends ChatMessagePacket implements PacketData {

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        setMessage(GsonComponentSerializer.gson().deserialize(in.readString()));
        setPosition(in.readByte());
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {
        // todo
    }

}
