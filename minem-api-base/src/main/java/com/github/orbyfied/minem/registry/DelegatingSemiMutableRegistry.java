package com.github.orbyfied.minem.registry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

/**
 * A semi-mutable registry which delegates un-overridden keys to the delegate registry.
 *
 * It won't allow edits to the primary key, but it will permit additions and overwrites of
 * dimensions.
 */
@RequiredArgsConstructor
@Getter
@SuppressWarnings({ "unchecked", "rawtypes" })
final class DelegatingSemiMutableRegistry<P, V> implements Registry<P, V>, Registry.Dimension<P, V> {

    final Registry<P, V> registry;

    // The overridden dimensions
    final HashMap<KeySet, DualMappedRegistry.DualMappedDimension> dimensions = new HashMap<>();

    @Override
    public KeySet<P> primaryKeySet() {
        return registry.primaryKeySet();
    }

    @Override
    public <K> Optional<V> getOptional(KeySet<K> keySet, K key) {
        return dimensions.containsKey(keySet) ? (Optional<V>) dimensions.get(keySet).getOptional(key).orElse(registry.getOptional(keySet, key)) : registry.getOptional(keySet, key);
    }

    @Override
    public <K> V getOrNull(KeySet<K> keySet, K key) {
        V val;
        Dimension dimension = dimensions.get(keySet);
        if (dimension != null) {
            if ((val = (V) dimension.getOrNull(key)) != null) {
                return val;
            }
        }

        return registry.getOrNull(keySet, key);
    }

    @Override
    public <K> K getKey(KeySet<K> keySet, V value) {
        K key;
        Dimension dimension = dimensions.get(keySet);
        if (dimension != null) {
            if ((key = (K) dimension.getKey(value)) != null) {
                return key;
            }
        }

        return registry.getKey(keySet, value);
    }

    @Override
    public <P1> Registry<P1, V> registry() {
        return (Registry<P1, V>) this;
    }

    @Override
    public Optional<V> getOptional(P key) {
        return registry.getOptional(key);
    }

    @Override
    public V getOrNull(P key) {
        return registry.getOrNull(key);
    }

    @Override
    public P getKey(V value) {
        return (P) registry.getKey(value);
    }

    @Override
    public Dimension<P, V> add(P key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Registry<P, V> register(P primaryKey, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K> Dimension<K, V> dimension(KeySet<K> keySet) {
        return dimensions.computeIfAbsent(keySet, __ -> new DualMappedRegistry.DualMappedDimension(this));
    }

    @Override
    public Collection<V> values() {
        return registry.values();
    }

    @Override
    public int size() {
        return registry.size();
    }

    @Override
    public void remove(V value) {
        throw new UnsupportedOperationException();
    }



}
