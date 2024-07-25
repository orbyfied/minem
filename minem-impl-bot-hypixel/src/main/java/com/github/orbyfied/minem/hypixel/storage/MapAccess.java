package com.github.orbyfied.minem.hypixel.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Nicer map access.
 */
@SuppressWarnings("unchecked")
public record MapAccess<K, V>(Map<K, V> map) {

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public V get(K key) {
        return map.get(key);
    }

    public <T> T getAs(K key) {
        return (T) map.get(key);
    }

    public <T> T getAs(K key, Class<T> tClass) {
        return (T) map.get(key);
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
        return map.getOrDefault(key, def);
    }

    public <T> T getAsOr(K key, T def) {
        return (T) map.getOrDefault(key, (V) def);
    }

    public <T> T getAsOr(K key, Class<T> tClass, T def) {
        return (T) map.getOrDefault(key, (V) def);
    }

    public V getOr(K key, Supplier<V> def) {
        return map.computeIfAbsent(key, __ -> def.get());
    }

    public <T> T getAsOr(K key, Supplier<T> def) {
        return (T) map.computeIfAbsent(key, __ -> (V) def.get());
    }

    public <T> T getAsOr(K key, Class<T> tClass, Supplier<T> def) {
        return (T) map.computeIfAbsent(key, __ -> (V) def.get());
    }

    public MapAccess<String, Object> properties(K key) {
        return map(key);
    }

    public <K2, V2> MapAccess<K2, V2> map(K key) {
        return new MapAccess<>((Map<K2, V2>) map.computeIfAbsent(key, __ -> (V) new HashMap<K2, V2>()));
    }

}
