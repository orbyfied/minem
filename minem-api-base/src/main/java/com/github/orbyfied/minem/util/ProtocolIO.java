package com.github.orbyfied.minem.util;

import java.io.IOException;
import java.io.OutputStream;

public class ProtocolIO {

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    /** Writes the given value as a VarInt directly to the given stream. */
    public static void writeVarIntToStream(OutputStream stream, int value) throws IOException {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                stream.write((byte) value);
                return;
            }

            stream.write((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            value >>>= 7;
        }
    }

}
