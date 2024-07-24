package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.protocol.handshake.ClientboundStatusPacket;
import com.github.orbyfied.minem.protocol.handshake.ServerboundHandshakePacket;
import com.github.orbyfied.minem.protocol.login.*;
import com.github.orbyfied.minem.protocol.play.ClientboundKeepAlivePacket;
import com.github.orbyfied.minem.protocol.play.ServerboundKeepAlivePacket;

/**
 * Registry of all common packet implementations, *not interfaces*.
 */
public final class CommonPacketImplementations {

    public static final Class<?>[] CLASSES = new Class[] {
            ServerboundHandshakePacket.class,
            ClientboundStatusPacket.class,
            ServerboundLoginStartPacket.class,
            ClientboundLoginDisconnectPacket.class,
            ClientboundEncryptionRequestPacket.class,
            ServerboundEncryptionResponsePacket.class,
            ClientboundLoginSuccessPacket.class,
            ClientboundSetCompressionPacket.class,
            ClientboundKeepAlivePacket.class,
            ServerboundKeepAlivePacket.class
    };

    public static final PacketMapping[] MAPPINGS = PacketUtil.compileAll(CLASSES);

}
