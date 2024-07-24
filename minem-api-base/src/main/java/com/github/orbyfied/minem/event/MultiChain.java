package com.github.orbyfied.minem.event;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a keyed store of {@link Chain} by some value type.
 *
 * @param <K> The key type.
 * @param <F> The handler type.
 */
@RequiredArgsConstructor
public class MultiChain<K, F> {

    final ChainFactory<K, F> factory;
    final Map<K, Chain<F>> map = new HashMap<>();

    public Chain<F> by(K key) {
        return map.computeIfAbsent(key, factory::create);
    }

    public Chain<F> orNull(K key) {
        return map.get(key);
    }

}
