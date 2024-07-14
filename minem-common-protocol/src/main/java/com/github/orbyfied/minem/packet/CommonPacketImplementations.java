package com.github.orbyfied.minem.packet;

import com.github.orbyfied.minem.protocol.PacketMapping;

/**
 * Registry of all common packet implementations, *not interfaces*.
 */
public final class CommonPacketImplementations {

    public static final Class<?>[] CLASSES = new Class[] {
            ServerboundHandshakePacket.class,
            ClientboundStatusPacket.class,
            ServerboundLoginStart.class,
            ClientboundLoginDisconnectPacket.class,
            ClientboundEncryptionRequestPacket.class,
            ServerboundEncryptionResponsePacket.class,
            ClientboundSetCompressionPacket.class
    };

    public static final PacketMapping[] MAPPINGS = PacketUtil.compileAll(CLASSES);

}
