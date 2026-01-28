package com.orbyfied.minem.hypixel.storage;

import java.util.Map;

public class PlayerDataMap extends MapAccess<String, Object> {

    public PlayerDataMap(Map<String, Object> map, MapAccess<String, Object> def) {
        super(map, def);
    }

    /* ------------ Helpers ------------ */

    public int getRank() {
        return this.getAsOr("rank", (Number) 0).intValue();
    }

}
