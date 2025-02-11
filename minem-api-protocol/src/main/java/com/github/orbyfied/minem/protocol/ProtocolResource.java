package com.github.orbyfied.minem.protocol;

/**
 * The key to a protocol resource.
 */
public record ProtocolResource<T>(Class<? super T> klass, String name) {

}
