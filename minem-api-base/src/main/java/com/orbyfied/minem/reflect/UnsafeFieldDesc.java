package com.orbyfied.minem.reflect;

import lombok.Data;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Describes the {@link sun.misc.Unsafe} related properties of a field.
 */
@Data
public final class UnsafeFieldDesc {

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    // primitive types
    public static final byte PT_REFERENCE = 0;
    public static final byte PT_LONG = 1;
    public static final byte PT_INT = 2;
    public static final byte PT_DOUBLE = 3;
    public static final byte PT_FLOAT = 4;
    public static final byte PT_SHORT = 5;
    public static final byte PT_BYTE = 6;
    public static final byte PT_BOOLEAN = 7;
    public static final byte PT_CHAR = 8;

    final Field field;           // The reflection field
    final String name;           // The origin name of the field
    final long offset;           // The object field offset
    final Type type;             // The generic type
    final byte primitiveType;    // The primitive type, must be one of the above defined constants
    final byte etcFlags;         // Other miscellaneous flags, such as @Nullable

    /**
     * Get the value in this field on the given instance, boxed if primitive.
     *
     * @param instance The instance to set it on.
     * @return The value.
     */
    public Object getAsObject(Object instance) {
        switch (primitiveType) {
            case PT_REFERENCE: return UNSAFE.getObject(instance, offset);
            case PT_LONG: return UNSAFE.getLong(instance, offset);
            case PT_INT: return UNSAFE.getInt(instance, offset);
            case PT_DOUBLE: return UNSAFE.getDouble(instance, offset);
            case PT_FLOAT: return UNSAFE.getFloat(instance, offset);
            case PT_SHORT: return UNSAFE.getShort(instance, offset);
            case PT_CHAR: return UNSAFE.getChar(instance, offset);
            case PT_BYTE: return UNSAFE.getByte(instance, offset);
            case PT_BOOLEAN: return UNSAFE.getBoolean(instance, offset);
        }

        throw new IllegalArgumentException("Goofy primitive type idk how to get that [byte primitiveType = " + primitiveType + "]");
    }

    /**
     * Set the value in this field on the given instance to the given
     * value, which will be unboxed if needed.
     *
     * @param instance The instance.
     * @param value The value.
     */
    public void setFromObject(Object instance, Object value) {
        switch (primitiveType) {
            case PT_REFERENCE: UNSAFE.putObject(instance, offset, value); break;
            case PT_LONG: UNSAFE.putLong(instance, offset, value != null ? (Long) value : 0); break;
            case PT_INT: UNSAFE.putInt(instance, offset, value != null ? (Integer) value : 0); break;
            case PT_DOUBLE: UNSAFE.putDouble(instance, offset, value != null ? (Double) value : 0); break;
            case PT_FLOAT: UNSAFE.putFloat(instance, offset, value != null ? (Float) value : 0); break;
            case PT_SHORT: UNSAFE.putShort(instance, offset, value != null ? (Short) value : 0); break;
            case PT_CHAR: UNSAFE.putChar(instance, offset, value != null ? (Character)value : 0); break;
            case PT_BYTE: UNSAFE.putByte(instance, offset, value != null ? (Byte) value : 0); break;
            case PT_BOOLEAN: UNSAFE.putBoolean(instance, offset, value != null ? (Boolean) value : false); break;
            default: throw new IllegalArgumentException("Goofy primitive type idk how to set that [byte primitiveType = " + primitiveType + "]");
        }
    }

    public static byte getPrimitiveType(Class<?> kl) {
        if (!kl.isPrimitive()) return PT_REFERENCE;
        if (kl == long.class) return PT_LONG;
        if (kl == int.class) return PT_INT;
        if (kl == double.class) return PT_DOUBLE;
        if (kl == float.class) return PT_FLOAT;
        if (kl == byte.class) return PT_BYTE;
        if (kl == short.class) return PT_SHORT;
        if (kl == boolean.class) return PT_BOOLEAN;
        if (kl == char.class) return PT_CHAR;

        throw new IllegalArgumentException("Weird primitive type " + kl);
    }

    public static UnsafeFieldDesc forField(Field field) {
        return new UnsafeFieldDesc(field, field.getName(), UNSAFE.objectFieldOffset(field), field.getGenericType(), getPrimitiveType(field.getType()), (byte) 0L);
    }

}
