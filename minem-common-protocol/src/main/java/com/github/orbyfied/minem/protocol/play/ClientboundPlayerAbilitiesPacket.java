package com.github.orbyfied.minem.protocol.play;

import lombok.*;

/**
 * Data class, requires static mapping.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ClientboundPlayerAbilitiesPacket {

    /* Parsed from bit field */
    boolean invulnerable;
    boolean flying;
    boolean canFly;
    boolean creativeMode;

    float flySpeed;    // The current fly speed
    float fovModifier; // The FOV modification

}
