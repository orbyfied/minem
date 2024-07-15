package com.github.orbyfied.minem;

import com.github.orbyfied.minem.protocol.ProtocolPhase;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The basic state of the {@link MinecraftClient}.
 */
@RequiredArgsConstructor
@Getter
public enum ClientState {

    NOT_CONNECTED(null),

    HANDSHAKE(ProtocolPhases.HANDSHAKE),

    LOGIN(ProtocolPhases.LOGIN),

    CONFIGURATION(ProtocolPhases.CONFIGURATION),

    PLAY(ProtocolPhases.PLAY)

    ;

    final ProtocolPhase phase;

}
