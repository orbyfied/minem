package com.orbyfied.minem.model.inventory;

import lombok.Getter;

import java.util.Map;
import java.util.Optional;

/**
 * An item type.
 */
@Getter
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ItemType {

    public static ItemType unresolved(String identifier, int numericalID) {
        ItemType type = new ItemType(identifier);
        type.resolved = false;
        type.unresolvedNumericalID = numericalID;
        return type;
    }

    public ItemType(String identifier) {
        this.identifier = identifier;
    }

    /**
     * The string identifier of this item type.
     */
    String identifier;

    /**
     * The numerical ID read from the network if this type is unresolved.
     */
    protected int unresolvedNumericalID;

    /**
     * Whether the item type could be resolved from a registry.
     */
    protected boolean resolved = true;

    /**
     * The component map, lazily created when needed.
     */
    protected Map<Class, Object> components;

    public <T> T getComponent(Class<T> tClass) {
        return components != null ? (T) components.get(tClass) : null;
    }

    public <T> Optional<T> getComponentOptionally(Class<T> tClass) {
        return Optional.ofNullable(getComponent(tClass));
    }

    public ItemType withComponent(Object obj) {
        Class<?> klass = obj.getClass();
        for (; klass != Object.class; klass = klass.getSuperclass()) {
            components.put(klass, obj);
        }

        return this;
    }

}
