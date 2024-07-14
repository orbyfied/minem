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
@Mapping(id = 0x00, phase = ProtocolPhases.LOGIN, primaryName = "ClientboundLoginDisconnect")
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundLoginDisconnectPacket implements PacketData {

    String reason; // Reason in JSON Text Component format, todo

    @Override
    public void read(Packet container, ByteBuf in) {
        reason = in.readString();
    }

    @Override
    public void write(Packet container, ByteBuf out) {
        out.writeString(reason);
    }

}
