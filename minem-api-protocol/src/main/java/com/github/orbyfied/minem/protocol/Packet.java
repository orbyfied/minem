package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.Context;
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

    int protocolVersion;         // The protocol version
    public int networkId;        // The read/written numeric ID of the packet
    public ProtocolPhase phase;  // The phase the packet was created/read in
    int flags;                   // The base flags for the packet

    ProtocolContext context;     // The context of this packet
    PacketSource source;         // The source of the packet, or constructed if null
    PacketMapping mapping;       // The packet mapping

    Object data;                 // The deserialized data of the packet

    public void reset() {
        this.data = null;
        this.flags = 0;
        this.networkId = 0;
    }

    public Packet source(PacketSource source) {
        this.source = source;
        return this;
    }

    public boolean isConstructed() {
        return source == null;
    }

    public boolean check(int flag) {
        return (flags & flag) > 0;
    }

    public boolean is(Class<?> klass) {
        return klass.isInstance(data);
    }

    public Packet withData(Object data) {
        this.data = data;
        return this;
    }

    public Packet set(int flag) {
        flags |= flag;
        return this;
    }

    public Packet clear(int flag) {
        flags &= ~flag;
        return this;
    }

    public Packet cancel(boolean b) {
        if (b) set(CANCEL);
        else clear(CANCEL);
        return this;
    }

    public Context context() {
        return context;
    }

    public ProtocolContext protocolContext() {
        return context;
    }

    public Protocol protocol() {
        return context.getProtocol();
    }

    @SuppressWarnings("unchecked")
    public <C extends Context> C context(Class<C> cClass) {
        if (!cClass.isInstance(context)) {
            throw new IllegalStateException("Expected game context of type " + cClass.getName());
        }

        return (C) context;
    }

    @SuppressWarnings("unchecked")
    public <T> T data() {
        return (T) data;
    }

    @SuppressWarnings("unchecked")
    public <D> D data(Class<D> cClass) {
        if (!cClass.isInstance(data)) {
            throw new IllegalStateException("Expected packet data of type " + cClass.getName());
        }

        return (D) data;
    }

    public boolean isUnknown() {
        return data instanceof UnknownPacket;
    }

}
