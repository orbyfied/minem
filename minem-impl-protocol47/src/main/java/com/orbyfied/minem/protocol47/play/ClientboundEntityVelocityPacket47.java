package com.orbyfied.minem.protocol47.play;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import com.orbyfied.minem.protocol.Mapping;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.ProtocolPhases;
import com.orbyfied.minem.protocol.play.ClientboundEntityVelocityPacket;

@Mapping(id = 0x12, phase = ProtocolPhases.PLAY, primaryName = "ClientboundEntityVelocity", dataClass = ClientboundEntityVelocityPacket.class)
public final class ClientboundEntityVelocityPacket47 {

    public static void read(ClientboundEntityVelocityPacket packet, PacketContainer container, UnsafeByteBuf in) throws Exception {
        packet.setEntityID(in.readVarInt());

        short sx = in.readShort();
        short sy = in.readShort();
        short sz = in.readShort();

        packet.setVelocityX(sx / 8000D);
        packet.setVelocityY(sy / 8000D);
        packet.setVelocityZ(sz / 8000D);
    }

    public static void write(ClientboundEntityVelocityPacket packet, PacketContainer container, UnsafeByteBuf out) throws Exception {
        // todo
    }

}
