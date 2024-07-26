package com.github.orbyfied.minem.math;

import lombok.Getter;

/**
 * A vector of 2 floats.
 */
public class Vec2f extends Vec2<Float> {

    @Getter
    public float x, y; // Components

    public Vec2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2f() { }

    public Vec2f(Vec2f other) {
        this.x = other.x;
        this.y = other.y;
    }

    public Vec2f(Vec3<?> other) {
        this.x = other.getGenericX().floatValue();
        this.y = other.getGenericY().floatValue();
    }

    public Vec2f copy() { return new Vec2f(x, y); }
    public Vec2f yx() { return new Vec2f(y, x); }

    public float x() { return x; }
    public float y() { return y; }
    public Vec2f x(float x) { this.x = x; return this; }
    public Vec2f y(float y) { this.y = y; return this; }

    @Override public Float getGenericX() { return x; }
    @Override public Float getGenericY() { return y; }

}
