package com.auracode.iwhitelist.listener;

import com.auracode.iwhitelist.IWhiteListPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.Consumer;

/**
 * Captures chat messages from players who are expected to type a value (e.g. a name to whitelist).
 */
public final class ChatInputListener implements Listener {

    private final IWhiteListPlugin plugin;

    public ChatInputListener(IWhiteListPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Consumer<String> handler = plugin.getChatInputManager().poll(event.getPlayer().getUniqueId());
        if (handler == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> handler.accept(text));
    }
}
