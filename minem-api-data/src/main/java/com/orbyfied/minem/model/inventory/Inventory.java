package com.orbyfied.minem.model.inventory;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class Inventory {

    // The dimensions
    protected final int width;
    protected final int height;

    /**
     * The main contents of this inventory.
     */
    protected final List<ItemStack> items = new ArrayList<>();

    protected Inventory(int width, int height) {
        this.width = width;
        this.height = height;
        for (int i = 0; i < width * height; i++) {
            items.add(ItemStack.EMPTY);
        }
    }

    public ItemStack getItem(int id) {
        ItemStack stack = items.get(id);
        if (stack == null) {
            stack = ItemStack.EMPTY;
        }

        return stack;
    }

    public void setItem(int id, ItemStack stack) {
        items.set(id, stack);
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getSize() {
        return width * height;
    }

}
