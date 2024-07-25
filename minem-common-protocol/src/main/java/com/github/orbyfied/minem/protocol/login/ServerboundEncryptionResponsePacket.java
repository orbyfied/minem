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
@Mapping(id = 0x01, primaryName = "ServerboundEncryptionResponse", phase = ProtocolPhases.LOGIN)
public class ServerboundEncryptionResponsePacket implements PacketData {

    byte[] sharedSecretBytes; // The bytes of the shared secret encrypted with the public key
    byte[] verifyTokenBytes;  // The bytes of the received verify token encrypted with the same public key

    @Override
    public void read(Packet container, UnsafeByteBuf in) throws Exception {
        sharedSecretBytes = in.readBytes(in.readVarInt());
        verifyTokenBytes = in.readBytes(in.readVarInt());
    }

    @Override
    public void write(Packet container, UnsafeByteBuf out) throws Exception {
        out.writeVarInt(sharedSecretBytes.length);
        out.writeBytes(sharedSecretBytes);
        out.writeVarInt(verifyTokenBytes.length);
        out.writeBytes(verifyTokenBytes);
    }

}
