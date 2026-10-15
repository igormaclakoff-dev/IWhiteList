package com.auracode.iwhitelist.listener;

import com.auracode.iwhitelist.IWhiteListPlugin;
import com.auracode.iwhitelist.manager.WhitelistManager;
import com.auracode.iwhitelist.util.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Enforces the whitelist at login time and notifies staff about denied players.
 */
public final class LoginListener implements Listener {

    private final IWhiteListPlugin plugin;

    public LoginListener(IWhiteListPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        WhitelistManager manager = plugin.getWhitelistManager();
        if (!manager.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("iwhitelist.bypass")) {
            return;
        }
        if (manager.isWhitelisted(player.getUniqueId())) {
            return;
        }
        event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, plugin.kickComponent(player.getName()));
        manager.notifyStaff("notify.denied", Placeholders.of().set("player", player.getName()).build());
    }
}
