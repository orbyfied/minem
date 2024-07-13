package com.github.orbyfied.minem;

import com.github.orbyfied.minem.protocol.Packet;

final class DebugUtils {

    /**
     * Create debug info for the given packet.
     */
    public static String debugInfo(Packet packet) {
        return "  Packet(id: " + packet.getId() + " `" + packet.getData().getClass().getSimpleName() + "`)";
    }

}
