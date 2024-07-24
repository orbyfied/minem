package com.github.orbyfied.minem.event;

@FunctionalInterface
public interface ChainFactory<K, F> {

    /**
     * Create a new event/interface chain.
     */
    Chain<F> create(K key);

}
