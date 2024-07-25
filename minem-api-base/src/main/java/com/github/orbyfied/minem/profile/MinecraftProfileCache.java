package com.github.orbyfied.minem.profile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Caches and resolves {@link MinecraftProfile}s.
 */
public class MinecraftProfileCache {

    static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    final Map<UUID, MinecraftProfile> byUUID = new ConcurrentHashMap<>();
    final Map<String, MinecraftProfile> byName = new ConcurrentHashMap<>();

    public MinecraftProfile referenceUUID(UUID uuid) {
        return byUUID.computeIfAbsent(uuid, __ -> new MinecraftProfile().uuid(uuid));
    }

    public MinecraftProfile forUUID(UUID uuid) {
        return byUUID.get(uuid);
    }

    public MinecraftProfile forName(String name) {
        return byName.get(name);
    }

    public MinecraftProfile setUUID(MinecraftProfile profile, UUID uuid) {
        if (uuid.equals(profile.uuid)) return profile;
        if (profile.uuid != null) byUUID.remove(profile.uuid);
        profile.uuid = uuid;
        byUUID.put(uuid, profile);
        return profile;
    }

    public MinecraftProfile setName(MinecraftProfile profile, String name) {
        if (name.equals(profile.name)) return profile;
        if (profile.name != null) byName.remove(profile.name.toLowerCase());
        profile.name = name;
        byName.put(name.toLowerCase(), profile);
        return profile;
    }

    public MinecraftProfile fetchByName(String name) {
        MinecraftProfile profile1 = byName.get(name.toLowerCase());
        if (profile1 != null) {
            return profile1;
        }

        UUID uuid = MinecraftProfileAPI.fetchUUIDFromName(name);
        MinecraftProfile profile = referenceUUID(uuid);
        setName(profile, name);
        return profile;
    }

    public MinecraftProfile fetchByUUID(UUID uuid) {
        MinecraftProfile profile = byUUID.get(uuid);
        if (profile != null) {
            return profile;
        }

        profile = referenceUUID(uuid);
        String name = MinecraftProfileAPI.fetchNameFromUUID(uuid);
        setName(profile, name);
        return profile;
    }

    public CompletableFuture<MinecraftProfile> fetchByNameAsync(String name) {
        MinecraftProfile profile1 = byName.get(name.toLowerCase());
        if (profile1 != null) {
            return CompletableFuture.completedFuture(profile1);
        }

        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = MinecraftProfileAPI.fetchUUIDFromName(name);
            MinecraftProfile profile = referenceUUID(uuid);
            setName(profile, name);
            return profile;
        }, EXECUTOR);
    }

    public CompletableFuture<MinecraftProfile> fetchByUUIDAsync(UUID uuid) {
        MinecraftProfile profile = byUUID.get(uuid);
        if (profile != null) {
            return CompletableFuture.completedFuture(profile);
        }

        profile = referenceUUID(uuid);
        MinecraftProfile finalProfile = profile;
        return CompletableFuture.supplyAsync(() -> {
            String name = MinecraftProfileAPI.fetchNameFromUUID(uuid);
            setName(finalProfile, name);
            return finalProfile;
        }, EXECUTOR);
    }

}
