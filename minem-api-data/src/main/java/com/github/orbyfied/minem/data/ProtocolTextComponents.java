package com.github.orbyfied.minem.data;

import com.github.orbyfied.minem.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * Utilities for working with text components sent over the network.
 */
public final class ProtocolTextComponents {

    public static Component readJSONTextComponent(ByteBuf buf) {
        String json = buf.readString();
        return GsonComponentSerializer.gson().deserializeOrNull(json);
    }

}
