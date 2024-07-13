package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.event.Chain;

/**
 * Represents the source of a packet, this could be a connection.
 */
public interface PacketSource extends PacketChannel {

    /**
     * How many packets have been received.
     */
    int countReceived();

    /**
     * Event: called when a packet was received and is about to be processed.
     */
    Chain<PacketHandler> onPacketReceived();

}
