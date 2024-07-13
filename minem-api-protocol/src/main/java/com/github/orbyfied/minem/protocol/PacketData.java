package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.util.ByteBuf;

/**
 * Base interface for packet data.
 */
public interface PacketData {

    /** Read the packet data from the given buffer. */
    void read(Packet container, ByteBuf in) throws Exception;

    /** Write the packet data to the given buffer. */
    void write(Packet container, ByteBuf out) throws Exception;

}
