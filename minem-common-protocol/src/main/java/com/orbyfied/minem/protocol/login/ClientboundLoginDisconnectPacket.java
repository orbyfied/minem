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
@Mapping(id = 0x00, phase = ProtocolPhases.LOGIN, primaryName = "ClientboundLoginDisconnect")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientboundLoginDisconnectPacket implements SerializablePacketData {

    String reason; // Reason in JSON Text Component format, todo

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) {
        reason = in.readString();
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) {
        out.writeString(reason);
    }

}
