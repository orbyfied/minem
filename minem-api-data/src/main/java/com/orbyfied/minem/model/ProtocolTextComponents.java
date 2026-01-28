package com.orbyfied.minem.model;

import com.orbyfied.minem.buffer.UnsafeByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for working with text components sent over the network.
 *
 * Uses https://github.com/KyoriPowered/adventure.
 */
public final class ProtocolTextComponents {

    public static Component readJSONTextComponent(UnsafeByteBuf buf) {
        String json = buf.readString();
        return GsonComponentSerializer.gson().deserializeOrNull(json);
    }

    public static Component fixLegacyFormattingInTree(Component component) {
        Component oldComponent = component;

        // fix this component
        String str;
        if (component instanceof TextComponent && (str = ((TextComponent)component).content()).indexOf(LegacyComponentSerializer.SECTION_CHAR) != -1) {
            component = LegacyComponentSerializer.legacySection().deserialize(str);

            List<Component> children = new ArrayList<>(component.children());
            for (Component child : oldComponent.children()) {
                if (!(child instanceof TextComponent)) {
                    children.add(fixLegacyFormattingInTree(child));
                }
            }

            return component.children(children);
        }

        // just sanitize children
        List<Component> oldChildren = oldComponent.children();
        List<Component> children = new ArrayList<>();
        for (Component oldChild : oldChildren) {
            children.add(fixLegacyFormattingInTree(oldChild));
        }

        return component.children(children);
    }

}
