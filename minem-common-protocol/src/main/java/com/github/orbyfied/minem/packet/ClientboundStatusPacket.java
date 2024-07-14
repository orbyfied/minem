package com.github.orbyfied.minem.packet;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.util.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Common packet implementation: Status
 *
 * https://wiki.vg/Protocol#Status
 */
@Mapping(id = 0x00, phase = ProtocolPhases.STATUS, primaryName = "ClientboundStatus")
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundStatusPacket implements PacketData {

    String json;

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        json = in.readString();
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {
        out.writeString(json);
    }

}
