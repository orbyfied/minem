package com.github.orbyfied.minem.packet;

import com.github.orbyfied.minem.buffer.ByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Common packet implementation: Login success
 *
 * https://wiki.vg/Protocol#Login
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Mapping(id = 0x02, primaryName = "ClientboundLoginSuccess", phase = ProtocolPhases.LOGIN)
public class ClientboundLoginSuccessPacket implements PacketData {

    UUID uuid;
    String username;
    Map<String, LoginProperty> properties = new HashMap<>();

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        uuid = in.readUUID();
        username = in.readString();

        // read properties
        int count = in.readVarInt();
        for (int i = 0; i < count; i++) {
            String name = in.readString();
            String value = in.readString();
            boolean signed = in.readBoolean();
            String signature = signed ? in.readString() : null;
            properties.put(name, new LoginProperty(name, value, signed, signature));
        }
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {
        out.writeUUID(uuid);
        out.writeString(username);

        // write properties
        out.writeVarInt(properties.size());
        for (Map.Entry<String, LoginProperty> entry : properties.entrySet()) {
            out.writeString(entry.getKey());
            out.writeString(entry.getValue().getValue());
            out.writeBoolean(entry.getValue().isSigned());
            if (entry.getValue().isSigned()) {
                out.writeString(entry.getValue().getSignature());
            }
        }
    }

}
