package com.orbyfied.minem.protocol.login;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import com.orbyfied.minem.protocol.ProtocolPhases;
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
public class ClientboundLoginSuccessPacket implements SerializablePacketData {

    UUID uuid;
    String username;
    Map<String, LoginProperty> properties = new HashMap<>();

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        uuid = container.getProtocolVersion() <= 47 ? UUID.fromString(in.readString()) : in.readUUID();
        username = in.readString();

        if (container.getProtocolVersion() > 47) {
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
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        if (container.getProtocolVersion() <= 47) out.writeString(uuid.toString());
        else out.writeUUID(uuid);
        out.writeString(username);

        if (container.getProtocolVersion() > 47) {
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

}
