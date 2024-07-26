package com.github.orbyfied.minem.hypixel.storage;

import lombok.Getter;

import java.util.*;

/**
 * Data storage for the Hypixel bot.
 */
@SuppressWarnings("unchecked")
@Getter
public abstract class HypixelBotStorage {

    final List<UUID> botOwners = new ArrayList<>();

    public HypixelBotStorage owner(UUID uuid) {
        botOwners.add(uuid);
        return this;
    }

    public HypixelBotStorage owner(String uuid) {
        return owner(UUID.fromString(uuid));
    }

    // Player Data
    Map<String, Object> defaultPlayerProperties = new HashMap<>();
    Map<String, Map<String, Object>> playerProperties = new HashMap<>();

    public MapAccess<String, Map<String, Object>> allPlayers() {
        return MapAccess.of(playerProperties);
    }

    // for some security and force shit
    private PlayerDataMap process(UUID uuid, PlayerDataMap map) {
        if (botOwners.contains(uuid)) {
            map.set("rank", /* operator */ 5);
        }

        return map;
    }

    public PlayerDataMap forPlayer(UUID uuid) {
        return process(uuid, new PlayerDataMap(playerProperties.computeIfAbsent(uuid.toString(), __ -> new HashMap<>(defaultPlayerProperties)), defaultPlayerProperties()));
    }

    public MapAccess<String, Object> defaultPlayerProperties() {
        return MapAccess.of(defaultPlayerProperties);
    }

    /* -------------- IO -------------- */

    public Map<String, Object> saveCompactDefault() {
        Map<String, Object> map = new HashMap<>();
        map.put("player-data", playerProperties);
        map.put("default-player-data", defaultPlayerProperties);
        return map;
    }

    public void loadCompactDefault(Map<String, Object> map) {
        playerProperties = (Map<String, Map<String, Object>>) map.get("player-data");
        defaultPlayerProperties = (Map<String, Object>) map.get("default-player-data");
    }

    public abstract void save();
    public abstract void load();

}
