package com.auracode.iwhitelist.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable, typed view over config.yml.
 */
public final class PluginConfig {

    private final boolean whitelistEnabled;
    private final String kickMessage;
    private final boolean kickOnExpiry;
    private final boolean disableVanillaWhitelist;
    private final boolean notifyAdmins;
    private final boolean logActions;
    private final int expiryCheckSeconds;

    public PluginConfig(FileConfiguration config) {
        this.whitelistEnabled = config.getBoolean("whitelist.enabled", true);
        this.kickMessage = config.getString("whitelist.kick-message", "<red>You are not whitelisted.");
        this.kickOnExpiry = config.getBoolean("whitelist.kick-on-expiry", true);
        this.disableVanillaWhitelist = config.getBoolean("settings.disable-vanilla-whitelist", true);
        this.notifyAdmins = config.getBoolean("settings.notify-admins", true);
        this.logActions = config.getBoolean("settings.log-actions", true);
        this.expiryCheckSeconds = Math.max(1, config.getInt("settings.expiry-check-seconds", 30));
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public boolean isKickOnExpiry() {
        return kickOnExpiry;
    }

    public boolean isDisableVanillaWhitelist() {
        return disableVanillaWhitelist;
    }

    public boolean isNotifyAdmins() {
        return notifyAdmins;
    }

    public boolean isLogActions() {
        return logActions;
    }

    public int getExpiryCheckSeconds() {
        return expiryCheckSeconds;
    }
}
