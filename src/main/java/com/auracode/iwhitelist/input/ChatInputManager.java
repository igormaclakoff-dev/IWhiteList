package com.auracode.iwhitelist.input;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks players who are expected to type a value in chat (e.g. a name to whitelist).
 */
public final class ChatInputManager {

    private static final long TIMEOUT_MILLIS = 60_000L;

    private record Pending(Consumer<String> handler, long createdAt) {
    }

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public void request(Player player, Consumer<String> handler) {
        pending.put(player.getUniqueId(), new Pending(handler, System.currentTimeMillis()));
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    /**
     * Drops expired requests so the map cannot grow when players never finish typing.
     */
    public void prune() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> now - entry.getValue().createdAt() > TIMEOUT_MILLIS);
    }

    public boolean isWaiting(UUID uuid) {
        Pending value = pending.get(uuid);
        return value != null && !isExpired(value);
    }

    /**
     * Removes and returns the pending handler, or {@code null} if none (or expired).
     */
    public Consumer<String> poll(UUID uuid) {
        Pending value = pending.remove(uuid);
        if (value == null || isExpired(value)) {
            return null;
        }
        return value.handler();
    }

    private boolean isExpired(Pending value) {
        return System.currentTimeMillis() - value.createdAt() > TIMEOUT_MILLIS;
    }
}
