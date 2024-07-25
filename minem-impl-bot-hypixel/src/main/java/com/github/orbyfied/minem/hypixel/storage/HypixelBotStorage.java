package com.github.orbyfied.minem.hypixel.storage;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data storage for the Hypixel bot.
 */
@SuppressWarnings("unchecked")
@Getter
public abstract class HypixelBotStorage {

    // Player Data
    Map<String, Object> defaultPlayerProperties = new HashMap<>();
    Map<UUID, Map<String, Object>> playerProperties = new HashMap<>();

    public MapAccess<UUID, Map<String, Object>> allPlayers() {
        return new MapAccess<>(playerProperties);
    }

    public MapAccess<String, Object> forPlayer(UUID uuid) {
        return new MapAccess<>(playerProperties.computeIfAbsent(uuid, __ -> new HashMap<>()));
    }

    public MapAccess<String, Object> defaultPlayerProperties() {
        return new MapAccess<>(defaultPlayerProperties);
    }

    /* -------------- IO -------------- */

    public Map<String, Object> saveCompactDefault() {
        Map<String, Object> map = new HashMap<>();
        map.put("player-data", playerProperties);
        map.put("default-player-data", defaultPlayerProperties);
        return map;
    }

    public void loadCompactDefault(Map<String, Object> map) {
        playerProperties = (Map<UUID, Map<String, Object>>) map.get("player-data");
        defaultPlayerProperties = (Map<String, Object>) map.get("default-player-data");
    }

    public abstract void save();
    public abstract void load();

}
