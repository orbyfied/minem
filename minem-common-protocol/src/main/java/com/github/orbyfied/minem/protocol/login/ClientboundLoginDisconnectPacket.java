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
@Mapping(id = 0x00, phase = ProtocolPhases.LOGIN, primaryName = "ClientboundLoginDisconnect")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundLoginDisconnectPacket implements PacketData {

    String reason; // Reason in JSON Text Component format, todo

    @Override
    public void read(Packet container, UnsafeByteBuf in) {
        reason = in.readString();
    }

    @Override
    public void write(Packet container, UnsafeByteBuf out) {
        out.writeString(reason);
    }

}
