package com.orbyfied.minem.client;

import com.orbyfied.minem.MinecraftClient;

// Event Handler: called when the client disconnects
public interface ClientDisconnectHandler {
    void onDisconnect(MinecraftClient client, DisconnectReason reason, Object details);
}
