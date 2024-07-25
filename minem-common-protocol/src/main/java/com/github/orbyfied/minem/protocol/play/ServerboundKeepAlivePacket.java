package com.github.orbyfied.minem.protocol.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
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
public class ServerboundKeepAlivePacket implements PacketData {

    int id; // The ID of the keep-alive transaction

    @Override
    public void read(Packet container, UnsafeByteBuf in) throws Exception {
        id = in.readVarInt();
    }

    @Override
    public void write(Packet container, UnsafeByteBuf out) throws Exception {
        out.writeVarInt(id);
    }

}
