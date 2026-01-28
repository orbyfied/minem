package com.orbyfied.minem.protocol.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.model.ProtocolTextComponents;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import lombok.*;
import net.kyori.adventure.text.Component;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundPlayDisconnectPacket implements SerializablePacketData {

    Component reason; // The text component reason sent by the server

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        reason = ProtocolTextComponents.readJSONTextComponent(in);
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        // todo
    }

}
