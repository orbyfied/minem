package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.reflect.UnsafeFieldDesc;
import com.github.orbyfied.minem.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the mapping of a packet type from a universal representation to
 * a version specific implementation.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PacketMapping {

    protected int registryId;                      // The numerical registry ID of this packet type
    protected int networkId;                       // The ID on the network for this mapping
    protected ProtocolPhase phase;                 // The game phase in which this mapping occurs
    protected int flags;                           // The flags on this mapping, this consists of mapping flags and Packet flags
    protected String primaryName;                  // The primary name of this mapping
    protected String[] aliases;                    // The aliases of this mapping
    protected Class<?> dataClass;                  // The data class of this mapping
    protected MethodHandle constructor;            // The constructor to be used when building packet data
    protected List<Class<?>> dataInterfaces;       // All interfaces/superclasses the data class implements
    protected Map<String, UnsafeFieldDesc> fields; // All compiled fields on the mapping (excludes transient)
    protected Destination destination;             // Where packets are bound
    protected MethodHandle methodDataRead;         // Fast method handle for method `void read(Object data, Packet packet, ByteBuf buf)`
    protected MethodHandle methodDataWrite;        // Fast method handle for method `void write(Object data, Packet packet, ByteBuf buf)`

    public void writePacketData(Packet packet, ByteBuf buf) {
        try {
            methodDataWrite.invoke(packet.data, packet, buf);
        } catch (Throwable ex) {
            Throwables.sneakyThrow(ex);
        }
    }

    public void readPacketData(Packet packet, ByteBuf buf) {
        try {
            methodDataRead.invoke(packet.data, packet, buf);
        } catch (Throwable ex) {
            Throwables.sneakyThrow(ex);
        }
    }

    /**
     * Create a new packet container with the default packet data.
     *
     * @param context The protocol/packet context.
     * @return The packet container.
     */
    public Packet createPacketContainer(ProtocolContext context) {
        try {
            Packet packet = new Packet();
            packet.context = context;
            packet.data = constructor.invoke();
            packet.flags = this.flags;
            packet.networkId = this.networkId;
            packet.phase = this.phase;
            packet.mapping = this;
            packet.protocolVersion = context.getProtocol().getProtocolNumber();
            return packet;
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            return null;
        }
    }

    /**
     * Compile the given class to a packet mapping from the data class using:
     * - The {@link Mapping} annotation,
     * - The methods in the {@link PacketData} interface and
     * - An empty constructor for construction of
     */
    public static PacketMapping compile(Class<?> klass) {
        try {
            Mapping annotation = klass.getAnnotation(Mapping.class);
            if (annotation == null) {
                throw new IllegalArgumentException("Class " + klass.getName() + " which is to be compiled to a PacketMapping does not have " +
                        "a @Mapping annotation");
            }

            // find primary name
            String primaryName = annotation.primaryName().isEmpty() ?
                    klass.getSimpleName().replace("Packet", "") :
                    annotation.primaryName();

            // find methods
            MethodHandles.Lookup lookup = UnsafeUtil.getInternalLookup();
            MethodHandle constructor = lookup.findConstructor(klass, MethodType.methodType(void.class));

            var mType = MethodType.methodType(void.class, Packet.class, ByteBuf.class);
            MethodHandle methodRead = lookup.findVirtual(klass, "read", mType);
            MethodHandle methodWrite = lookup.findVirtual(klass, "write", mType);

            // find data interfaces
            List<Class<?>> dataItf = new ArrayList<>(List.of(klass.getInterfaces()));
            if (klass.getSuperclass() != Object.class) {
                dataItf.add(klass.getSuperclass());
            }

            // compile fields
            Map<String, UnsafeFieldDesc> fieldMap = new HashMap<>();
            for (Field field : klass.getFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
                    continue;
                }

                fieldMap.put(field.getName(), UnsafeFieldDesc.forField(field));
            }

            Destination destination = annotation.dest();
            if (destination == Destination.FIND) {
                destination = annotation.primaryName().toLowerCase().startsWith("serverbound") ?
                        Destination.SERVERBOUND : Destination.CLIENTBOUND;
            }

            int networkId = annotation.id();
            return new PacketMapping(PacketRegistry.getRegistryID(networkId, destination), networkId, annotation.phase(), annotation.flags(),
                    primaryName, annotation.aliases(), klass, constructor, dataItf,
                    fieldMap, destination, methodRead, methodWrite);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compile mapping " + klass, ex);
        }
    }

    // todo: compile class with static serializer methods but common
    //  packet data type into a mapping (possible through the read and write
    //  method handles)

}
