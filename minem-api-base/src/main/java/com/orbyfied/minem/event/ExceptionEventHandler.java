package com.orbyfied.minem.event;

@FunctionalInterface
public interface ExceptionEventHandler {

    /**
     * Called when an exception/error occurs.
     */
    void onException(Throwable ex);

}
