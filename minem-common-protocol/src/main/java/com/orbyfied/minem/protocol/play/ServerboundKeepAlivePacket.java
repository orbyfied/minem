package com.orbyfied.minem.protocol.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import com.orbyfied.minem.protocol.ProtocolPhases;
import lombok.*;

/**
 * Common packet implementation: Play-stage keep alive (serverbound)
 *
 * https://wiki.vg/Protocol#Play
 */
@Mapping(id = 0x00, phase = ProtocolPhases.PLAY, primaryName = "ServerboundKeepAlive")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ServerboundKeepAlivePacket implements SerializablePacketData {

    int id; // The ID of the keep-alive transaction

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        id = in.readVarInt();
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        out.writeVarInt(id);
    }

}
