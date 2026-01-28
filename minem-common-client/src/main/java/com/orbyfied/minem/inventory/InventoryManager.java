package com.orbyfied.minem.inventory;

import com.orbyfied.minem.ClientComponent;
import com.orbyfied.minem.model.inventory.Inventory;

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
