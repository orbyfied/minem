package com.orbyfied.minem.protocol.handshake;

import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.buffer.UnsafeByteBuf;
import lombok.*;

/**
 * Common packet implementation: Status
 *
 * https://wiki.vg/Protocol#Status
 */
@Mapping(id = 0x00, phase = ProtocolPhases.STATUS, primaryName = "ClientboundStatus")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundStatusPacket implements SerializablePacketData {

    String json;

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        json = in.readString();
        System.out.println(json);
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        out.writeString(json);
    }

}
