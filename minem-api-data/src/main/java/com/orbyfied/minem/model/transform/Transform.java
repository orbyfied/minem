package com.orbyfied.minem.model.transform;

import com.orbyfied.minem.math.Vec3d;

/**
 * Minecraft-consistent entity transform.
 */
public class Transform {

    // The 3D position of the transform, this object must be immutable
    // and updates should happen by atomically replacing the instance.
    public Vec3d position;

    public float yaw;
    public float pitch;

    public Transform() {
        this.position = new Vec3d();
    }

    public Transform(Vec3d position) {
        this.position = position;
    }

    public Transform(Vec3d position, float yaw, float pitch) {
        this.position = position;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public Transform orient(float yaw, float pitch) {
        this.pitch = pitch;
        this.yaw = yaw;
        return this;
    }

    public Transform positioned(Vec3d position) {
        this.position = position;
        return this;
    }

    public Transform x(double x) {
        this.position = this.position.copy().x(x);
        return this;
    }

    public Transform y(double y) {
        this.position = this.position.copy().y(y);
        return this;
    }

    public Transform z(double z) {
        this.position = this.position.copy().z(z);
        return this;
    }

    public double x() {
        return this.position.x;
    }

    public double y() {
        return this.position.y;
    }

    public double z() {
        return this.position.z;
    }

    public float yaw() {
        return this.yaw;
    }

    public float pitch() {
        return this.pitch;
    }

    public Vec3d position() {
        return this.position;
    }

    public Transform yaw(float yaw) {
        this.yaw = yaw;
        return this;
    }

    public Transform pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public Transform update(Vec3d pos, float yaw, float pitch) {
        this.position = pos;
        this.yaw = yaw;
        this.pitch = pitch;
        return this;
    }

    public Transform copy() {
        return new Transform(position, yaw, pitch);
    }

}
