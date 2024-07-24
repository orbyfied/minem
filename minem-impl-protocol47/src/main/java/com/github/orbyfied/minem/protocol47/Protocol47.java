package com.github.orbyfied.minem.protocol47;

import com.github.orbyfied.minem.protocol.CommonPacketImplementations;
import com.github.orbyfied.minem.protocol.PacketUtil;
import com.github.orbyfied.minem.protocol.Protocol;
import com.github.orbyfied.minem.protocol47.play.ClientboundChatMessagePacket;

public class Protocol47 {

    /**
     * The protocol object.
     */
    public static final Protocol PROTOCOL = Protocol.create(47);

    // All defined packet classes
    private static final Class<?>[] CLASSES = new Class[] {
            ClientboundChatMessagePacket.class
    };

    static {
        PROTOCOL.registerPacketMappings(CommonPacketImplementations.MAPPINGS);
        PROTOCOL.registerPacketMappings(PacketUtil.compileAll(CLASSES));
    }

}
