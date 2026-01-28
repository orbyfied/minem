package com.orbyfied.minem.protocol47.adhoc;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.SerializablePacketData;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Mapping(id = 0x06, phase = ProtocolPhases.PLAY)
public class ClientboundUpdateHealthPacket47 implements SerializablePacketData {

    float health;
    int food;
    float saturation;

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        this.health = in.readFloat();
        this.food = in.readVarInt();
        this.saturation = in.readFloat();
        System.out.println(this);
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {

    }

}
