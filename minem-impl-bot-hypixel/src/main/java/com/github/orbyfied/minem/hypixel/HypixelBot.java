package com.github.orbyfied.minem.hypixel;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.hypixel.storage.HypixelBotStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Core services for the Hypixel bot components.
 */
@RequiredArgsConstructor
@Getter
public class HypixelBot extends ClientComponent {

    /**
     * The data storage to use.
     */
    final HypixelBotStorage storage;

    /**
     * The ranks by ID.
     */
    PermissionRank[] ranks;

    @Override
    protected boolean attach(MinecraftClient client) {
        return true;
    }

    @Override
    protected void resetState() {

    }

    public HypixelBot ranks(PermissionRank[] ranks) {
        this.ranks = ranks;
        return this;
    }

    public PermissionRank getRank(int id) {
        return ranks[id];
    }

    public PermissionRank getRank(String name) {
        for (PermissionRank rank : ranks) {
            if (rank != null && rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }

        return null;
    }

}
