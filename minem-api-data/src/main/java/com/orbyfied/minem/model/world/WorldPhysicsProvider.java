package com.orbyfied.minem.model.world;

import com.orbyfied.minem.model.transform.AxisAlignedBB;

public interface WorldPhysicsProvider {

    // todo: likely refactor completely

    /**
     * @return true if the given bounding box collides with the world
     */
    boolean collidesHorizontally(AxisAlignedBB bb);

    /**
     * @return the Y level of the closest solid block below the box,
     *         or Double.NEGATIVE_INFINITY if none exists
     */
    double getGroundHeight(AxisAlignedBB bb);

    /**
     * @return true if the given bounding box is considered on ground
     */
    boolean isOnGround(AxisAlignedBB bb);

    /**
     * @return true if the given bounding box is in liquid (water/lava)
     */
    boolean isInLiquid(AxisAlignedBB bb);

    /**
     * @return slipperiness value (0.6F default, ice ~0.98F)
     */
    float getBlockSlipperinessBelow(AxisAlignedBB bb);

    static WorldPhysicsProvider stub() {
        return new WorldPhysicsProvider() {
            static final double FIXED_GROUND_HEIGHT = 4;

            @Override
            public boolean collidesHorizontally(AxisAlignedBB bb) {
                return false;
            }

            @Override
            public double getGroundHeight(AxisAlignedBB bb) {
                return FIXED_GROUND_HEIGHT;
            }

            @Override
            public boolean isOnGround(AxisAlignedBB bb) {
                return bb.getMinY() - FIXED_GROUND_HEIGHT < 0.011;
            }

            @Override
            public boolean isInLiquid(AxisAlignedBB bb) {
                return false;
            }

            @Override
            public float getBlockSlipperinessBelow(AxisAlignedBB bb) {
                return 0.6f;
            }
        };
    }
}
