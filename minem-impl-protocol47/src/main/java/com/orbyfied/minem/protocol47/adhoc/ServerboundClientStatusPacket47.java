package com.orbyfied.minem.protocol47.adhoc;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import com.orbyfied.minem.protocol.ProtocolPhases;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Mapping(id = 0x16, phase = ProtocolPhases.PLAY)
public class ServerboundClientStatusPacket47 implements SerializablePacketData {

    int actionID;

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        // todo
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        out.writeVarInt(actionID);
    }

}
