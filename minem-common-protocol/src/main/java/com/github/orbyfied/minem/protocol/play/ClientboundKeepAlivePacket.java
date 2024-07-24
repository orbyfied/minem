package com.github.orbyfied.minem.protocol.play;

import com.github.orbyfied.minem.buffer.ByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import lombok.*;

/**
 * Common packet implementation: Play-stage keep alive (clientbound)
 *
 * https://wiki.vg/Protocol#Play
 */
@Mapping(id = 0x00, phase = ProtocolPhases.PLAY, primaryName = "ClientboundKeepAlive")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundKeepAlivePacket implements PacketData {

    int id; // The ID of the keep-alive transaction

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        id = in.readVarInt();
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {
        out.writeVarInt(id);
    }

}
