package com.github.orbyfied.minem.protocol;

/**
 * All default protocol phases.
 */
public enum ProtocolPhases implements ProtocolPhase {

    /**
     * Exclusive to UnknownPacket
     */
    UNDEFINED,

    HANDSHAKE,

    STATUS, // unused in client

    LOGIN,

    CONFIGURATION,

    PLAY

}
