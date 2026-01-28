package com.orbyfied.minem.registry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The key for a key set of a registry.
 *
 * @param <K> The key type.
 */
@RequiredArgsConstructor
@Getter
public class KeySet<K> {

    final Class<K> runtimeClass;

}
