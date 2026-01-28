package com.orbyfied.minem.protocol.play;

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
        switch (val) {
            case -1: { return NETHER; }
            case  0: { return OVERWORLD; }
            case  1: { return END; }
            default: { return null; }
        }
    }

}
