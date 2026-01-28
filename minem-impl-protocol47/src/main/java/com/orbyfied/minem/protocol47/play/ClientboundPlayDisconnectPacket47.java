package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.model.ProtocolTextComponents;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ClientboundPlayDisconnectPacket;

@Mapping(id = 0x40, phase = ProtocolPhases.PLAY, primaryName = "ClientboundPlayDisconnect", dataClass = ClientboundPlayDisconnectPacket.class)
public class ClientboundPlayDisconnectPacket47 {

    public static void read(ClientboundPlayDisconnectPacket packet, PacketContainer container, UnsafeByteBuf in) throws Exception {
        packet.setReason(ProtocolTextComponents.readJSONTextComponent(in));
    }

    public static void write(ClientboundPlayDisconnectPacket packet, PacketContainer container, UnsafeByteBuf out) throws Exception {
        // todo
    }

}
