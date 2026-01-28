package com.orbyfied.minem.protocol;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import slatepowered.veru.reflect.UnsafeUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Set;

/**
 * An unmapped packet, allows for modification of the raw data.
 */
public class UnknownPacket implements SerializablePacketData {

    static final MethodHandle CONSTRUCTOR;

    static {
        try {
            CONSTRUCTOR = UnsafeUtil.getInternalLookup()
                    .findConstructor(UnknownPacket.class, MethodType.methodType(void.class));
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public static PacketMapping CLIENTBOUND_MAPPING = new PacketMapping(UnknownPacket.class, -1, -1, ProtocolPhases.UNDEFINED, 0,
            "ClientboundUnknown", new String[0], UnknownPacket.class, CONSTRUCTOR, Set.of(), Collections.emptyMap(), Destination.CLIENTBOUND,
            null, null);
    public static PacketMapping SERVERBOUND_MAPPING = new PacketMapping(UnknownPacket.class, -1, -1, ProtocolPhases.UNDEFINED, 0,
            "ServerboundUnknown", new String[0], UnknownPacket.class, CONSTRUCTOR, Set.of(), Collections.emptyMap(), Destination.SERVERBOUND,
            null, null);

    /**
     * The data buffer.
     */
    volatile UnsafeByteBuf buf;

    public UnsafeByteBuf buffer() {
        if (buf == null) {
            throw new IllegalStateException("Packet data buffer has been released");
        }

        return buf;
    }

    @Override
    public void read(PacketContainer container, UnsafeByteBuf in) throws Exception {
        this.buf = in; // this should be cleared after synchronous packet handling
    }

    @Override
    public void write(PacketContainer container, UnsafeByteBuf out) throws Exception {

    }

    public UnknownPacket buffer(UnsafeByteBuf buf) {
        this.buf = buf;
        return this;
    }

}
