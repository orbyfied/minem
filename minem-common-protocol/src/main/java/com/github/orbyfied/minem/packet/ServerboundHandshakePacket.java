package com.github.orbyfied.minem.packet;

import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.PacketData;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.util.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Common packet implementation: Handshake
 *
 * https://wiki.vg/Protocol#Handshaking
 */
@Mapping(id = 0x00, phase = ProtocolPhases.HANDSHAKE, primaryName = "ServerboundHandshake")
@NoArgsConstructor
@AllArgsConstructor
public class ServerboundHandshakePacket implements PacketData {

    @Override
    public void read(Packet container, ByteBuf in) {
        protocolVersion = in.readVarInt();
        address = in.readString();
        nextState = NextState.values()[in.readVarInt() - 1];
    }

    @Override
    public void write(Packet container, ByteBuf out) {
        out.writeVarInt(protocolVersion);
        out.writeString(address);
        out.writeVarInt(nextState.ordinal() + 1);
    }

    public enum NextState {
        STATUS,  // Check the server status after handshake
        LOGIN,   // Login after handshake
        TRANSFER // idk
    }

    int protocolVersion; // The protocol version
    String address;      // The address used to join
    NextState nextState; // The next protocol state

}
