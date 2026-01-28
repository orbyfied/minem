package com.orbyfied.minem.model.entity;

public interface LivingEntity extends Entity {

    float getHealth();

    default boolean isDead() {
        return getHealth() < 0f;
    }

}
