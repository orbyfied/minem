package com.github.orbyfied.minem.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Destination {

    FIND(-1),
    SERVERBOUND(0),
    CLIENTBOUND(1)

    ;

    final int idX2Offset; // Used to ensure serverbound uses even numbers and clientbound uses odd in registries

}
