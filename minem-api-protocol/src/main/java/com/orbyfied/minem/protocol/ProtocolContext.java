package com.orbyfied.minem.protocol;

import com.orbyfied.minem.Context;

public abstract class ProtocolContext extends Context {

    /**
     * Get the protocol used for this context.
     *
     * @return The protocol instance.
     */
    public abstract Protocol getProtocol();

}
