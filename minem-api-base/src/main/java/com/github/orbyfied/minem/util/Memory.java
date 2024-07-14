package com.github.orbyfied.minem.util;

public class Memory {

    public static byte[] ensureByteArrayCapacity(byte[] in, int capacity) {
        if (in.length < capacity) {
            byte[] old = in;
            in = new byte[(int) (capacity * 1.5)];
            System.arraycopy(old, 0, in, 0, old.length);
        }

        return in;
    }

}
