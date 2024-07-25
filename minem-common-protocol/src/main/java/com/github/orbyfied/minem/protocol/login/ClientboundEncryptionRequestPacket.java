package com.github.orbyfied.minem.protocol.login;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import lombok.*;

/**
 * Common packet implementation: Encryption request
 *
 * https://wiki.vg/Protocol#Login
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Mapping(id = 0x01, primaryName = "ClientboundEncryptionRequest", phase = ProtocolPhases.LOGIN)
public class ClientboundEncryptionRequestPacket implements PacketData {

    String serverID;           // The server ID
    byte[] publicKeyBytes;     // The byte data for the public key
    byte[] verifyTokenBytes;   // The byte data for the verify token
    boolean shouldAuth = true; // Whether to authenticate

    @Override
    public void read(Packet container, UnsafeByteBuf in) throws Exception {
        serverID = in.readString();
        publicKeyBytes = in.readBytes(in.readVarInt());
        verifyTokenBytes = in.readBytes(in.readVarInt());
        if (container.getProtocolVersion() > 47) {
            shouldAuth = in.readBoolean();
        }
    }

    @Override
    public void write(Packet container, UnsafeByteBuf out) throws Exception {
        out.writeString(serverID);
        out.writeVarInt(publicKeyBytes.length);
        out.writeBytes(publicKeyBytes);
        out.writeVarInt(verifyTokenBytes.length);
        out.writeBytes(verifyTokenBytes);
        if (container.getProtocolVersion() > 47) {
            out.writeBoolean(shouldAuth);
        }
    }

}
