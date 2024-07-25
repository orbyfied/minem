package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;

/**
 * Base interface for packet data.
 */
public interface PacketData {

    /** Read the packet data from the given buffer. */
    void read(Packet container, UnsafeByteBuf in) throws Exception;

    /** Write the packet data to the given buffer. */
    void write(Packet container, UnsafeByteBuf out) throws Exception;

}
