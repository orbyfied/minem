package com.github.orbyfied.minem.buffer;

import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class Memory {

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();
    static final MethodHandles.Lookup LOOKUP = UnsafeUtil.getInternalLookup();

    public static final Cleaner CLEANER = Cleaner.create();
    public static float GROW_FACTOR = 1.5f;

    /* Direct buffer instantiation */
    static final Class<?> classDirectByteBuffer;
    static final long fieldOffsetAddress;
    static final long fieldOffsetCapacity;

    static {
        try {
            classDirectByteBuffer = Class.forName("java.nio.DirectByteBuffer");
            fieldOffsetAddress = UNSAFE.objectFieldOffset(Buffer.class.getDeclaredField("address"));
            fieldOffsetCapacity = UNSAFE.objectFieldOffset(Buffer.class.getDeclaredField("capacity"));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Ensure the given byte array has at least the given capacity.
     *
     * It does this by checking the current capacity, and if it's to small
     * it allocates a new buffer with the requested capacity multiplied by
     * {@link #GROW_FACTOR}, copies the old contents into it and returns it.
     *
     * @param in The input buffer.
     * @param capacity The requested capacity.
     * @return The output buffer, different from the input if reallocated.
     */
    public static byte[] ensureByteArrayCapacity(byte[] in, int capacity) {
        if (in.length < capacity) {
            byte[] old = in;
            in = new byte[(int) (capacity * GROW_FACTOR)];
            System.arraycopy(old, 0, in, 0, old.length);
        }

        return in;
    }

    /**
     * Reserve the byte order of the given extracted double.
     *
     * @param d The double.
     * @return The double with reversed bytes.
     */
    public static double reverseByteOrder(double d) {
        return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(d)));
    }

    /**
     * Wrap the given native memory address using a NIO {@link ByteBuffer},
     * more specifically a {@link java.nio.DirectByteBuffer} instantiated using
     * Unsafe.
     *
     * @param addr The address to wrap.
     * @param capacity The capacity of the buffer.
     * @return The buffer.
     */
    public static ByteBuffer wrapAddressAsByteBuffer(long addr, int capacity) {
        try {
            ByteBuffer buffer = (ByteBuffer) UNSAFE.allocateInstance(classDirectByteBuffer);
            UNSAFE.putLong(buffer, fieldOffsetAddress, addr);
            UNSAFE.putInt(buffer, fieldOffsetCapacity, capacity);
            buffer.position(0);
            buffer.limit(capacity);
            return buffer;
        } catch (Exception e) {
            Throwables.sneakyThrow(e);
            throw new AssertionError();
        }
    }

}
