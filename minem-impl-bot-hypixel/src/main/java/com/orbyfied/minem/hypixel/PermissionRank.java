package com.orbyfied.minem.hypixel;

import lombok.RequiredArgsConstructor;

/**
 * Permission rank in the bot.
 */
@RequiredArgsConstructor
public class PermissionRank {

    final int id;
    final String name;

    public static final PermissionRank[] DEFAULTS = new PermissionRank[] {
            new PermissionRank(0, "Default"),
            new PermissionRank(1, "VIP"),
            new PermissionRank(2, "MVP"),
            new PermissionRank(3, "Premium"),
            new PermissionRank(4, "Admin"),
            new PermissionRank(5, "Operator")
    };

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }
}
