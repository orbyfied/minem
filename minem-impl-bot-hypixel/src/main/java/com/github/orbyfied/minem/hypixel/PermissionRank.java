package com.github.orbyfied.minem.hypixel;

/**
 * Permission rank in the bot.
 */
public record PermissionRank(int id, String name) {

    public static final PermissionRank[] DEFAULTS = new PermissionRank[] {
            new PermissionRank(0, "Default"),
            new PermissionRank(1, "VIP"),
            new PermissionRank(2, "MVP"),
            new PermissionRank(3, "Premium"),
            new PermissionRank(4, "Admin"),
            new PermissionRank(5, "Operator")
    };

}
