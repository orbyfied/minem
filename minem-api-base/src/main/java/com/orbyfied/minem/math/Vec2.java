package com.orbyfied.minem.math;

import java.util.Objects;

/**
 * A two component vector of type {@code T}.
 *
 * @param <T> The element type.
 */
public abstract class Vec2<T extends Number> {

    public abstract T getGenericX();
    public abstract T getGenericY();

    @Override
    public int hashCode() {
        return Objects.hash(getGenericX(), getGenericY());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Vec2<?>)) return false;
        Vec2<?> vec3 = (Vec2<?>) obj;
        return
                vec3.getGenericX().equals(getGenericX()) &&
                        vec3.getGenericY().equals(getGenericY());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getGenericX() + ", " + getGenericY() + ")";
    }

}
