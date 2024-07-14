package com.github.orbyfied.minem.util;

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

    /** Writes the given value as a VarInt directly to the given stream. */
    public static int writeVarIntToStream(OutputStream stream, int value) throws IOException {
        int bytes = 0;
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                stream.write((byte) value);
                bytes++;
                return bytes;
            }

            stream.write((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));
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

    /** Read a VarInt directly from the given stream. */
    public static int readVarIntFromStream(InputStream stream) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = (byte) stream.read();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
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

}
