package com.orbyfied.minem.protocol;

import com.orbyfied.minem.buffer.UnsafeByteBuf;

/**
 * Base interface for packet data.
 */
public interface SerializablePacketData {

    /** Read the packet data from the given buffer. */
    void read(PacketContainer container, UnsafeByteBuf in) throws Exception;

    /** Write the packet data to the given buffer. */
    void write(PacketContainer container, UnsafeByteBuf out) throws Exception;

}
