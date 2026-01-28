package com.orbyfied.minem.protocol.play;

import lombok.*;

/**
 * Data class only, needs static implementation.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ServerboundPlayerPositionAndLookPacket {

    double x;         // Absolute coordinates
    double y;
    double z;
    float yaw;        // Absolute rotations
    float pitch;
    boolean grounded; // Whether the player is currently grounded

}
