package com.github.orbyfied.minem.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a named secret of type T.
 */
@RequiredArgsConstructor
@Getter
public class Secret<T> {

    final String name;
    volatile T value;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name: " + name + ", valid: " + isValid() + ")";
    }

    /**
     * Check if this secret is valid.
     */
    public boolean isValid() {
        return value != null;
    }

    /**
     * Get the value of this secret.
     */
    public synchronized T getValue() {
        return value;
    }

    /**
     * Set the value of this secret.
     */
    public synchronized Secret<T> value(T value) {
        this.value = value;
        return this;
    }

}
