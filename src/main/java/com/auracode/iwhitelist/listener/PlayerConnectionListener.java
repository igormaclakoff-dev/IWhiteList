package com.auracode.iwhitelist.listener;

import com.auracode.iwhitelist.IWhiteListPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Keeps whitelist names up to date when a tracked player changes their name.
 */
public final class PlayerConnectionListener implements Listener {

    private final IWhiteListPlugin plugin;

    public PlayerConnectionListener(IWhiteListPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getWhitelistManager().contains(player.getUniqueId())) {
            plugin.getWhitelistManager().updateName(player.getUniqueId(), player.getName());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getChatInputManager().cancel(event.getPlayer().getUniqueId());
    }
}
