package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.buffer.ByteBuf;
import slatepowered.veru.reflect.UnsafeUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.List;

/**
 * An unmapped packet, allows for modification of the raw data.
 */
public class UnknownPacket implements PacketData {

    static final MethodHandle CONSTRUCTOR;

    static {
        try {
            CONSTRUCTOR = UnsafeUtil.getInternalLookup()
                    .findConstructor(UnknownPacket.class, MethodType.methodType(void.class));
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public static PacketMapping CLIENTBOUND_MAPPING = new PacketMapping(-1, -1, ProtocolPhases.UNDEFINED, 0,
            "ClientboundUnknown", new String[0], UnknownPacket.class, CONSTRUCTOR, List.of(), Collections.emptyMap(), Destination.CLIENTBOUND,
            null, null);
    public static PacketMapping SERVERBOUND_MAPPING = new PacketMapping(-1, -1, ProtocolPhases.UNDEFINED, 0,
            "ServerboundUnknown", new String[0], UnknownPacket.class, CONSTRUCTOR, List.of(), Collections.emptyMap(), Destination.SERVERBOUND,
            null, null);

    /**
     * The data buffer.
     */
    volatile ByteBuf buf;

    public ByteBuf buffer() {
        if (buf == null) {
            throw new IllegalStateException("Packet data buffer has been released");
        }

        return buf;
    }

    @Override
    public void read(Packet container, ByteBuf in) throws Exception {
        this.buf = in; // this should be cleared after synchronous packet handling
    }

    @Override
    public void write(Packet container, ByteBuf out) throws Exception {

    }

    public UnknownPacket buffer(ByteBuf buf) {
        this.buf = buf;
        return this;
    }

}
