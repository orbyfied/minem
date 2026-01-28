package com.orbyfied.minem.hypixel.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Nicer map access.
 */
@RequiredArgsConstructor
@Getter
@SuppressWarnings("unchecked")
public class MapAccess<K, V> {

    final Map<K, V> map;
    final MapAccess<K, V> def;

    public static <K, V> MapAccess<K, V> of(Map<K, V> map) {
        return new MapAccess<>(map, null);
    }

    public static <K, V> MapAccess<K, V> ofDefaulted(Map<K, V> map, MapAccess<K, V> def) {
        return new MapAccess<>(map, def);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public V get(K key) {
        V val = map.get(key);
        if (val == null && def != null) {
            val = def.get(key);
        }

        return val;
    }

    public <T> T getAs(K key) {
        return (T) get(key);
    }

    public <T> T getAs(K key, Class<T> tClass) {
        return (T) get(key);
    }

    public MapAccess<K, V> set(K key, V val) {
        map.put(key, val);
        return this;
    }

    public MapAccess<K, V> put(K key, V val) {
        map.put(key, val);
        return this;
    }

    public MapAccess<K, V> unset(K key) {
        map.remove(key);
        return this;
    }

    public MapAccess<K, V> remove(K key) {
        map.remove(key);
        return this;
    }

    public V getOr(K key, V def) {
        V val = get(key);
        return val != null ? val : def;
    }

    public <T> T getAsOr(K key, T def) {
        V val = get(key);
        return val != null ? (T) val : def;
    }

    public <T> T getAsOr(K key, Class<T> tClass, T def) {
        return getAsOr(key, def);
    }

    public V getOr(K key, Supplier<V> def) {
        V val = get(key);
        return val != null ? val : def.get();
    }

    public <T> T getAsOr(K key, Supplier<T> def) {
        V val = get(key);
        return val != null ? (T) val : def.get();
    }

    public <T> T getAsOr(K key, Class<T> tClass, Supplier<T> def) {
        return getAsOr(key, def);
    }

    public MapAccess<String, Object> properties(K key) {
        return map(key);
    }

    public <K2, V2> MapAccess<K2, V2> map(K key) {
        return new MapAccess<>((Map<K2, V2>) map.computeIfAbsent(key, __ -> (V) new HashMap<K2, V2>()), null);
    }

}
