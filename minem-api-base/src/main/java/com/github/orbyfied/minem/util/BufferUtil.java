package com.github.orbyfied.minem.util;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import lombok.Builder;
import slatepowered.veru.misc.ANSI;

import java.io.DataInputStream;
import java.util.function.Consumer;

public class BufferUtil {

    public enum TextDumpMode {
        DISABLED, // No text dump
        INLINE,   // After each word the ASCII translation
        SIDE,     // Inline, separated on the side
        BELOW,    // Completely separate dump
    }

    @Builder(toBuilder = true)
    public static class HexDumpOptions {
        @Builder.Default
        boolean header = true; // whether to show a buffer header

        @Builder.Default
        boolean showOffsets = true; // include addresses/offset into the dump

        @Builder.Default
        int wordWrap = 8; // how many words per line, 0 to disable line wrap

        @Builder.Default
        int wordSize = 2; // the size of one word in nibbles

        @Builder.Default
        TextDumpMode textDumpMode = TextDumpMode.SIDE; // whether to display ascii translation

        @Builder.Default
        boolean formatted = true; // whether to add ascii formatting

        @Builder.Default
        Consumer<StringBuilder> lineStarter = null; // allows configuration of line prefixes
    }

    static final HexDumpOptions DEFAULT_OPTIONS = HexDumpOptions.builder()
            .build();

    /**
     * Write a hex dump of the specified slice of the given buffer to the given
     * string builder.
     *
     * @param buf The input buffer.
     * @param offset The slice start offset.
     * @param length The length of the slice in bytes.
     * @param options Additional options.
     */
    public static void hexDump(StringBuilder b, UnsafeByteBuf buf, int offset, int length,
                               HexDumpOptions options) {
        // verify offsets
        if (offset < 0) throw new IllegalArgumentException("Offset can not be below zero");
        if (offset >= buf.capacity()) throw new IllegalArgumentException("Offset out of buffer capacity");
        if (offset + length >= buf.capacity()) throw new IllegalArgumentException("End offset out of buffer capacity");
        int currentOffset = offset;
        int endOffset = offset + length;
        int maxOffsetLengthDigits = Integer.toHexString(endOffset).length();

        if (options == null) {
            options = DEFAULT_OPTIONS;
        }

        // extract options
        boolean showOffsets = options.showOffsets;
        int wordWrap = options.wordWrap;
        int wordSize4 = options.wordSize;
        TextDumpMode textDumpMode = options.textDumpMode;
        boolean formatted = options.formatted;
        Consumer<StringBuilder> lineStarter = options.lineStarter;
        int wordSizeBytes = wordSize4 / 2;

        // verify options
        if (wordSize4 < 2) throw new IllegalArgumentException("Word size can not be under 2");

        // check for header
        if (options.header) {
            b.append(formatted ? ANSI.PURPLE : "").append(buf.getClass().getSimpleName())
                    .append(formatted ? ANSI.WHITE : "").append(" capacity: ")
                    .append(formatted ? ANSI.DARK_YELLOW : "").append(buf.capacity())
                    .append(formatted ? ANSI.WHITE : "").append(" written: ")
                    .append(formatted ? ANSI.DARK_YELLOW : "").append(buf.writeIndex())
                    .append(formatted ? ANSI.WHITE : "").append(" read: ")
                    .append(formatted ? ANSI.DARK_YELLOW : "").append(buf.readIndex())
                    .append("\n");
        }

        // build hex dump
        int lineNumber = 0;
        int wordNumber = 0; // on line
        boolean isNewLine = true;
        int totalWords = (endOffset - currentOffset) / wordSizeBytes;
        int wordsPerLine = wordWrap != 0 ? wordWrap : totalWords;
        byte[] lineBuffer = new byte[wordsPerLine * wordSizeBytes];
        for (; currentOffset < endOffset; currentOffset += wordSizeBytes) {
            // handle line shit
            if (isNewLine) {
                if (lineNumber != 0) {
                    b.append("\n");
                }

                isNewLine = false;
                lineNumber++;
                wordNumber = 0;

                if (lineStarter != null) {
                    lineStarter.accept(b);
                }

                // display offsets
                if (showOffsets) {
                    String offsetStr = (formatted ? ANSI.GRAY : "") + "0x" + (formatted ? ANSI.RED : "") +
                            padEnd(Integer.toHexString(currentOffset).toUpperCase(), maxOffsetLengthDigits, ' ') +
                            (formatted ? ANSI.WHITE : "") + " | " + (formatted ? ANSI.RESET : "");
                    b.append(offsetStr);
                }
            }

            // get and write word
            int s = wordNumber * wordSizeBytes;
            buf.getBytes(currentOffset, lineBuffer, s, wordSizeBytes);
            b.append(formatted ? ANSI.CYAN : "");
            for (int i = 0; i < wordSizeBytes; i++) {
                b.append(padStart(Integer.toUnsignedString((int)(lineBuffer[s + i]) & 0xFF, 16).toUpperCase(), wordSize4, '0'));
            }

            b.append(formatted ? ANSI.RESET : "").append(" ");

            // check for inline ascii
            if (textDumpMode == TextDumpMode.INLINE) {
                b.append(formatted ? ANSI.GREEN : "").append("(");
                for (int i = 0; i < wordSizeBytes; i++) {
                    String ansiCode = formatted ? ANSI.GREEN : "";
                    char c = (char) (lineBuffer[s + i]);
                    if (c <= 31 || c >= 127) {
                        c = /* special/invalid char */ '.';
                        ansiCode = formatted ? ANSI.DARK_GREEN : "";
                    }

                    b.append(ansiCode).append(c);
                }

                b.append(formatted ? ANSI.GREEN : "").append(") ").append(formatted ? ANSI.RESET : "");
            }

            wordNumber++;

            // check for word wrap or last word
            if (wordWrap != 0 && wordNumber >= wordWrap || (currentOffset + wordSizeBytes) >= endOffset) {
                isNewLine = true;
            }

            // write side ascii dump
            if (isNewLine && textDumpMode == TextDumpMode.SIDE) {
                // pad space
                if (wordNumber < wordsPerLine) {
                    b.append((" ".repeat(wordSize4) + " ").repeat(wordsPerLine - wordNumber));
                }

                // write dump
                for (int i = 0; i < Math.min(lineBuffer.length, wordNumber * wordSizeBytes); i++) {
                    String ansiCode = formatted ? ANSI.GREEN : "";
                    char c = (char) (lineBuffer[i]);
                    if (c <= 31 || c >= 127) {
                        c = /* special/invalid char */ '.';
                        ansiCode = formatted ? ANSI.BLUE : "";
                    }

                    b.append(ansiCode).append(c);
                }
            }
        }

        // todo: below ascii shit
    }

    public static String hexDump(UnsafeByteBuf buf, int offset, int length, HexDumpOptions options) {
        StringBuilder b = new StringBuilder();
        hexDump(b, buf, offset, length, options);
        return b.toString();
    }

    public static String hexDump(UnsafeByteBuf buf, HexDumpOptions options) {
        StringBuilder b = new StringBuilder();
        hexDump(b, buf, 0, buf.writeIndex(), options);
        return b.toString();
    }

    public static String hexDump(UnsafeByteBuf buf) {
        StringBuilder b = new StringBuilder();
        hexDump(b, buf, 0, buf.writeIndex(), null);
        return b.toString();
    }

    private static String padStart(String str, int targetLength, char c) {
        if (str.length() < targetLength) {
            str = String.valueOf(c).repeat(targetLength - str.length()) + str;
        }

        return str;
    }

    private static String padEnd(String str, int targetLength, char c) {
        if (str.length() < targetLength) {
            str = str + String.valueOf(c).repeat(targetLength - str.length());
        }

        return str;
    }

}
