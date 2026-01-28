package com.orbyfied.minem.protocol;

import com.orbyfied.minem.event.Chain;

/**
 * Represents a sink for packets, this could be a socket connection.
 */
public interface PacketSink extends PacketChannel {

    /**
     * How many packets have been sent.
     */
    int countSent();

    /**
     * Synchronously send the given packet.
     *
     * @param packet The packet.
     */
    void sendSync(PacketContainer packet);

    /**
     * Event: called when a packet will be sunk/sent.
     */
    Chain<PacketHandler> onPacketSink();

}
