package com.github.orbyfied.minem.component;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.data.Inventory;

/**
 * Tracks, manages and handles inventories on the client.
 */
public class InventoryManager extends ClientComponent {

    /**
     * The inventory of the local player.
     */
    Inventory playerInventory;

    /**
     * The top window of the open inventory view, if any.
     */
    Inventory topOpenInventory;

}
