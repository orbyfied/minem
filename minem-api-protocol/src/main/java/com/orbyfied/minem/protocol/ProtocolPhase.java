package com.orbyfied.minem.protocol;

/**
 * What phase the game is currently in.
 */
public interface ProtocolPhase {

    // Basically an extensible enum
    String name();
    int ordinal();

}
