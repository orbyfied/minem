package com.orbyfied.minem.event;

import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Represents a keyed store of {@link Chain} by some value type.
 *
 * @param <K> The key type.
 * @param <F> The handler type.
 */
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class MultiChain<K, F> {

    EventKeyMapper<K> keyMapper = k -> (Collection<K>) List.of(k);
    final ChainFactory<K, F> factory;
    final Map<K, Chain<F>> map = new HashMap<>();

    public MultiChain<K, F> keyMapper(EventKeyMapper<K> keyMapper) {
        this.keyMapper = keyMapper;
        return this;
    }

    /**
     * Get or create the single chain corresponding directly to the given key.
     */
    public Chain<F> get(K key) {
        return map.computeIfAbsent(key, factory::create);
    }

    /**
     * Get or create a (possibly compound) chain corresponding to the given mappable key.
     */
    public ChainAccess<F> by(Object key) {
        Collection<K> keys = keyMapper.mapKeys(key);
        if (keys.size() == 1) {
            return get(keys.iterator().next());
        }

        List<Chain<F>> chains = new ArrayList<>(keys.size());
        for (K k : keys) {
            chains.add(get(k));
        }

        return ChainAccess.of(chains);
    }

    /**
     * Get the single chain corresponding directly to the given key.
     */
    public Chain<F> orNull(K key) {
        return map.get(key);
    }

}
