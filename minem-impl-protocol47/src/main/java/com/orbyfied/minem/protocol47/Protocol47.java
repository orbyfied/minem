package com.orbyfied.minem.protocol47;

import com.orbyfied.minem.protocol.CommonPacketImplementations;
import com.orbyfied.minem.protocol.PacketUtil;
import com.orbyfied.minem.protocol.Protocol;
import com.orbyfied.minem.protocol.play.ClientboundEntityVelocityPacket;
import com.orbyfied.minem.protocol47.adhoc.ClientboundUpdateHealthPacket47;
import com.orbyfied.minem.protocol47.adhoc.ServerboundClientStatusPacket47;
import com.orbyfied.minem.protocol47.play.*;
import com.orbyfied.minem.protocol47.play.*;

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
            ServerboundPlayerGroundedPacket47.class,
            ClientboundEntityVelocityPacket47.class,

            ServerboundClientStatusPacket47.class,
            ClientboundUpdateHealthPacket47.class
    };

    static {
        PROTOCOL.registerPacketMappings(CommonPacketImplementations.MAPPINGS);
        PROTOCOL.registerPacketMappings(PacketUtil.compileAll(CLASSES));
    }

}
