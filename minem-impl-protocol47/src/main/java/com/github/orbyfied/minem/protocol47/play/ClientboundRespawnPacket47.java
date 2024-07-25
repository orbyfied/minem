package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ClientboundJoinGamePacket;
import com.github.orbyfied.minem.protocol.play.ClientboundRespawnPacket;
import com.github.orbyfied.minem.protocol.play.Dimension;
import com.github.orbyfied.minem.protocol.play.Gamemode;

@Mapping(id = 0x07, phase = ProtocolPhases.PLAY, primaryName = "ClientboundRespawn", dataClass = ClientboundRespawnPacket.class)
public class ClientboundRespawnPacket47 {

    public static void write(ClientboundRespawnPacket packet, Packet container, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ClientboundRespawnPacket packet, Packet container, UnsafeByteBuf buf) {
        packet.setDimension(Dimension.from(buf.readInt()));
        packet.setDifficulty(buf.readByte());
        packet.setGamemode(Gamemode.values()[buf.readByte()]);
        packet.setLevelType(buf.readString());
    }

}
