package com.orbyfied.minem.protocol;

import lombok.RequiredArgsConstructor;

/**
 * The key to a protocol resource.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ProtocolResource<T> {

    public ProtocolResource(Class klass, String name) {
        this.klass = klass;
        this.name = name;
    }

    final Class<? extends T> klass;
    final String name;

    public Class<? extends T> klass() {
        return klass;
    }

    public String name() {
        return name;
    }

}
