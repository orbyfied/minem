package com.github.orbyfied.minem.protocol;

/**
 * All default protocol phases.
 */
public enum ProtocolPhases implements ProtocolPhase {

    HANDSHAKE,

    STATUS, // unused in client

    LOGIN,

    CONFIGURATION,

    PLAY

}
