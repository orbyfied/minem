package com.github.orbyfied.minem.packet;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.util.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Common packet implementation: Login-stage disconnect
 *
 * https://wiki.vg/Protocol#Status
 */
@Mapping(id = 0x03, phase = ProtocolPhases.LOGIN, primaryName = "ClientboundSetCompression")
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundSetCompressionPacket implements PacketData {

    int threshold; // The compression threshold to use

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        threshold = in.readVarInt();
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {
        out.writeVarInt(threshold);
    }

}
