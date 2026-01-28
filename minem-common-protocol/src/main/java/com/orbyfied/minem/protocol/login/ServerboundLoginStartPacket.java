package com.orbyfied.minem.protocol.login;

import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.buffer.UnsafeByteBuf;
import lombok.*;

import java.util.UUID;

/**
 * Common packet implementation: Login start
 *
 * https://wiki.vg/Protocol#Login
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Mapping(id = 0x00, primaryName = "ServerboundLoginStart", phase = ProtocolPhases.LOGIN)
public class ServerboundLoginStartPacket implements SerializablePacketData {

    String username;
    UUID uuid;

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        username = in.readString();
        if (container.getProtocolVersion() > 47) {
            uuid = in.readUUID();
        }
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        out.writeString(username);
        if (container.getProtocolVersion() > 47) {
            out.writeUUID(uuid);
        }
    }

}
