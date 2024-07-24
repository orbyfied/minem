package com.github.orbyfied.minem.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Destination {

    /** This should never be set on a packet. This is used exclusively to represent
     *  the default value in the {@link Mapping} annotation. */
    FIND(-Integer.MAX_VALUE),

    SERVERBOUND(0),
    CLIENTBOUND(1)

    ;

    final int idX2Offset; // Used to ensure serverbound uses even numbers and clientbound uses odd in registries

}
