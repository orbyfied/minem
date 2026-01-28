package com.orbyfied.minem.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

// The reason for a disconnect
@RequiredArgsConstructor
@Getter
public enum DisconnectReason {
    /**
     * A client side error.
     */
    ERROR(Throwable.class),

    /**
     * The remote host/server issued a disconnect with the given text component reason.
     */
    REMOTE(Component.class),

    /**
     * Disconnect was forced by local code
     */
    LOCAL(null);
    final Class<?> detailsType;
}
