package com.github.orbyfied.minem.protocol.login;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.buffer.ByteBuf;
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
public class ServerboundLoginStartPacket implements PacketData {

    String username;
    UUID uuid;

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        username = in.readString();
        if (container.getProtocolVersion() > 47) {
            uuid = in.readUUID();
        }
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {
        out.writeString(username);
        if (container.getProtocolVersion() > 47) {
            out.writeUUID(uuid);
        }
    }

}
