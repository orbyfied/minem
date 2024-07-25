package com.github.orbyfied.minem.profile;

import java.util.UUID;

public class MinecraftProfile {

    public static MinecraftProfileCache GLOBAL_CACHE = new MinecraftProfileCache();

    /**
     * The UUID.
     */
    volatile UUID uuid;

    /**
     * The username.
     */
    volatile String name;

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    protected MinecraftProfile uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    protected MinecraftProfile username(String username) {
        this.name = username;
        return this;
    }

    @Override
    public String toString() {
        return "MinecraftProfile(" +
                "" + uuid +
                ", name: '" + name + '\'' +
                ')';
    }

}
