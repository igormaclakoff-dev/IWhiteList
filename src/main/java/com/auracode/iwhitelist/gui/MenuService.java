package com.auracode.iwhitelist.gui;

import com.auracode.iwhitelist.IWhiteListPlugin;
import com.auracode.iwhitelist.data.WhitelistEntry;
import com.auracode.iwhitelist.util.DurationUtil;
import com.auracode.iwhitelist.util.Placeholders;
import com.auracode.iwhitelist.util.PlayerResolver;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Creates and opens the plugin's inventories and handles deferred chat input.
 */
public final class MenuService {

    private final IWhiteListPlugin plugin;

    public MenuService(IWhiteListPlugin plugin) {
        this.plugin = plugin;
    }

    public void openList(Player player, int page) {
        player.openInventory(new WhitelistMenu(plugin, player, page).getInventory());
    }

    public void openConfirm(Player player, WhitelistEntry entry, int page) {
        player.openInventory(new ConfirmMenu(plugin, player, entry, page).getInventory());
    }

    /**
     * Prompts the player to type a name (optionally with a duration) in chat.
     */
    public void beginAdd(Player player, int page) {
        plugin.getChatInputManager().request(player, input -> handleAddInput(player, input, page));
        player.closeInventory();
        plugin.getMessages().send(player, "menu.add-prompt");
    }

    private void handleAddInput(Player player, String input, int page) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("отмена") || value.equalsIgnoreCase("cancel")) {
            plugin.getMessages().send(player, "menu.add-cancelled");
            openList(player, page);
            return;
        }

        String[] parts = value.split("\\s+");
        String name = parts[0];
        long duration = 0L;
        if (parts.length >= 2) {
            Long parsed = DurationUtil.parse(parts[1]);
            if (parsed == null) {
                plugin.getMessages().send(player, "command.bad-duration");
                openList(player, page);
                return;
            }
            duration = parsed;
        }

        UUID uuid = PlayerResolver.resolveUuid(name);
        if (uuid == null) {
            plugin.getMessages().send(player, "command.unknown-player", Placeholders.of().set("player", name).build());
            openList(player, page);
            return;
        }
        if (plugin.getWhitelistManager().contains(uuid)) {
            plugin.getMessages().send(player, "command.already",
                    Placeholders.of().set("player", PlayerResolver.displayName(name, uuid)).build());
            openList(player, page);
            return;
        }

        String display = PlayerResolver.displayName(name, uuid);
        plugin.getWhitelistManager().add(uuid, display, player.getUniqueId(), player.getName(), duration);
        plugin.getMessages().send(player, "command.added", Placeholders.of()
                .set("player", display)
                .set("duration", duration <= 0L ? plugin.getMessages().permanent() : plugin.getMessages().duration(duration))
                .build());
        openList(player, page);
    }

    /**
     * Formats the remaining time of an entry for display, as a live-ticking clock.
     */
    public String durationOf(WhitelistEntry entry) {
        if (entry.isPermanent()) {
            return plugin.getMessages().permanent();
        }
        return plugin.getMessages().clock(entry.remaining(System.currentTimeMillis()));
    }

    public String actorName(Player player) {
        return player == null ? "CONSOLE" : player.getName();
    }
}
