package com.orbyfied.minem.io;

import java.io.IOException;

@FunctionalInterface
public interface IOByteSupplier {
    // Value is an int from 0-255, can be cast to a byte
    // Makes no difference in the stack frame as its all 32-bits+
    int getByte() throws IOException;

    default byte getAsByte() throws IOException {
        return (byte) getByte();
    }
}
