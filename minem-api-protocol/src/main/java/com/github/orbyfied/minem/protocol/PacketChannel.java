package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.event.Chain;

/**
 * Represents either a {@link PacketSource} or {@link PacketSink}.
 */
public interface PacketChannel {

    /**
     * Whether this packet channel is open.
     */
    boolean isOpen();

    /**
     * Tries to close this packet channel.
     *
     * @return Whether it closed an open channel.
     */
    boolean close();

    /**
     * Event: called when a packet is sent or received.
     */
    Chain<PacketHandler> onPacket();

}
