package com.github.orbyfied.minem.data;

import lombok.Getter;
import lombok.Setter;

/**
 * An item stack with a type, an amount, a damage value and NBT.
 */
@Getter
@Setter
public class ItemStack {

    /** An empty item stack. */
    public static final ItemStack EMPTY = null;

    public ItemStack() {

    }

    public ItemStack(ItemType type) {
        this.type = type;
    }

    public ItemStack(ItemType type, int count) {
        this.type = type;
        this.count = count;
    }

    /**
     * The type of item this item stack holds.
     */
    ItemType type = ItemTypes.AIR;

    /**
     * The amount of items in this stack.
     */
    int count = 1;

}
