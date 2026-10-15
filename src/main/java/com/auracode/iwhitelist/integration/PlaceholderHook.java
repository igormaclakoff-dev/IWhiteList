package com.auracode.iwhitelist.integration;

import com.auracode.iwhitelist.IWhiteListPlugin;
import com.auracode.iwhitelist.data.WhitelistEntry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion: %iwhitelist_*%.
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final IWhiteListPlugin plugin;

    public PlaceholderHook(IWhiteListPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "iwhitelist";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AuraCode";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "enabled" -> plugin.getWhitelistManager().isEnabled()
                    ? plugin.getMessages().raw("placeholder.enabled")
                    : plugin.getMessages().raw("placeholder.disabled");
            case "count" -> String.valueOf(plugin.getWhitelistManager().sortedEntries().size());
            case "status" -> player == null ? "-" : status(player);
            case "expires" -> player == null ? "-" : expires(player);
            default -> null;
        };
    }

    private String status(OfflinePlayer player) {
        boolean listed = plugin.getWhitelistManager().isWhitelisted(player.getUniqueId());
        return listed ? plugin.getMessages().raw("placeholder.yes") : plugin.getMessages().raw("placeholder.no");
    }

    private String expires(OfflinePlayer player) {
        WhitelistEntry entry = plugin.getWhitelistManager().sortedEntries().stream()
                .filter(value -> value.uuid().equals(player.getUniqueId()))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            return "-";
        }
        return plugin.getMenuService().durationOf(entry);
    }
}
