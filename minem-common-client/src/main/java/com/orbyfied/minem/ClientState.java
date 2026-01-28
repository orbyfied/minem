package com.orbyfied.minem;

import com.orbyfied.minem.protocol.ProtocolPhase;
import com.orbyfied.minem.protocol.ProtocolPhases;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The basic state of the {@link MinecraftClient}.
 */
@RequiredArgsConstructor
@Getter
public enum ClientState {

    INACTIVE(null),

    CONNECTING(null),

    HANDSHAKE(ProtocolPhases.HANDSHAKE),

    LOGIN(ProtocolPhases.LOGIN),

    CONFIGURATION(ProtocolPhases.CONFIGURATION),

    PLAY(ProtocolPhases.PLAY)

    ;

    final ProtocolPhase phase;

}
