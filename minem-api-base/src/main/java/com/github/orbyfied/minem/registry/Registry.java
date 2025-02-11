package com.github.orbyfied.minem.registry;

import java.util.Collection;
import java.util.Optional;

/**
 * A registry mapping between a number of key types and the value objects.
 *
 * @param <P> The primary key.
 */
public interface Registry<P, V> {

    static <P, V> Registry<P, V> dualMapped(KeySet<P> primaryKeySet, Class<V> vClass) {
        return new DualMappedRegistry<>(primaryKeySet, vClass);
    }

    static <P, V> Registry<P, V> semiDelegating(Registry<P, V> registry) {
        return new DelegatingSemiMutableRegistry<>(registry);
    }

    /**
     * The primary key set.
     */
    KeySet<P> primaryKeySet();

    /**
     * Optionally get a value by the key set and key.
     */
    <K> Optional<V> getOptional(KeySet<K> keySet, K key);

    /**
     * Get a value by the key set and key or null if absent.
     */
    <K> V getOrNull(KeySet<K> keySet, K key);

    /**
     * Get the key for the given value in the given key set.
     */
    <K> K getKey(KeySet<K> keySet, V value);

    /**
     * Optionally get a value by the primary key.
     */
    Optional<V> getOptional(P key);

    /**
     * Get a value by the primary key or null if absent.
     */
    V getOrNull(P key);

    /**
     * Get the key for the given value in this dimension.
     */
    P getKey(V value);

    /**
     * Register the given object to this registry with the given primary key.
     *
     * @param primaryKey The primary key.
     * @param value The value.
     * @return The registry.
     */
    Registry<P, V> register(P primaryKey, V value);

    /**
     * Get the dimension access for the given key set.
     */
    <K> Dimension<K, V> dimension(KeySet<K> keySet);

    /**
     * All values registered to this registry.
     */
    Collection<V> values();

    /**
     * Get the amount of items in this registry.
     *
     * @return The size.
     */
    int size();

    /**
     * Remove the given value from the registry.
     */
    void remove(V value);

    /**
     * Represents the map for a specific key set.
     */
    interface Dimension<K, V> {

        /**
         * The registry which owns this dimension.
         */
        <P> Registry<P, V> registry();

        /**
         * Optionally get a value by the primary key.
         */
        Optional<V> getOptional(K key);

        /**
         * Get a value by the primary key or null if absent.
         */
        V getOrNull(K key);

        /**
         * Get the key for the given value in this dimension.
         */
        K getKey(V value);

        /**
         * Register the given object to this dimension with the given key.
         *
         * @param key The primary key.
         * @param value The value.
         * @return The registry.
         */
        Dimension<K, V> add(K key, V value);

        /**
         * Remove the given value from the dimension.
         */
        void remove(V value);

    }

}
