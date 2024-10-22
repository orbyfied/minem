package com.github.orbyfied.minem.data;

import com.github.orbyfied.minem.buffer.UnsafeByteBuf;
import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.tags.collection.CompoundTag;
import slatepowered.veru.misc.Throwables;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Utilities for working with NBT sent over the network.
 *
 * Uses https://github.com/BitBuf/nbt.
 */
public final class ProtocolNBT {

    static final Nbt NBT = new Nbt();

    public static CompoundTag read(UnsafeByteBuf buf) {
        try {
            return NBT.fromStream(new DataInputStream(buf.readingInputStream()));
        } catch (IOException e) {
            Throwables.sneakyThrow(e);
            return null;
        }
    }

    public static void write(UnsafeByteBuf buf, CompoundTag tag) {
        try {
            NBT.toStream(tag, new DataOutputStream(buf.writingOutputStream()));
        } catch (IOException e) {
            Throwables.sneakyThrow(e);
        }
    }

}
