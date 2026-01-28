package com.orbyfied.minem.client;

import com.orbyfied.minem.MinecraftClient;

// Event Handler: 50ms tick handler
public interface ClientTickHandler {
    void onTick(MinecraftClient client);
}
