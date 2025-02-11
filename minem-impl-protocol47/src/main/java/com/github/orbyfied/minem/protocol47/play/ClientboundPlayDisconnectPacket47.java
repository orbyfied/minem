package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.data.ProtocolTextComponents;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ClientboundPlayDisconnectPacket;

@Mapping(id = 0x40, phase = ProtocolPhases.PLAY, primaryName = "ClientboundPlayDisconnect", dataClass = ClientboundPlayDisconnectPacket.class)
public class ClientboundPlayDisconnectPacket47 {

    public static void read(ClientboundPlayDisconnectPacket packet, Packet container, UnsafeByteBuf in) throws Exception {
        packet.setReason(ProtocolTextComponents.readJSONTextComponent(in));
    }

    public static void write(ClientboundPlayDisconnectPacket packet, Packet container, UnsafeByteBuf out) throws Exception {
        // todo
    }

}
