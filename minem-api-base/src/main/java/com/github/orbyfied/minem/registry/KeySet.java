package com.github.orbyfied.minem.registry;

/**
 * The key for a key set of a registry.
 *
 * @param <K> The key type.
 */
public record KeySet<K>(Class<K> runtimeClass) {

}
