package com.github.orbyfied.minem.io;

import com.github.orbyfied.minem.math.Vec3i;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProtocolIO {

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    /** Counts the encoded length of a var int with the given value. */
    public static int lengthVarInt(int value) {
        int bytes = 0;
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                bytes++;
                return bytes;
            }

            bytes++;

            value >>>= 7;
        }
    }

    /** Flexible implementation of readVarInt() for any byte stream-like source */
    public static int readVarInt(IOByteSupplier byteSupplier) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = (byte) byteSupplier.getByte();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    /** Flexible implementation of writeVarInt() for any byte stream-like destination */
    public static int writeVarInt(IOByteConsumer consumer, int value) throws IOException {
        int bytes = 0;
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                consumer.acceptByte(value);
                bytes++;
                return bytes;
            }

            consumer.acceptByte((value & SEGMENT_BITS) | CONTINUE_BIT);

            bytes++;
            value >>>= 7;
        }
    }

    /** Writes the given value as a VarInt directly to the given byte array. */
    public static int writeVarIntToBytes(byte[] bytes, int value) {
        int i = 0;
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                bytes[i++] = ((byte) value);
                return i;
            }

            bytes[i++] = ((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            value >>>= 7;
        }
    }

    /** Read a VarInt directly from the given byte array. */
    public static int readVarIntFromBytes(byte[] bytes, int offset) {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = bytes[offset++];
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    public static int writeVarIntToStream(OutputStream stream, int value) throws IOException {
        return writeVarInt(stream::write, value);
    }

    public static int readVarIntFromStream(InputStream stream) throws IOException {
        return readVarInt(stream::read);
    }

    /** Pack the given position into 64 bits. */
    public static long packPosition64(int x, int y, int z) {
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFF);
    }

    public static long packPosition64(Vec3i vec) {
        return packPosition64(vec.x(), vec.y(), vec.z());
    }

    public static void unpackPosition64(long val, Vec3i vec) {
        vec.x = (int) (val >> 38);
        vec.y = (int) (val << 52 >> 52);
        vec.z = (int) (val << 26 >> 38);
    }

    public static Vec3i unpackPosition64(long val) {
        Vec3i vec = new Vec3i();
        unpackPosition64(val, vec);
        return vec;
    }

}