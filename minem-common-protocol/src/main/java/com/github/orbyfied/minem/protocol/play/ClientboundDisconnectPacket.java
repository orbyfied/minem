package com.github.orbyfied.minem.protocol.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.data.ProtocolTextComponents;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import lombok.*;
import net.kyori.adventure.text.Component;


@Mapping(id = 0x40, phase = ProtocolPhases.PLAY, primaryName = "ClientboundDisconnect")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundDisconnectPacket implements PacketData {

    Component reason; // The text component reason sent by the server

    @Override
    public void read(Packet container, UnsafeByteBuf in) throws Exception {
        reason = ProtocolTextComponents.readJSONTextComponent(in);
    }

    @Override
    public void write(Packet container, UnsafeByteBuf out) throws Exception {
        // todo
    }

}
