package com.github.orbyfied.minem.packet;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.util.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Common packet implementation: Login start
 *
 * https://wiki.vg/Protocol#Login
 */
@NoArgsConstructor
@AllArgsConstructor
@Mapping(id = 0x00, primaryName = "ServerboundLoginStart", phase = ProtocolPhases.LOGIN)
public class ServerboundLoginStart implements PacketData {

    String username;
    UUID uuid;

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        username = in.readString();
        uuid = in.readUUID();
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {
        out.writeString(username);
        out.writeUUID(uuid);
    }

}
