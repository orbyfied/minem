package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.Context;
import com.github.orbyfied.minem.util.ByteBuf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents an instance of a packet. Note that this serves more as a container, and not
 * as a base class for packet data. The packet data is contained in the data field.
 */
@RequiredArgsConstructor
@Getter
public class Packet {

    /**
     * Base packet flag.
     *
     * Indicates the packet is inbound.
     */
    public static int INBOUND = 1 << 1;

    /**
     * Base packet flag.
     *
     * Indicates the packet is outbound.
     */
    public static int OUTBOUND = 1 << 2;

    /**
     * Base packet flag.
     *
     * Indicates the packet should be cancelled.
     */
    public static int CANCEL = 1 << 3;

    int id;                  // The read numeric ID of the packet
    ProtocolPhase phase;     // The phase the packet was created/read in
    int flags;               // The base flags for the packet

    ProtocolContext context; // The context of this packet
    PacketSource source;     // The source of the packet, or constructed if null
    PacketMapping mapping;   // The packet mapping

    Object data;             // The deserialized data of the packet

    public boolean isConstructed() {
        return source == null;
    }

    public boolean check(int flag) {
        return (flags & flag) > 0;
    }

    public Packet set(int flag) {
        flags |= flag;
        return this;
    }

    public Packet clear(int flag) {
        flags &= ~flag;
        return this;
    }

    public Context context() {
        return context;
    }

    @SuppressWarnings("unchecked")
    public <C extends Context> C context(Class<C> cClass) {
        if (!cClass.isInstance(context)) {
            throw new IllegalStateException("Expected game context of type " + cClass.getName());
        }

        return (C) context;
    }

    public Object data() {
        return data;
    }

    @SuppressWarnings("unchecked")
    public <D> D data(Class<D> cClass) {
        if (!cClass.isInstance(context)) {
            throw new IllegalStateException("Expected packet data of type " + cClass.getName());
        }

        return (D) data;
    }

}
