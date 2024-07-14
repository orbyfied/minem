package com.github.orbyfied.minem;

import com.github.orbyfied.minem.protocol.ProtocolPhase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The basic state of the {@link MinecraftClient}.
 */
@RequiredArgsConstructor
@Getter
public enum ClientState {

    NOT_CONNECTED(null),

    ;

    final ProtocolPhase phase;

}
