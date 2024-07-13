package com.github.orbyfied.minem.util;

import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Protocol friendly dynamic sized byte buffer.
 */
public class ByteBuf {

    public static ByteBuf create(int capacity) {
        return new ByteBuf().reallocate(capacity);
    }

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    int readIndex = 0;  // The current read offset in bytes into the buffer
    int writeIndex = 0; // The current write offset in bytes into the buffer
    int capacity = 0;
    long ptr;

    public void clear() {
        writeIndex = 0;
        readIndex = 0;
    }

    public void free() {
        if (ptr != 0) {
            UNSAFE.freeMemory(ptr);
        }
    }

    /**
     * Reallocate this byte buffer.
     *
     * @param capacity The target capacity.
     */
    public ByteBuf reallocate(int capacity) {
        long newPtr = UNSAFE.allocateMemory(capacity);
        if (ptr != 0) {
            UNSAFE.copyMemory(ptr, newPtr, this.capacity);
        }

        this.capacity = capacity;
        this.ptr = newPtr;
        return this;
    }

    public boolean hasMoreCapacity() {
        return hasMoreCapacity(1);
    }

    public boolean hasMoreCapacity(int amt) {
        return readIndex + amt < capacity;
    }

    public boolean hasMoreWritten() {
        return hasMoreCapacity(1);
    }

    public boolean hasMoreWritten(int amt) {
        return readIndex + amt < writeIndex;
    }

    public int remainingReadCapacity() {
        return capacity - readIndex;
    }

    public int remainingWriteCapacity() {
        return capacity - writeIndex;
    }

    public int remainingWritten() {
        return writeIndex - readIndex;
    }

    // check if we have enough capacity to read the given amount of bytes
    private void checkReadCap(int amt) {
        if (readIndex + amt >= capacity) {
            throw new IllegalStateException("Can not read " + amt + " more bytes, buffer only has " + (capacity - readIndex) + " capacity remaining");
        }
    }

    // ensure we have enough capacity to write the given amount of bytes
    private void ensureWriteCap(int amt) {
        if (writeIndex + amt >= capacity) {
            reallocate((int) (capacity * 1.5));
        }
    }

    // advance the writer by the given amount
    private void advWriter(int amt) {
        writeIndex += amt;
        if (writeIndex >= capacity) {
            reallocate((int) (capacity * 1.5));
        }
    }

    public long getLong(int offset) { return UNSAFE.getLong(this.ptr + offset); }
    public int getInt(int offset) { return UNSAFE.getInt(this.ptr + offset); }
    public float getFloat(int offset) { return UNSAFE.getFloat(this.ptr + offset); }
    public double getDouble(int offset) { return UNSAFE.getDouble(this.ptr + offset); }
    public short getShort(int offset) { return UNSAFE.getShort(this.ptr + offset); }
    public char getChar(int offset) { return UNSAFE.getChar(this.ptr + offset); }
    public byte getByte(int offset) { return UNSAFE.getByte(this.ptr + offset); }
    public boolean getBoolean(int offset) { return getByte(offset) > 0; }

    public long readLong() { checkReadCap(8); var r = getLong(readIndex); readIndex += 8; return r; }
    public int readInt() { checkReadCap(4); var r = getInt(readIndex); readIndex += 4; return r; }
    public double readDouble() { checkReadCap(8); var r = getDouble(readIndex); readIndex += 8; return r; }
    public float readFloat() { checkReadCap(4); var r = getFloat(readIndex); readIndex += 4; return r; }
    public short readShort() { checkReadCap(2); var r = getShort(readIndex); readIndex += 2; return r; }
    public char readChar() { checkReadCap(2); var r = getChar(readIndex); readIndex += 2; return r; }
    public byte readByte() { checkReadCap(1); var r = getByte(readIndex); readIndex += 1; return r; }
    public boolean readBoolean() { checkReadCap(1); return readByte() > 0; }

    public void setLong(int offset, long val) { UNSAFE.putLong(this.ptr + offset, val); }
    public void setInt(int offset, int val) { UNSAFE.putInt(ptr + offset, val); }
    public void setFloat(int off, float val) { UNSAFE.putFloat(ptr + off, val); }
    public void setDouble(int o, double v) { UNSAFE.putDouble(ptr + o, v); }
    public void setShort(int o, short v) { UNSAFE.putShort(ptr + o, v); }
    public void setChar(int o, char v) { UNSAFE.putChar(ptr + o, v); }
    public void setByte(int o, byte v) { UNSAFE.putByte(ptr + o, v); }
    public void setBoolean(int o, boolean v) { setByte(o, (byte) (v ? 1 : 0)); }

    public void writeLong(long val) { ensureWriteCap(8); setLong(writeIndex, val); advWriter(8); }
    public void writeInt(int val) { ensureWriteCap(4); setInt(writeIndex, val); advWriter(4); }
    public void writeDouble(long val) { ensureWriteCap(8); setDouble(writeIndex, val); advWriter(8); }
    public void writeFloat(float val) { ensureWriteCap(4); setFloat(writeIndex, val); advWriter(4); }
    public void writeShort(short val) { ensureWriteCap(2); setShort(writeIndex, val); advWriter(2); }
    public void writeChar(char val) { ensureWriteCap(2); setChar(writeIndex, val); advWriter(2); }
    public void writeByte(byte val) { ensureWriteCap(1); setByte(writeIndex, val); advWriter(1); }
    public void writeBoolean(boolean val) { ensureWriteCap(1); setBoolean(writeIndex, val); advWriter(1); }

    public void getBytes(int offset, byte[] bytes, int destOff, int len) {
        if (offset + len >= capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        if (destOff + len >= bytes.length)
            throw new IllegalArgumentException("destOff + len > destLen, out of array bounds");
        UNSAFE.copyMemory(ptr + offset, getAddressOfObject(bytes) + BASE_OFF_BYTE_ARRAY + destOff, len);
    }

    public void getBytes(int offset, long address, int destOff, int len) {
        if (offset + len >= capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        UNSAFE.copyMemory(ptr + offset, address + destOff, len);
    }

    public void getBytes(int offset, byte[] bytes) {
        getBytes(offset, bytes, 0, bytes.length);
    }

    public void setBytes(int offset, byte[] bytes, int srcOff, int len) {
        if (offset + len >= capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        if (srcOff + len >= bytes.length)
            throw new IllegalArgumentException("srcOff + len > destLen, out of array bounds");
        UNSAFE.copyMemory(getAddressOfObject(bytes) + BASE_OFF_BYTE_ARRAY + srcOff, ptr + offset, len);
    }

    public void setBytes(int offset, long address, int srcOff, int len) {
        if (offset + len >= capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        UNSAFE.copyMemory(address + srcOff, ptr + offset, len);
    }

    public void setBytes(int offset, byte[] bytes) {
        setBytes(offset, bytes, 0, bytes.length);
    }

    static final int bufSize = 1024;

    public int setFrom(InputStream stream, int len, int offset) {
        try {
            int read = 0;
            byte[] buffer = new byte[bufSize];
            long addr = getAddressOfObject(buffer) + BASE_OFF_BYTE_ARRAY;
            while (read <= len) {
                int r = stream.read(buffer);
                read += r;

                setBytes(offset, addr, 0, r);
                offset += r;
            }

            return read;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            return 0;
        }
    }

    public int writeFrom(InputStream stream, int len) {
        try {
            int read = 0;
            byte[] buffer = new byte[bufSize];
            long addr = getAddressOfObject(buffer) + BASE_OFF_BYTE_ARRAY;
            while (read <= len) {
                int r = stream.read(buffer);
                read += r;

                ensureWriteCap(r);
                setBytes(writeIndex, addr, 0, r);
                writeIndex += r;
            }

            return read;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            return 0;
        }
    }

    public int getTo(OutputStream stream, int len, int offset) {
        try {
            int read = 0;
            byte[] buffer = new byte[bufSize];
            long addr = getAddressOfObject(buffer) + BASE_OFF_BYTE_ARRAY;
            while (read <= len) {
                int remaining = capacity - offset;
                if (remaining < 0) {
                    throw new IllegalStateException("No read capacity remaining");
                }

                int r = Math.min(remaining, bufSize);
                getBytes(offset, addr, 0, r);
                stream.write(buffer);

                read += r;
                offset += r;
            }

            return read;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            return 0;
        }
    }

    public int readTo(OutputStream stream, int len) {
        int read = getTo(stream, len, readIndex);
        readIndex += read;
        return read;
    }

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    public void writeString(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;
        ensureWriteCap(len + 5 /* var int */);
        writeVarInt(len);
        setBytes(writeIndex, bytes);
        advWriter(len);
    }

    public String readString() {
        int len = readVarInt();
        checkReadCap(len);
        byte[] bytes = new byte[len];
        getBytes(readIndex, bytes);
        readIndex += len;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public UUID readUUID() {
        checkReadCap(16);
        long msb = getLong(readIndex);
        long lsb = getLong(readIndex + 8);
        readIndex += 16;
        return new UUID(msb, lsb);
    }

    public void writeUUID(UUID uuid) {
        ensureWriteCap(16);
        setLong(writeIndex, uuid.getMostSignificantBits());
        setLong(writeIndex, uuid.getLeastSignificantBits());
        advWriter(16);
    }

    public int readVarInt() {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = readByte();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    public long readVarLong() {
        long value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = readByte();
            value |= (long) (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 64) throw new RuntimeException("VarLong is too big");
        }

        return value;
    }

    public void writeVarInt(int value) {
        ensureWriteCap(5);
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                writeByte((byte) value);
                return;
            }

            writeByte((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            value >>>= 7;
        }
    }

    public void writeVarLong(long value) {
        ensureWriteCap(10);
        while (true) {
            if ((value & ~((long) SEGMENT_BITS)) == 0) {
                writeByte((byte) value);
                return;
            }

            writeByte((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            value >>>= 7;
        }
    }

    public int capacity() {
        return capacity;
    }

    public int readIndex() {
        return readIndex;
    }

    public int writeIndex() {
        return writeIndex;
    }

    public ByteBuf writeIndex(int writeIndex) {
        this.writeIndex = writeIndex;
        return this;
    }

    public ByteBuf readIndex(int readIndex) {
        this.readIndex = readIndex;
        return this;
    }

    public long ptr() {
        return ptr;
    }

    static final long BASE_OFF_BYTE_ARRAY = UNSAFE.arrayBaseOffset(byte[].class);
    static final long BASE_OFF_OBJ_ARRAY = UNSAFE.arrayBaseOffset(Object[].class);

    public long getAddressOfObject(Object obj) {
        Object helperArray[] = new Object[1];
        helperArray[0] = obj;
        return UNSAFE.getLong(helperArray, BASE_OFF_OBJ_ARRAY);
    }

}
