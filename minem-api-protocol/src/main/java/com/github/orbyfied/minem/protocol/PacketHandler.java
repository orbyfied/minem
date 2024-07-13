package com.github.orbyfied.minem.protocol;

@FunctionalInterface
public interface PacketHandler {

    /**
     * Handle the given packet.
     *
     * @return An integer with the necessary action flags set.
     */
    int onPacket(Packet packet);

}
