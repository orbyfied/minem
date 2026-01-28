package com.orbyfied.minem.buffer;

/**
 * Allocates direct (native) memory for the buffer.
 */
public class UnsafeDirectByteBuf extends UnsafeByteBuf {

    public UnsafeDirectByteBuf() {

    }

    public UnsafeDirectByteBuf(long ptr, int cap) {
        this.ptr = ptr;
        this.capacity = cap;
    }

    @Override
    public void free() {
        if (ptr != 0) {
            UNSAFE.freeMemory(ptr);
            ptr = 0;
            capacity = 0;
        }

        this.nio0Offset = null;
    }

    @Override
    public UnsafeByteBuf reallocate(int capacity) {
        if ((flags & FLAG_FIXED_POINTER) > 0 && ptr != 0) {
            throw new UnsupportedOperationException("Can not reallocate fixed-pointer byte buffer");
        }

        long newPtr = UNSAFE.allocateMemory(capacity);
        if (ptr != 0) {
            UNSAFE.copyMemory(ptr, newPtr, this.capacity);
            UNSAFE.freeMemory(ptr);
        }

        this.capacity = capacity;
        this.ptr = newPtr;
        this.nio0Offset = null;
        return this;
    }

}
