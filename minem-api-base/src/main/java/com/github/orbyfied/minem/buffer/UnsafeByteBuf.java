package com.github.orbyfied.minem.buffer;

import lombok.RequiredArgsConstructor;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Protocol friendly dynamic sizeable byte buffer which uses {@link Unsafe} to
 * directly access a heap or off-heap memory pointer.
 */
public abstract class UnsafeByteBuf {

    public static UnsafeByteBuf createDirect(int capacity) {
        return new UnsafeDirectByteBuf().reallocate(capacity);
    }

    public static UnsafeByteBuf fixed(long ptr, int capacity) {
        return new UnsafeDirectByteBuf(ptr, capacity).setFlags(FLAG_FIXED_POINTER);
    }

    public static UnsafeByteBuf fixedInto(byte[] byteArray) {
        return new UnsafeDirectByteBuf(getAddressOfObject((Object) byteArray) + BASE_OFF_BYTE_ARRAY, byteArray.length)
                .setFlags(FLAG_FIXED_POINTER);
    }

    /** The unsafe instance. */
    protected static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    /** The constructor for a direct byte buffer which takes an address. */
    static final MethodHandle constructorDirectNIOBuffer;

    static {
        try {
            constructorDirectNIOBuffer = UnsafeUtil.getInternalLookup()
                    .findConstructor(Class.forName("java.nio.DirectByteBuffer"), MethodType.methodType(void.class,
                            long.class /* addr */, int.class /* cap */));
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /** Flag: indicates the buffer should not be reallocated under any circumstances */
    public static int FLAG_FIXED_POINTER = 1 << 1;

    /**
     * The flags set on this buffer
     */
    protected int flags = 0;

    /**
     * The current read offset in bytes into the buffer.
     */
    protected int readIndex = 0;

    /**
     * The current write offset in bytes into the buffer.
     */
    protected int writeIndex = 0;

    /**
     * The capacity of the buffer.
     */
    protected int capacity = 0;

    /**
     * The direct (unsafe) pointer to the data of the buffer.
     */
    protected long ptr;

    {
        Memory.CLEANER.register(this, this::free);
    }

    @Override
    protected void finalize() throws Throwable {
        free();
    }

    /**
     * Reset the read and write offsets.
     */
    public final void reset() {
        writeIndex = 0;
        readIndex = 0;
    }

    /**
     * Free the data backing this buffer.
     */
    public abstract void free();

    /**
     * Reallocate this byte buffer.
     *
     * @param capacity The target capacity.
     */
    public abstract UnsafeByteBuf reallocate(int capacity);

    public final UnsafeByteBuf setFlags(int flags) {
        this.flags |= flags;
        return this;
    }

    public final UnsafeByteBuf clearFlags(int flags) {
        this.flags &= ~flags;
        return this;
    }

    public final UnsafeByteBuf resetFlags(int to) {
        this.flags = to;
        return this;
    }

    public final boolean hasMoreCapacity() {
        return hasMoreCapacity(1);
    }

    public final boolean hasMoreCapacity(int amt) {
        return readIndex + amt < capacity;
    }

    public final boolean hasMoreWritten() {
        return hasMoreCapacity(1);
    }

    public final boolean hasMoreWritten(int amt) {
        return readIndex + amt < writeIndex;
    }

    public final int remainingReadCapacity() {
        return capacity - readIndex;
    }

    public final int remainingWriteCapacity() {
        return capacity - writeIndex;
    }

    public final int remainingWritten() {
        return writeIndex - readIndex;
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

    public UnsafeByteBuf writeIndex(int writeIndex) {
        this.writeIndex = writeIndex;
        return this;
    }

    public UnsafeByteBuf readIndex(int readIndex) {
        this.readIndex = readIndex;
        return this;
    }

    public long pointer() {
        return ptr;
    }

    // check if we have enough capacity to read the given amount of bytes
    private void checkReadCap(int amt) {
        if (readIndex + amt >= capacity) {
            throw new IllegalStateException("Insufficient capacity to read " + amt + " bytes from " + this);
        }
    }

    // ensure we have at least the given capacity
    public final void ensureCapacity(int target) {
        if (target >= capacity) {
            if ((flags & FLAG_FIXED_POINTER) > 0) {
                throw new IllegalStateException("Insufficient capacity in fixed-ptr byte buffer (cap: " + capacity + ", target: " + target + ")");
            }

            reallocate((int) (target * 1.5));
        }
    }

    // ensure we have enough capacity to write the given amount of bytes
    public final void ensureWriteCapacity(int amt) {
        ensureCapacity(writeIndex + amt);
    }

    // advance the writer by the given amount
    private void advWriter(int amt) {
        writeIndex += amt;
        if (writeIndex >= capacity) {
            reallocate((int) (capacity * 1.5));
        }
    }

    /*
        Primitive Access
     */

    public final long getLong(int offset) { return UNSAFE.getLong(this.ptr + offset); }
    public final int getInt(int offset) { return UNSAFE.getInt(this.ptr + offset); }
    public final float getFloat(int offset) { return UNSAFE.getFloat(this.ptr + offset); }
    public final double getDouble(int offset) { return UNSAFE.getDouble(this.ptr + offset); }
    public final short getShort(int offset) { return UNSAFE.getShort(this.ptr + offset); }
    public final char getChar(int offset) { return UNSAFE.getChar(this.ptr + offset); }
    public final byte getByte(int offset) { return UNSAFE.getByte(this.ptr + offset); }
    public final boolean getBoolean(int offset) { return getByte(offset) > 0; }

    public final long getLongReversed(int offset) { return Long.reverseBytes(UNSAFE.getLong(this.ptr + offset)); }
    public final int getIntReversed(int offset) { return Integer.reverseBytes(UNSAFE.getInt(this.ptr + offset)); }
    public final float getFloatReversed(int offset) { return Float.intBitsToFloat(Integer.reverseBytes(UNSAFE.getInt(this.ptr + offset))); }
    public final double getDoubleReversed(int offset) { return Double.longBitsToDouble(Long.reverseBytes(UNSAFE.getLong(this.ptr + offset))); }
    public final short getShortReversed(int offset) { return Short.reverseBytes(UNSAFE.getShort(this.ptr + offset)); }
    public final char getCharReversed(int offset) { return Character.reverseBytes(UNSAFE.getChar(this.ptr + offset)); }

    public final long readLong() { checkReadCap(8); var r = getLong(readIndex); readIndex += 8; return r; }
    public final int readInt() { checkReadCap(4); var r = getInt(readIndex); readIndex += 4; return r; }
    public final double readDouble() { checkReadCap(8); var r = getDouble(readIndex); readIndex += 8; return r; }
    public final float readFloat() { checkReadCap(4); var r = getFloat(readIndex); readIndex += 4; return r; }
    public final short readShort() { checkReadCap(2); var r = getShort(readIndex); readIndex += 2; return r; }
    public final char readChar() { checkReadCap(2); var r = getChar(readIndex); readIndex += 2; return r; }
    public final byte readByte() { checkReadCap(1); var r = getByte(readIndex); readIndex += 1; return r; }
    public final boolean readBoolean() { checkReadCap(1); return readByte() > 0; }

    public final long readLongReversed() { checkReadCap(8); var r = getLongReversed(readIndex); readIndex += 8; return r; }
    public final int readIntReversed() { checkReadCap(4); var r = getIntReversed(readIndex); readIndex += 4; return r; }
    public final double readDoubleReversed() { checkReadCap(8); var r = getDoubleReversed(readIndex); readIndex += 8; return r; }
    public final float readFloatReversed() { checkReadCap(4); var r = getFloatReversed(readIndex); readIndex += 4; return r; }
    public final short readShortReversed() { checkReadCap(2); var r = getShortReversed(readIndex); readIndex += 2; return r; }
    public final char readCharReversed() { checkReadCap(2); var r = getCharReversed(readIndex); readIndex += 2; return r; }

    public final void setLong(int offset, long val) { UNSAFE.putLong(this.ptr + offset, val); }
    public final void setInt(int offset, int val) { UNSAFE.putInt(ptr + offset, val); }
    public final void setFloat(int off, float val) { UNSAFE.putFloat(ptr + off, val); }
    public final void setDouble(int o, double v) { UNSAFE.putDouble(ptr + o, v); }
    public final void setShort(int o, short v) { UNSAFE.putShort(ptr + o, v); }
    public final void setChar(int o, char v) { UNSAFE.putChar(ptr + o, v); }
    public final void setByte(int o, byte v) { UNSAFE.putByte(ptr + o, v); }
    public final void setBoolean(int o, boolean v) { setByte(o, (byte) (v ? 1 : 0)); }

    public final void setLongReversed(int offset, long val) { UNSAFE.putLong(this.ptr + offset, Long.reverse(val)); }
    public final void setIntReversed(int offset, int val) { UNSAFE.putInt(ptr + offset, Integer.reverseBytes(val)); }
    public final void setFloatReversed(int off, float val) { UNSAFE.putFloat(ptr + off, Integer.reverseBytes(Float.floatToIntBits(val))); }
    public final void setDoubleReversed(int o, double v) { UNSAFE.putDouble(ptr + o, Long.reverseBytes(Double.doubleToLongBits(v))); }
    public final void setShortReversed(int o, short v) { UNSAFE.putShort(ptr + o, Short.reverseBytes(v)); }
    public final void setCharReversed(int o, char v) { UNSAFE.putChar(ptr + o, Character.reverseBytes(v)); }

    public final void writeLong(long val) { ensureWriteCapacity(8); setLong(writeIndex, val); advWriter(8); }
    public final void writeInt(int val) { ensureWriteCapacity(4); setInt(writeIndex, val); advWriter(4); }
    public final void writeDouble(double val) { ensureWriteCapacity(8); setDouble(writeIndex, val); advWriter(8); }
    public final void writeFloat(float val) { ensureWriteCapacity(4); setFloat(writeIndex, val); advWriter(4); }
    public final void writeShort(short val) { ensureWriteCapacity(2); setShort(writeIndex, val); advWriter(2); }
    public final void writeChar(char val) { ensureWriteCapacity(2); setChar(writeIndex, val); advWriter(2); }
    public final void writeByte(byte val) { ensureWriteCapacity(1); setByte(writeIndex, val); advWriter(1); }
    public final void writeBoolean(boolean val) { ensureWriteCapacity(1); setBoolean(writeIndex, val); advWriter(1); }

    public final void writeLongReversed(long val) { ensureWriteCapacity(8); setLongReversed(writeIndex, val); advWriter(8); }
    public final void writeIntReversed(int val) { ensureWriteCapacity(4); setIntReversed(writeIndex, val); advWriter(4); }
    public final void writeDoubleReversed(double val) { ensureWriteCapacity(8); setDoubleReversed(writeIndex, val); advWriter(8); }
    public final void writeFloatReversed(float val) { ensureWriteCapacity(4); setFloatReversed(writeIndex, val); advWriter(4); }
    public final void writeShortReversed(short val) { ensureWriteCapacity(2); setShortReversed(writeIndex, val); advWriter(2); }
    public final void writeCharReversed(char val) { ensureWriteCapacity(2); setCharReversed(writeIndex, val); advWriter(2); }

    /**
     * Copy the {@code len} bytes from this buffer at offset {@code offset} into
     * the given byte array at index {@code destOff}.
     */
    public final void getBytes(int offset, byte[] bytes, int destOff, int len) {
        if (offset + len > capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        if (destOff + len > bytes.length)
            throw new IllegalArgumentException("destOff + len > destLen, out of array bounds");
        UNSAFE.copyMemory(ptr + offset, getAddressOfObject((Object) bytes) + BASE_OFF_BYTE_ARRAY + destOff, len);
    }

    /**
     * Copy the {@code len} bytes from this buffer at offset {@code offset} into
     * the given address at offset {@code destOff}.
     */
    public final void getBytes(int offset, long address, int destOff, int len) {
        if (offset + len > capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        UNSAFE.copyMemory(ptr + offset, address + destOff, len);
    }

    public final void getBytes(int offset, byte[] bytes) {
        getBytes(offset, bytes, 0, bytes.length);
    }

    /**
     * Copy the {@code len} bytes from the byte array at index {@code srcOff} into
     * this buffer at offset {@code offset}.
     */
    public final void setBytes(int offset, byte[] bytes, int srcOff, int len) {
        if (offset + len > capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        if (srcOff + len > bytes.length)
            throw new IllegalArgumentException("srcOff + len > destLen, out of array bounds");
        UNSAFE.copyMemory(getAddressOfObject((Object) bytes) + BASE_OFF_BYTE_ARRAY + srcOff, ptr + offset, len);
    }

    /**
     * Copy the {@code len} bytes from the pointer at offset {@code srcOff} into
     * this buffer at offset {@code offset}.
     */
    public final void setBytes(int offset, long address, int srcOff, int len) {
        if (offset + len >= capacity)
            throw new IllegalArgumentException("offset + len > capacity, out of buffer bounds");
        UNSAFE.copyMemory(address + srcOff, ptr + offset, len);
    }

    public final void setBytes(int offset, byte[] bytes) {
        setBytes(offset, bytes, 0, bytes.length);
    }

    /**
     * The default IO operations buffer size.
     */
    protected static final int IO_BUFFER_SIZE = 1024;

    /**
     * How many times to retry an IO operation.
     */
    protected static final int IO_RETRIES = 3;

    /**
     * Try to read {@code len} amount of bytes from the stream into
     * this buffer at offset {@code offset}.
     */
    public final int setFrom(InputStream stream, int len, int offset) {
        try {
            int remaining = len;
            byte[] buffer = new byte[IO_BUFFER_SIZE];
            long addr = getAddressOfObject(buffer) + BASE_OFF_BYTE_ARRAY;
            int t = IO_RETRIES;
            while (remaining > 0) {
                int r = stream.read(buffer, 0, Math.min(remaining, IO_BUFFER_SIZE));
                if (r == -1) {
                    if (t > 0) {
                        t--;
                        continue;
                    }

                    throw new IllegalStateException("Failed to read " + Math.min(remaining, IO_BUFFER_SIZE) + " more bytes from stream, EOF");
                }

                remaining -= r;

                setBytes(offset, addr, 0, r);
                offset += r;
            }

            return len - remaining;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            return 0;
        }
    }

    /**
     * Try to read {@code len} amount of bytes from the stream into
     * this buffer at the current write offset.
     */
    public final int writeFrom(InputStream stream, int len) {
        try {
            int remaining = len;
            byte[] buffer = new byte[IO_BUFFER_SIZE];
            long addr = getAddressOfObject((Object) buffer) + BASE_OFF_BYTE_ARRAY;
            int t = IO_RETRIES;
            while (remaining > 0) {
                int r = stream.read(buffer, 0, Math.min(remaining, IO_BUFFER_SIZE));
                if (r == -1) {
                    if (t > 0) {
                        t--;
                        continue;
                    }

                    throw new IllegalStateException("Failed to read " + Math.min(remaining, IO_BUFFER_SIZE) + " more bytes from stream, EOF");
                }

                remaining -= r;

                ensureWriteCapacity(r);
                setBytes(writeIndex, addr, 0, r);
                writeIndex += r;
            }

            return len - remaining;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            return 0;
        }
    }

    /**
     * Try to copy {@code len} amount of bytes from this buffer starting at
     * offset {@code offset} into the given output stream.
     */
    public final int getTo(OutputStream stream, int len, int offset) {
        try {
            int remaining = Math.min(capacity - offset, len);
            byte[] buffer = new byte[IO_BUFFER_SIZE];
            long addr = getAddressOfObject((Object) buffer) + BASE_OFF_BYTE_ARRAY;
            while (remaining > 0) {
                int r = Math.min(remaining, IO_BUFFER_SIZE);
                getBytes(offset, addr, 0, r);
                stream.write(buffer, 0, r);

                remaining -= r;
                offset += r;
            }

            return len - remaining;
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
            return 0;
        }
    }

    /**
     * Try to read {@code len} amount of bytes from this buffer starting at
     * the current read offset into the given output stream.
     */
    public final int readTo(OutputStream stream, int len) {
        int read = getTo(stream, len, readIndex);
        readIndex += read;
        return read;
    }

    /* ------------- Helper Functions for complex types ------------- */

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    public final void writeString(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;
        ensureWriteCapacity(len + 5 /* var int */);
        writeVarInt(len);
        setBytes(writeIndex, bytes);
        advWriter(len);
    }

    public final String readString() {
        int len = readVarInt();
        checkReadCap(len);
        byte[] bytes = new byte[len];
        getBytes(readIndex, bytes);
        readIndex += len;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public final UUID readUUID() {
        checkReadCap(16);
        long msb = getLong(readIndex);
        long lsb = getLong(readIndex + 8);
        readIndex += 16;
        return new UUID(msb, lsb);
    }

    public final void writeUUID(UUID uuid) {
        ensureWriteCapacity(16);
        setLong(writeIndex, uuid.getMostSignificantBits());
        setLong(writeIndex, uuid.getLeastSignificantBits());
        advWriter(16);
    }

    public final int readVarInt() {
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

    public final long readVarLong() {
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

    public final void writeVarInt(int value) {
        ensureWriteCapacity(5);
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                writeByte((byte) value);
                return;
            }

            writeByte((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            value >>>= 7;
        }
    }

    public final void writeVarLong(long value) {
        ensureWriteCapacity(10);
        while (true) {
            if ((value & ~((long) SEGMENT_BITS)) == 0) {
                writeByte((byte) value);
                return;
            }

            writeByte((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            value >>>= 7;
        }
    }

    public final byte[] readBytes(byte[] bytes) {
        return readBytes(bytes, 0, bytes.length);
    }

    public final byte[] readBytes(byte[] bytes, int offset, int len) {
        checkReadCap(len);
        getBytes(readIndex, bytes, offset, len);
        readIndex += len;
        return bytes;
    }

    public final byte[] readBytes(int len) {
        byte[] bytes = new byte[len];
        checkReadCap(len);
        getBytes(readIndex, bytes, 0, len);
        readIndex += len;
        return bytes;
    }

    public final void writeBytes(byte[] bytes) {
        ensureWriteCapacity(bytes.length);
        setBytes(writeIndex, bytes);
        advWriter(bytes.length);
    }

    public final void writeBytes(byte[] bytes, int off, int len) {
        ensureWriteCapacity(len);
        setBytes(writeIndex, bytes, off, len);
        advWriter(len);
    }

    /**
     * Create a new input stream which reads from the current read offset
     * to the write offset.
     *
     * @return The input stream.
     */
    public InputStream readingInputStream() {
        return new BufReadingInputStream(this);
    }

    /**
     * Create a new input stream which writes from the current write offset.
     *
     * @return The input stream.
     */
    public OutputStream writingOutputStream() {
        return new BufWritingOutputStream(this);
    }

    /**
     * Create a new {@link ByteBuffer} which serves as a direct reference to this
     * buffers current memory.
     *
     * WARNING: If this buffer changes pointer (for example due to reallocation),
     * the NIO reference will point to invalid memory.
     *
     * @return The NIO reference buffer
     */
    public ByteBuffer nioReference() {
        return nioReference(0, capacity);
    }

    /**
     * Create a new {@link ByteBuffer} which serves as a direct reference to a slice
     * of this buffers current memory.
     *
     * WARNING: If this buffer changes pointer (for example due to reallocation),
     * the NIO reference will point to invalid memory.
     *
     * @param offset The offset into this buffer to start the slice.
     * @param length The length of the slice.
     * @return The NIO reference buffer
     */
    public ByteBuffer nioReference(int offset, int length) {
        try {
            return (ByteBuffer) constructorDirectNIOBuffer.invoke(this.ptr + offset, length);
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            throw new AssertionError();
        }
    }

    static final long BASE_OFF_BYTE_ARRAY = UNSAFE.arrayBaseOffset(byte[].class);
    static final long BASE_OFF_OBJ_ARRAY = UNSAFE.arrayBaseOffset(Object[].class);

    // THE VARARGS ARRAY IS SUPPOSED TO BE OF LENGTH 1
    // CONTAINING ONLY THE OBJECT YOU WANT THE ADDRESS OF
    public static long getAddressOfObject(Object... obj) {
        return UNSAFE.getLong(obj, BASE_OFF_OBJ_ARRAY);
    }

    @Override
    public String toString() {
        return "ByteBuf(" +
                "readIndex=" + readIndex +
                ", writeIndex=" + writeIndex +
                ", capacity=" + capacity +
                ", pointer=" + Long.toHexString(pointer()) +
                ')';
    }

    /** Reads from a buffer. */
    @RequiredArgsConstructor
    static class BufReadingInputStream extends InputStream {

        final UnsafeByteBuf buf;

        @Override
        public int read() throws IOException {
            return buf.readIndex < buf.writeIndex ? buf.readByte() : -1;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            len = Math.min(len, buf.remainingWritten());
            buf.readBytes(b, off, len);
            return len;
        }

        @Override
        public int readNBytes(byte[] b, int off, int len) throws IOException {
            if (buf.remainingWritten() < len)
                throw new IllegalArgumentException("not enough bytes lol");
            buf.readBytes(b, off, len);
            return len;
        }

        @Override
        public byte[] readNBytes(int len) throws IOException {
            byte[] buffer = new byte[len];
            readNBytes(buffer, 0, len);
            return buffer;
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            byte[] buffer = new byte[buf.remainingWritten()];
            return buf.readBytes(buffer);
        }

    }

    /** Writes to a buffer. */
    @RequiredArgsConstructor
    static class BufWritingOutputStream extends OutputStream {

        final UnsafeByteBuf buf;

        @Override
        public void write(int b) throws IOException {
            buf.writeByte((byte) b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            buf.writeBytes(b, off, len);
        }

    }

}
