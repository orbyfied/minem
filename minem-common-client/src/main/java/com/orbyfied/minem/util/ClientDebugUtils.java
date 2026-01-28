package com.orbyfied.minem.util;

import com.orbyfied.minem.protocol.PacketContainer;

public final class ClientDebugUtils {

    /**
     * Create debug info for the given packet.
     */
    public static String debugInfo(PacketContainer packet) {
        return "Packet(id: " + packet.getNetworkId() + " `" + packet.getData().getClass().getSimpleName() + "`)";
    }

}
