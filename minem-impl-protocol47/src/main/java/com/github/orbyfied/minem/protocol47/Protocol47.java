package com.github.orbyfied.minem.protocol47;

import com.github.orbyfied.minem.protocol.CommonPacketImplementations;
import com.github.orbyfied.minem.protocol.PacketUtil;
import com.github.orbyfied.minem.protocol.Protocol;
import com.github.orbyfied.minem.protocol.play.ClientboundJoinGamePacket;
import com.github.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerGroundedPacket;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerPositionPacket;
import com.github.orbyfied.minem.protocol47.play.*;

public class Protocol47 {

    /**
     * The protocol object.
     */
    public static final Protocol PROTOCOL = Protocol.create(47);

    // All defined packet classes
    private static final Class<?>[] CLASSES = new Class[] {
            ClientboundChatMessagePacket47.class,
            ServerboundChatPacket47.class,
            ClientboundPlayDisconnectPacket47.class,
            ClientboundPlayerAbilitiesPacket47.class,
            ServerboundPlayerAbilitiesPacket47.class,
            ClientboundJoinGamePacket47.class,
            ClientboundRespawnPacket47.class,
            ClientboundPositionAndLookPacket47.class,
            ServerboundPlayerPositionAndLookPacket47.class,
            ServerboundPlayerPositionPacket47.class,
            ServerboundPlayerLookPacket47.class,
            ServerboundPlayerPositionPacket47.class,
            ServerboundPlayerGroundedPacket47.class
    };

    static {
        PROTOCOL.registerPacketMappings(CommonPacketImplementations.MAPPINGS);
        PROTOCOL.registerPacketMappings(PacketUtil.compileAll(CLASSES));
    }

}
