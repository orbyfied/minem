package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ClientboundJoinGamePacket;
import com.orbyfied.minem.protocol.play.Dimension;
import com.orbyfied.minem.protocol.play.Gamemode;

@Mapping(id = 0x01, phase = ProtocolPhases.PLAY, primaryName = "ClientboundJoinGame", dataClass = ClientboundJoinGamePacket.class)
public class ClientboundJoinGamePacket47 {

    public static void write(ClientboundJoinGamePacket packet, PacketContainer container, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ClientboundJoinGamePacket packet, PacketContainer container, UnsafeByteBuf buf) {
        packet.setEntityID(buf.readInt());
        packet.setGamemode(Gamemode.values()[buf.readByte()]);
        packet.setDimension(Dimension.from(buf.readByte()));
        packet.setDifficulty(buf.readByte());
        packet.setMaxPlayers(buf.readByte());
        packet.setLevelType(buf.readString());
        packet.setReducedDebugInfo(buf.readBoolean());
    }

}
