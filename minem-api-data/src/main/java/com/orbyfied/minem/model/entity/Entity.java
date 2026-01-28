package com.orbyfied.minem.model.entity;

import com.orbyfied.minem.math.Vec3d;
import com.orbyfied.minem.model.transform.AxisAlignedBB;
import com.orbyfied.minem.model.transform.Transform;

/**
 * The data on an entity in the current world.
 */
public interface Entity {

    /**
     * Get the entity ID for this entity.
     */
    int getEntityID();

    /**
     * Get the current velocity of the entity.
     */
    Vec3d velocity();

    /**
     * Get the in-world transform of this entity.
     */
    Transform transform();

    /**
     * Get or create the translated bounding box of this entity.
     */
    AxisAlignedBB boundingBox();

}
