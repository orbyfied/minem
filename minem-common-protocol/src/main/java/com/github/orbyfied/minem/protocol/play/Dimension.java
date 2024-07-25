package com.github.orbyfied.minem.protocol.play;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A dimension (type).
 */
@RequiredArgsConstructor
@Getter
public enum Dimension {

    NETHER(-1),
    OVERWORLD(0),
    END(1)
    ;

    final int value;

    public static Dimension from(int val) {
        return switch (val) {
            case -1 -> NETHER;
            case  0 -> OVERWORLD;
            case  1 -> END;
            default -> { throw new IllegalArgumentException(); }
        };
    }

}
