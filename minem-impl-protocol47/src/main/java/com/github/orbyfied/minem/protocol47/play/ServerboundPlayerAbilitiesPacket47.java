package com.github.orbyfied.minem.protocol47.play;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import com.github.orbyfied.minem.protocol.Mapping;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.play.ClientboundPlayerAbilitiesPacket;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerAbilitiesPacket;

@Mapping(id = 0x13, phase = ProtocolPhases.PLAY, primaryName = "ServerboundPlayerAbilities", dataClass = ServerboundPlayerAbilitiesPacket.class)
public class ServerboundPlayerAbilitiesPacket47 {

    public static void write(ServerboundPlayerAbilitiesPacket packet, Packet container, UnsafeByteBuf buf) {
        // todo
    }

    public static void read(ServerboundPlayerAbilitiesPacket packet, Packet container, UnsafeByteBuf buf) {
        byte flags = buf.readByte();
        packet.setInvulnerable((flags & 0x01) > 0);
        packet.setFlying((flags & 0x02) > 0);
        packet.setCanFly((flags & 0x04) > 0);
        packet.setCreativeMode((flags & 0x08) > 0);

        packet.setFlySpeed(buf.readFloat());
        packet.setWalkSpeed(buf.readFloat());
    }

}
