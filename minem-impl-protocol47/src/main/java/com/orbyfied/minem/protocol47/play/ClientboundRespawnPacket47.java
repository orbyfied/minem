package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ClientboundRespawnPacket;
import com.orbyfied.minem.protocol.play.Dimension;
import com.orbyfied.minem.protocol.play.Gamemode;

@Mapping(id = 0x07, phase = ProtocolPhases.PLAY, primaryName = "ClientboundRespawn", dataClass = ClientboundRespawnPacket.class)
public class ClientboundRespawnPacket47 {

    public static void write(ClientboundRespawnPacket packet, PacketContainer container, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ClientboundRespawnPacket packet, PacketContainer container, UnsafeByteBuf buf) {
        packet.setDimension(Dimension.from(buf.readInt()));
        packet.setDifficulty(buf.readByte());
        packet.setGamemode(Gamemode.values()[buf.readByte()]);
        packet.setLevelType(buf.readString());
    }

}
