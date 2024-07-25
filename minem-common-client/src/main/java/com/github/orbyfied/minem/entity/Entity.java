package com.github.orbyfied.minem.entity;

import com.github.orbyfied.minem.math.Vec3d;

/**
 * The data on an entity in the current world.
 */
public interface Entity {

    /**
     * Get the entity ID for this entity.
     *
     * @return The EID.
     */
    int getEntityID();

    /**
     * Get the current tracked position for this entity.
     *
     * @return The entity.
     */
    Vec3d getPosition();

    /**
     * Get the horizontal rotation (yaw) for this entity.
     *
     * @return The yaw.
     */
    float getYaw();

    /**
     * Get the vertical rotation (pitch) for this entity.
     *
     * @return The pitch.
     */
    float getPitch();

}
