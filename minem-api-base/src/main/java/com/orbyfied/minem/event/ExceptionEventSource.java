package com.orbyfied.minem.event;

public interface ExceptionEventSource {

    Chain<ExceptionEventHandler> onException();

}
