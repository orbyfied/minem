package com.github.orbyfied.minem.registry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@SuppressWarnings({ "unchecked", "rawtypes" })
final class DualMappedRegistry<P, V> implements Registry<P, V>, Registry.Dimension<P, V> {

    // The primary key set
    final KeySet<P> primaryKeySet;
    // The value class
    final Class<V> valueClass;

    final HashMap<P, V> keyToValueMap = new HashMap<>();
    final HashMap<V, P> valueToKeyMap = new HashMap<>();

    final HashMap<KeySet, Dimension> dimensions = new HashMap<>();

    @Override
    public KeySet<P> primaryKeySet() {
        return primaryKeySet;
    }

    @Override
    public <K> Optional<V> getOptional(KeySet<K> keySet, K key) {
        return keySet == primaryKeySet ? Optional.ofNullable(keyToValueMap.get(key)) : (dimensions.containsKey(keySet) ? dimensions.get(keySet).getOptional(key) : Optional.empty());
    }

    @Override
    public <K> V getOrNull(KeySet<K> keySet, K key) {
        return keySet == primaryKeySet ? keyToValueMap.get(key) : (dimensions.containsKey(keySet) ? (V) dimensions.get(keySet).getOrNull(key) : null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> K getKey(KeySet<K> keySet, V value) {
        return keySet == primaryKeySet ? (K) valueToKeyMap.get(value) : (dimensions.containsKey(keySet) ? (K) dimensions.get(keySet).getKey(value) : null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P1> Registry<P1, V> registry() {
        return (Registry<P1, V>) this;
    }

    @Override
    public Optional<V> getOptional(P key) {
        return Optional.ofNullable(keyToValueMap.get(key));
    }

    @Override
    public V getOrNull(P key) {
        return keyToValueMap.get(key);
    }

    @Override
    public P getKey(V value) {
        return valueToKeyMap.get(value);
    }

    @Override
    public Dimension<P, V> add(P key, V value) {
        register(key, value);
        return this;
    }

    @Override
    public Registry<P, V> register(P primaryKey, V value) {
        keyToValueMap.put(primaryKey, value);
        valueToKeyMap.put(value, primaryKey);
        return this;
    }

    @Override
    public <K> Dimension<K, V> dimension(KeySet<K> keySet) {
        return keySet == primaryKeySet ? (Dimension<K, V>) this : dimensions.computeIfAbsent(keySet, __ -> new DualMappedDimension(this));
    }

    @Override
    public Collection<V> values() {
        return keyToValueMap.values();
    }

    @Override
    public int size() {
        return keyToValueMap.size();
    }

    @Override
    public void remove(V value) {
        P key = valueToKeyMap.remove(value);
        keyToValueMap.remove(key);

        // remove from all dimensions
        for (Dimension dimension : dimensions.values()) {
            dimension.remove(value);
        }
    }

    @RequiredArgsConstructor
    @Getter
    public static class DualMappedDimension<K, V> implements Dimension<K, V> {

        final Registry registry;

        final HashMap<K, V> keyToValueMap = new HashMap<>();
        final HashMap<V, K> valueToKeyMap = new HashMap<>();

        @Override
        public <P> Registry<P, V> registry() {
            return registry;
        }

        @Override
        public Optional<V> getOptional(K key) {
            return Optional.ofNullable(keyToValueMap.get(key));
        }

        @Override
        public V getOrNull(K key) {
            return keyToValueMap.get(key);
        }

        @Override
        public K getKey(V value) {
            return valueToKeyMap.get(value);
        }

        @Override
        public Dimension<K, V> add(K key, V value) {
            keyToValueMap.put(key, value);
            valueToKeyMap.put(value, key);
            return this;
        }

        @Override
        public void remove(V value) {
            K key = valueToKeyMap.remove(value);
            keyToValueMap.remove(key);
        }

    }

}
