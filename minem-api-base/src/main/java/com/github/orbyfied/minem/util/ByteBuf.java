package com.github.orbyfied.minem.util;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;

/**
 * Friendly byte buffer.
 */
@RequiredArgsConstructor
public class ByteBuf {

    final ByteBuffer buffer;

    public static ByteBuf createDirect(int size) {
        return new ByteBuf(ByteBuffer.allocateDirect(size));
    }

}
