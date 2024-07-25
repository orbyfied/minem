package com.github.orbyfied.minem.protocol47;

import com.github.orbyfied.minem.protocol.CommonPacketImplementations;
import com.github.orbyfied.minem.protocol.PacketUtil;
import com.github.orbyfied.minem.protocol.Protocol;
import com.github.orbyfied.minem.protocol47.play.ClientboundChatMessagePacket47;
import com.github.orbyfied.minem.protocol47.play.ClientboundPlayerAbilitiesPacket47;
import com.github.orbyfied.minem.protocol47.play.ServerboundChatPacket47;
import com.github.orbyfied.minem.protocol47.play.ServerboundPlayerAbilitiesPacket47;

public class Protocol47 {

    /**
     * The protocol object.
     */
    public static final Protocol PROTOCOL = Protocol.create(47);

    // All defined packet classes
    private static final Class<?>[] CLASSES = new Class[] {
            ClientboundChatMessagePacket47.class,
            ServerboundChatPacket47.class,
            ClientboundPlayerAbilitiesPacket47.class,
            ServerboundPlayerAbilitiesPacket47.class
    };

    static {
        PROTOCOL.registerPacketMappings(CommonPacketImplementations.MAPPINGS);
        PROTOCOL.registerPacketMappings(PacketUtil.compileAll(CLASSES));
    }

}
