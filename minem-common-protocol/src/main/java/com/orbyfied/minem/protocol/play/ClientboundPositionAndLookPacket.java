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
public class ClientboundPositionAndLookPacket {

    public static final int FLAG_REL_X     = 0x01;
    public static final int FLAG_REL_Y     = 0x02;
    public static final int FLAG_REL_Z     = 0x04;
    public static final int FLAG_REL_YAW   = 0x08;
    public static final int FLAG_REL_PITCH = 0x10;

    double x;    // The X position (relative or absolute is determined by the flags below)
    double y;    // The Y position (relative or absolute is determined by the flags below)
    double z;    // The Z position (relative or absolute is determined by the flags below)
    float yaw;   // The yaw (relative or absolute is determined by the flags below)
    float pitch; // The pitch (relative or absolute is determined by the flags below)
    byte flags;  // Relative or absolute bitfield

}
