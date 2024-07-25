package com.github.orbyfied.minem.math;

import java.util.Objects;

/**
 * A three component vector of type {@code T}.
 *
 * @param <T> The element type.
 */
public abstract class Vec3<T extends Number> {

    public abstract T getGenericX();
    public abstract T getGenericY();
    public abstract T getGenericZ();

    @Override
    public int hashCode() {
        return Objects.hash(getGenericX(), getGenericY(), getGenericZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Vec3<?> vec3)) return false;
        return
                vec3.getGenericX().equals(getGenericX()) &&
                vec3.getGenericY().equals(getGenericY()) &&
                vec3.getGenericZ().equals(getGenericZ());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getGenericX() + ", " + getGenericY() + ", " + getGenericZ() + ")";
    }

}
