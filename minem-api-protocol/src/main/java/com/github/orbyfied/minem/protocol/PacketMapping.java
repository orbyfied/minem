package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.reflect.UnsafeFieldDesc;
import com.github.orbyfied.minem.util.ByteBuf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.veru.reflect.UnsafeUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the mapping of a packet type from a universal representation to
 * a version specific implementation.
 */
@RequiredArgsConstructor
@Getter
public class PacketMapping {

    final int id;                              // The numerical ID of this packet type
    final int phase;                           // The game phase in which this mapping occurs
    final int flags;                           // The flags on this mapping, this consists of mapping flags and Packet flags
    final String primaryName;                  // The primary name of this mapping
    final String[] aliases;                    // The aliases of this mapping
    final Class<?> dataClass;                  // The data class of this mapping
    final MethodHandle constructor;            // The constructor to be used when building packet data
    final List<Class<?>> dataInterfaces;       // All interfaces/superclasses the data class implements
    final Map<String, UnsafeFieldDesc> fields; // All compiled fields on the mapping (excludes transient)
    final MethodHandle methodDataRead;
    final MethodHandle methodDataWrite;

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

            return new PacketMapping(annotation.id(), annotation.phase().ordinal(), annotation.flags(),
                    primaryName, annotation.aliases(), klass, constructor, dataItf,
                    fieldMap, methodRead, methodWrite);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compile mapping " + klass, ex);
        }
    }

}
