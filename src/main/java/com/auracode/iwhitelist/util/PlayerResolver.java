package com.auracode.iwhitelist.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Resolves player names / UUIDs without blocking on unknown names.
 */
public final class PlayerResolver {

    private PlayerResolver() {
    }

    /**
     * @return the UUID of the player, or {@code null} if it cannot be resolved.
     */
    public static UUID resolveUuid(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return online.getUniqueId();
        }
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            // not a UUID, continue
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    /**
     * Best-effort display name for a resolved UUID.
     */
    public static String displayName(String fallback, UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null && !name.isBlank() ? name : fallback;
    }
}
