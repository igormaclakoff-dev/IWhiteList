package com.auracode.iwhitelist.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * A single whitelist record. A non-positive {@link #expiresAt()} means the entry is permanent.
 */
public final class WhitelistEntry {

    public static final long PERMANENT = 0L;

    private final UUID uuid;
    private String name;
    private final long addedAt;
    private final UUID addedBy;
    private final String addedByName;
    private final long expiresAt;

    public WhitelistEntry(UUID uuid, String name, long addedAt, UUID addedBy, String addedByName, long expiresAt) {
        this.uuid = uuid;
        this.name = name;
        this.addedAt = addedAt;
        this.addedBy = addedBy;
        this.addedByName = addedByName;
        this.expiresAt = expiresAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public long addedAt() {
        return addedAt;
    }

    public UUID addedBy() {
        return addedBy;
    }

    public String addedByName() {
        return addedByName;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt <= PERMANENT;
    }

    public boolean isExpired(long now) {
        return !isPermanent() && now >= expiresAt;
    }

    public long remaining(long now) {
        return isPermanent() ? Long.MAX_VALUE : Math.max(0L, expiresAt - now);
    }

    public static WhitelistEntry from(UUID uuid, ConfigurationSection section) {
        return new WhitelistEntry(
                uuid,
                section.getString("name", "unknown"),
                section.getLong("added-at", System.currentTimeMillis()),
                parseUuid(section.getString("added-by")),
                section.getString("added-by-name", "CONSOLE"),
                section.getLong("expires-at", PERMANENT));
    }

    public void write(ConfigurationSection section) {
        section.set("name", name);
        section.set("added-at", addedAt);
        section.set("added-by", addedBy == null ? null : addedBy.toString());
        section.set("added-by-name", addedByName);
        section.set("expires-at", expiresAt);
    }

    private static UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
