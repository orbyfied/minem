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
public class ServerboundPlayerGroundedPacket {

    boolean grounded; // Whether the player is currently grounded

}
