package com.github.orbyfied.minem.math;

import lombok.Getter;
import lombok.Setter;

/**
 * A vector of 3 doubles.
 */
public class Vec3d implements Vec3<Double> {

    @Getter
    public double x, y, z; // Components

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d() { }

    public Vec3d(Vec3d other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public Vec3d(Vec3<?> other) {
        this.x = other.getGenericX().intValue();
        this.y = other.getGenericY().intValue();
        this.z = other.getGenericZ().intValue();
    }

    public Vec3d copy() { return new Vec3d(x, y, z); }
    public Vec3d xzy() { return new Vec3d(x, z, y); }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public Vec3d x(double x) { this.x = x; return this; }
    public Vec3d y(double y) { this.y = y; return this; }
    public Vec3d z(double z) { this.z = z; return this; }

    @Override public Double getGenericX() { return x; }
    @Override public Double getGenericY() { return y; }
    @Override public Double getGenericZ() { return z; }

}

