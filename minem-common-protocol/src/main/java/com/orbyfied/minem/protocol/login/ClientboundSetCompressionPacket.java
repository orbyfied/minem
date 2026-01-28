package com.orbyfied.minem.protocol.login;

import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.buffer.UnsafeByteBuf;
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
public class ClientboundSetCompressionPacket implements SerializablePacketData {

    int threshold; // The compression threshold to use

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        threshold = in.readVarInt();
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        out.writeVarInt(threshold);
    }

}
