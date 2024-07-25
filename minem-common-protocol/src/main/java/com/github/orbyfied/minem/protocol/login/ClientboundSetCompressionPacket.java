package com.github.orbyfied.minem.protocol.login;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import lombok.*;

/**
 * Common packet implementation: Login-stage disconnect
 *
 * https://wiki.vg/Protocol#Status
 */
@Mapping(id = 0x03, phase = ProtocolPhases.LOGIN, primaryName = "ClientboundSetCompression")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundSetCompressionPacket implements PacketData {

    int threshold; // The compression threshold to use

    @Override
    public void read(Packet container, UnsafeByteBuf in) throws Exception {
        threshold = in.readVarInt();
    }

    @Override
    public void write(Packet container, UnsafeByteBuf out) throws Exception {
        out.writeVarInt(threshold);
    }

}
