package com.github.orbyfied.minem.util;

import com.github.orbyfied.minem.protocol.Packet;

public final class ClientDebugUtils {

    /**
     * Create debug info for the given packet.
     */
    public static String debugInfo(Packet packet) {
        return "Packet(id: " + packet.getNetworkId() + " `" + packet.getData().getClass().getSimpleName() + "`)";
    }

}
