package com.github.orbyfied.minem.math;

import static java.lang.Math.*;

/**
 * Utilities for working with Minecraft yaw and pitch math.
 */
public class MinecraftRotation {

    public static Vec3d lookVector(float yaw, float pitch) {
        double x = -cos(pitch) * sin(yaw);
        double y = -sin(pitch);
        double z =  cos(pitch) * cos(yaw);
        return new Vec3d(x, y, z);
    }

    public static Vec3d lookVector(Vec2f yawAndPitch) {
        return lookVector(yawAndPitch.x, yawAndPitch.y);
    }

    // Ensure the given look vector is normalized
    public static Vec2f yawAndPitchFromLookVector(Vec3d lookVector) {
        double r = lookVector.magnitude();
        float yaw = (float) (-atan2(lookVector.x, lookVector.z) / PI * 180);
        if (yaw < 0) {
            yaw = 360 - yaw;
        }

        float pitch = (float) (-asin(lookVector.y / r) / PI * 180);
        return new Vec2f(yaw, pitch);
    }

}
