package com.orbyfied.minem.math;

import lombok.Getter;

/**
 * A vector of 3 doubles, mutable by default.
 */
public class Vec3d extends Vec3<Double> {

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
        this.x = other.getGenericX().doubleValue();
        this.y = other.getGenericY().doubleValue();
        this.z = other.getGenericZ().doubleValue();
    }

    public Vec3d copy() { return new Vec3d(x, y, z); }
    public Vec3d xyz() { return new Vec3d(x, y, z); }
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

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3d normalized() {
        double mag = Math.sqrt(x * x + y * y + z * z);
        return new Vec3d(x / mag, y / mag, z / mag);
    }

    @Mutates
    public Vec3d normalize() {
        double mag = Math.sqrt(x * x + y * y + z * z);
        this.x = x / mag;
        this.y = y / mag;
        this.z = z / mag;
        return this;
    }

    public Vec3d sub(Vec3d v) { return new Vec3d(x - v.x, y - v.y, z - v.z); }
    public Vec3d add(Vec3d v) { return new Vec3d(x + v.x, y + v.y, z + v.z); }
    public Vec3d mul(Vec3d v) { return new Vec3d(x * v.x, y * v.y, z * v.z); }
    public Vec3d div(Vec3d v) { return new Vec3d(x / v.x, y / v.y, z / v.z); }
    public Vec3d sub(double vx, double vy, double vz) { return new Vec3d(x - vx, y - vy, z - vz); }
    public Vec3d add(double vx, double vy, double vz) { return new Vec3d(x + vx, y + vy, z + vz); }
    public Vec3d mul(double vx, double vy, double vz) { return new Vec3d(x * vx, y * vy, z * vz); }
    public Vec3d div(double vx, double vy, double vz) { return new Vec3d(x / vx, y / vy, z / vz); }
    public Vec3d mul(double c) { return new Vec3d(x * c, y * c, z * c); }
    public Vec3d div(double c) { return new Vec3d(x / c, y / c, z / c); }

    @Mutates public Vec3d subMut(Vec3d v) { this.x = x - v.x; this.y = y - v.y; this.z = z - v.z; return this; }
    @Mutates public Vec3d addMut(Vec3d v) { this.x = x + v.x; this.y = y + v.y; this.z = z + v.z; return this; }
    @Mutates public Vec3d mulMut(Vec3d v) { this.x = x * v.x; this.y = y * v.y; this.z = z * v.z; return this; }
    @Mutates public Vec3d divMut(Vec3d v) { this.x = x / v.x; this.y = y / v.y; this.z = z / v.z; return this; }
    @Mutates public Vec3d mulMut(double c) { this.x = x * c; this.y = y * c; this.z = z * c; return this; }
    @Mutates public Vec3d divMut(double c) { this.x = x / c; this.y = y / c; this.z = z / c; return this; }

}

