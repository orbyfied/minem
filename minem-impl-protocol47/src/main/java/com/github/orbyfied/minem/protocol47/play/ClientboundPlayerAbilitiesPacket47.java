package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ClientboundPlayerAbilitiesPacket;

@Mapping(id = 0x39, phase = ProtocolPhases.PLAY, primaryName = "ClientboundPlayerAbilities", dataClass = ClientboundPlayerAbilitiesPacket.class)
public class ClientboundPlayerAbilitiesPacket47 {

    public static void write(ClientboundPlayerAbilitiesPacket packet, Packet container, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ClientboundPlayerAbilitiesPacket packet, Packet container, UnsafeByteBuf buf) {
        byte flags = buf.readByte();
        packet.setInvulnerable((flags & 0x01) > 0);
        packet.setFlying((flags & 0x02) > 0);
        packet.setCanFly((flags & 0x04) > 0);
        packet.setCreativeMode((flags & 0x08) > 0);

        packet.setFlySpeed(buf.readFloat());
        packet.setFovModifier(buf.readFloat());
    }

}
