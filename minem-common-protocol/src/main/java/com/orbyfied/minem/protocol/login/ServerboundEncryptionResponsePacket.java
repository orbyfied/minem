package com.orbyfied.minem.protocol.login;

import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.SerializablePacketData;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.buffer.UnsafeByteBuf;
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
@Mapping(id = 0x01, primaryName = "ServerboundEncryptionResponse", phase = ProtocolPhases.LOGIN)
public class ServerboundEncryptionResponsePacket implements SerializablePacketData {

    byte[] sharedSecretBytes; // The bytes of the shared secret encrypted with the public key
    byte[] verifyTokenBytes;  // The bytes of the received verify token encrypted with the same public key

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        sharedSecretBytes = in.readBytes(in.readVarInt());
        verifyTokenBytes = in.readBytes(in.readVarInt());
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {
        out.writeVarInt(sharedSecretBytes.length);
        out.writeBytes(sharedSecretBytes);
        out.writeVarInt(verifyTokenBytes.length);
        out.writeBytes(verifyTokenBytes);
    }

}
