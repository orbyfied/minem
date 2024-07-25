package com.github.orbyfied.minem.protocol.handshake;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import lombok.*;

/**
 * Common packet implementation: Handshake
 *
 * https://wiki.vg/Protocol#Handshaking
 */
@Mapping(id = 0x00, phase = ProtocolPhases.HANDSHAKE, primaryName = "ServerboundHandshake")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ServerboundHandshakePacket implements PacketData {

    @Override
    public void read(Packet container, UnsafeByteBuf in) {
        protocolVersion = in.readVarInt();
        address = in.readString();
        port = in.readShort();
        nextState = NextState.values()[in.readVarInt() - 1];
    }

    @Override
    public void write(Packet container, UnsafeByteBuf out) {
        out.writeVarInt(protocolVersion);
        out.writeString(address);
        out.writeShort(port);
        out.writeVarInt(nextState.getValue());
    }

    @RequiredArgsConstructor
    @Getter
    public enum NextState {
        STATUS(1),  // Check the server status after handshake
        LOGIN(2),   // Login after handshake
        TRANSFER(3) // idk

        ;
        final int value;
    }

    int protocolVersion; // The protocol version
    String address;      // The address used to join
    short port;          // The port used to join
    NextState nextState; // The next protocol state

}
