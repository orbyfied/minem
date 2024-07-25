package com.github.orbyfied.minem.math;

import lombok.Getter;
import lombok.Setter;

/**
 * A vector of 3 integers.
 */
public class Vec3i extends Vec3<Integer> {

    @Getter
    public int x, y, z; // Components

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3i() { }

    public Vec3i(Vec3i other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public Vec3i(Vec3<?> other) {
        this.x = other.getGenericX().intValue();
        this.y = other.getGenericY().intValue();
        this.z = other.getGenericZ().intValue();
    }

    public Vec3i copy() { return new Vec3i(x, y, z); }
    public Vec3i xzy() { return new Vec3i(x, z, y); }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public Vec3i x(int x) { this.x = x; return this; }
    public Vec3i y(int y) { this.y = y; return this; }
    public Vec3i z(int z) { this.z = z; return this; }

    @Override public Integer getGenericX() { return x; }
    @Override public Integer getGenericY() { return y; }
    @Override public Integer getGenericZ() { return z; }

}
