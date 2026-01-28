package com.orbyfied.minem.protocol;

import com.orbyfied.minem.model.inventory.ItemType;
import com.orbyfied.minem.registry.Registry;

/**
 * Protocol resources used by packet serialization.
 */
public final class ProtocolResources {

    public static final ProtocolResource<Registry<String, ItemType>> ITEM_TYPE_REGISTRY
            = new ProtocolResource<>(Registry.class, "ITEM_TYPE_REGISTRY");

}
