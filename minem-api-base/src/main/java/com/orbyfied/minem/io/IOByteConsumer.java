package com.orbyfied.minem.io;

import java.io.IOException;

@FunctionalInterface
public interface IOByteConsumer {
    // Value is an int from 0-255, can be cast to a byte
    // Makes no difference in the stack frame as its all 32-bits+
    void acceptByte(int b) throws IOException;
}
