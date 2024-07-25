package com.github.orbyfied.minem.protocol.play;

import lombok.*;

/**
 * Data class only, needs static implementation.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ServerboundPlayerPositionPacket {

    double x;         // Absolute coordinates
    double y;
    double z;
    boolean grounded; // Whether the player is currently grounded

}
