package com.orbyfied.minem.protocol;

import com.orbyfied.minem.reflect.UnsafeFieldDesc;
import com.orbyfied.minem.buffer.UnsafeByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Represents the mapping of a packet type from a universal representation to
 * a version specific implementation.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PacketMapping {

    protected Class<?> declaringClass;             // The class declaring this mapping (optional)
    protected int registryId;                      // The numerical registry ID of this packet type
    protected int networkId;                       // The ID on the network for this mapping
    protected ProtocolPhase phase;                 // The game phase in which this mapping occurs
    protected int flags;                           // The flags on this mapping, this consists of mapping flags and Packet flags
    protected String primaryName;                  // The primary name of this mapping
    protected String[] aliases;                    // The aliases of this mapping
    protected Class<?> dataClass;                  // The data class of this mapping
    protected MethodHandle constructor;            // The constructor to be used when building packet data
    protected Set<Class<?>> dataInterfaces;        // All interfaces/superclasses the data class implements
    protected Map<String, UnsafeFieldDesc> fields; // All compiled fields on the mapping (excludes transient)
    protected Destination destination;             // Where packets are bound
    protected MethodHandle methodDataRead;         // Fast method handle for method `void read(Object data, Packet packet, ByteBuf buf)`
    protected MethodHandle methodDataWrite;        // Fast method handle for method `void write(Object data, Packet packet, ByteBuf buf)`

    public static final int FLAG_STATIC = 1 << 16;

    @Override
    public String toString() {
        return "PacketMapping(ID 0x" + Integer.toHexString(registryId) + " " + phase + ":" + destination + ":0x" + Integer.toHexString(networkId) + " named: " + primaryName + ", declared by " + declaringClass.getSimpleName() + " with data class: " + dataClass.getSimpleName() + ")";
    }

    public void writePacketData(PacketContainer packet, UnsafeByteBuf buf) {
        try {
            methodDataWrite.invoke(packet.data, packet, buf);
        } catch (Throwable ex) {
            Throwables.sneakyThrow(ex);
        }
    }

    public void readPacketData(PacketContainer packet, UnsafeByteBuf buf) {
        try {
            methodDataRead.invoke(packet.data, packet, buf);
        } catch (Throwable ex) {
            ex.printStackTrace();
            Throwables.sneakyThrow(ex);
        }
    }

    /**
     * Create a new packet container without any data.
     *
     * @param context The protocol/packet context.
     * @return The packet container.
     */
    public PacketContainer createPacketContainer(ProtocolContext context) {
        PacketContainer packet = new PacketContainer();
        packet.context = context;
        packet.flags = this.flags;
        packet.networkId = this.networkId;
        packet.phase = this.phase;
        packet.mapping = this;
        packet.protocolVersion = context.getProtocol().getProtocolNumber();
        return packet;
    }

    /**
     * Create a new packet container with the default packet data.
     *
     * @param context The protocol/packet context.
     * @return The packet container.
     */
    public PacketContainer createPacketContainerWithData(ProtocolContext context) {
        try {
            return createPacketContainer(context).withData(constructor.invoke());
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            return null;
        }
    }

    public static PacketMapping compileMapping(Class<?> klass) {
        Mapping annotation = klass.getAnnotation(Mapping.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class " + klass.getName() + " which is to be compiled to a PacketMapping does not have " +
                    "a @Mapping annotation");
        }

        // check for static
        if (annotation.dataClass() != Void.class || (annotation.flags() & FLAG_STATIC) > 0 ||
            Modifier.isFinal(klass.getModifiers())) {
            return compileStaticMapping(klass, annotation);
        }

        // compile data mapping
        return compileDataMapping(klass, annotation);
    }

    /**
     * Compile the given class to a packet mapping from the unique data class using:
     * - The {@link Mapping} annotation,
     * - The methods in the {@link SerializablePacketData} interface and
     * - An empty constructor for construction
     */
    public static PacketMapping compileDataMapping(Class<?> klass, Mapping annotation) {
        try {
            // find primary name
            String primaryName = annotation.primaryName().isEmpty() ?
                    klass.getSimpleName().replace("Packet", "").replaceAll("[0-9]", "") :
                    annotation.primaryName();

            // find methods
            MethodHandles.Lookup lookup = UnsafeUtil.getInternalLookup();
            MethodHandle constructor = lookup.findConstructor(klass, MethodType.methodType(void.class));

            var mType = MethodType.methodType(void.class, PacketContainer.class, UnsafeByteBuf.class);
            MethodHandle methodRead = lookup.findVirtual(klass, "read", mType);
            MethodHandle methodWrite = lookup.findVirtual(klass, "write", mType);

            // find data interfaces
            Set<Class<?>> dataItf = new HashSet<>(List.of(klass.getInterfaces()));
            dataItf.add(klass);
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
                destination = primaryName.toLowerCase().startsWith("serverbound") ?
                        Destination.SERVERBOUND : Destination.CLIENTBOUND;
            }

            int networkId = annotation.id();
            return new PacketMapping(klass, PacketRegistry.getRegistryID(networkId, destination), networkId, annotation.phase(), annotation.flags(),
                    primaryName, annotation.aliases(), klass, constructor, dataItf,
                    fieldMap, destination, methodRead, methodWrite);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compile mapping " + klass, ex);
        }
    }

    /**
     * Compile the given class to a packet mapping from the unique data class using:
     * - The {@link Mapping} annotation,
     * - The methods in the {@link SerializablePacketData} interface statically implemented and
     * - An empty constructor on the data class for construction
     */
    public static PacketMapping compileStaticMapping(Class<?> klass, Mapping annotation) {
        try {
            // get data class
            Class<?> dataClass = annotation.dataClass();
            if (dataClass == Void.class) {
                if ((dataClass = klass.getSuperclass()) == Void.class || dataClass == Object.class || dataClass == null) {
                    throw new IllegalArgumentException("Static mapping by class " + klass.getName() + " does not have a data class specified");

                }
            }

            // find primary name
            String primaryName = annotation.primaryName().isEmpty() ?
                    klass.getSimpleName().replace("Packet", "") :
                    annotation.primaryName();

            MethodHandles.Lookup lookup = UnsafeUtil.getInternalLookup();

            // find data constructor on data class
            MethodHandle constructor = lookup.findConstructor(dataClass, MethodType.methodType(void.class));

            // find static serialization methods
            MethodHandle methodRead = lookup.findStatic(klass, "read", MethodType.methodType(void.class, dataClass, PacketContainer.class, UnsafeByteBuf.class));
            MethodHandle methodWrite = lookup.findStatic(klass, "write", MethodType.methodType(void.class, dataClass, PacketContainer.class, UnsafeByteBuf.class));

            // find data interfaces on data class
            Set<Class<?>> dataItf = new HashSet<>(List.of(dataClass.getInterfaces()));
            dataItf.add(dataClass);
            if (dataClass.getSuperclass() != Object.class) {
                dataItf.add(dataClass.getSuperclass());
            }

            // compile fields on data class
            Map<String, UnsafeFieldDesc> fieldMap = new HashMap<>();
            for (Field field : dataClass.getFields()) {
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
            return new PacketMapping(klass, PacketRegistry.getRegistryID(networkId, destination), networkId, annotation.phase(), annotation.flags(),
                    primaryName, annotation.aliases(), dataClass, constructor, dataItf,
                    fieldMap, destination, methodRead, methodWrite);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compile mapping " + klass, ex);
        }
    }

    // todo: compile class with static serializer methods but common
    //  packet data type into a mapping (possible through the read and write
    //  method handles)

}
