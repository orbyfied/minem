package com.github.orbyfied.minem.util;

public class Memory {

    public static float GROW_FACTOR = 1.5f;

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

}
