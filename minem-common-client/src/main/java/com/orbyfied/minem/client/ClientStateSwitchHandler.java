package com.orbyfied.minem.client;

import com.orbyfied.minem.ClientState;

// Event Handler: called when the client switches state
public interface ClientStateSwitchHandler {
    void onStateSwitch(ClientState oldState, ClientState newState);
}
