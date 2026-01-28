package com.orbyfied.minem.protocol.play;

import lombok.*;

/**
 * Data class, requires static mapping.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ServerboundPlayerAbilitiesPacket {

    /* Parsed from bit field */
    boolean invulnerable;
    boolean flying;
    boolean canFly;
    boolean creativeMode;

    float flySpeed;  // The current fly speed
    float walkSpeed; // The current walking speed

    public ServerboundPlayerAbilitiesPacket flying(boolean flying) {
        this.flying = flying;
        return this;
    }

    public ServerboundPlayerAbilitiesPacket flySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
        return this;
    }

    public ServerboundPlayerAbilitiesPacket walkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
        return this;
    }

}
