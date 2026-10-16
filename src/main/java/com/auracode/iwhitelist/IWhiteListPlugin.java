package com.auracode.iwhitelist;

import com.auracode.iwhitelist.command.IWhiteListCommand;
import com.auracode.iwhitelist.config.PluginConfig;
import com.auracode.iwhitelist.gui.MenuService;
import com.auracode.iwhitelist.input.ChatInputManager;
import com.auracode.iwhitelist.integration.PlaceholderHook;
import com.auracode.iwhitelist.listener.ChatInputListener;
import com.auracode.iwhitelist.listener.LoginListener;
import com.auracode.iwhitelist.listener.MenuListener;
import com.auracode.iwhitelist.listener.PlayerConnectionListener;
import com.auracode.iwhitelist.manager.WhitelistManager;
import com.auracode.iwhitelist.util.Messages;
import com.auracode.iwhitelist.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Map;

/**
 * Plugin entry point. Wires up all components and manages their lifecycle.
 */
public final class IWhiteListPlugin extends JavaPlugin {

    private static final long PRUNE_INTERVAL_TICKS = 20L * 30L;

    private PluginConfig pluginConfig;
    private Messages messages;
    private WhitelistManager whitelistManager;
    private MenuService menuService;
    private ChatInputManager chatInputManager;
    private BukkitTask pruneTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");

        this.pluginConfig = new PluginConfig(getConfig());
        this.messages = new Messages(this);
        this.whitelistManager = new WhitelistManager(this);
        this.menuService = new MenuService(this);
        this.chatInputManager = new ChatInputManager();

        registerCommands();
        registerListeners();
        this.whitelistManager.start();
        this.pruneTask = Bukkit.getScheduler().runTaskTimer(this, chatInputManager::prune,
                PRUNE_INTERVAL_TICKS, PRUNE_INTERVAL_TICKS);
        disableVanillaWhitelist();
        setupPlaceholders();

        getLogger().info("IWhiteList enabled (whitelist " + (whitelistManager.isEnabled() ? "on" : "off")
                + ", " + whitelistManager.sortedEntries().size() + " entries).");
    }

    @Override
    public void onDisable() {
        if (pruneTask != null) {
            pruneTask.cancel();
        }
        if (whitelistManager != null) {
            whitelistManager.shutdown();
            whitelistManager.saveSync();
        }
    }

    private void registerCommands() {
        registerCommand("iwl", new IWhiteListCommand(this));
    }

    private void registerCommand(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);
    }

    private void disableVanillaWhitelist() {
        if (!pluginConfig.isDisableVanillaWhitelist()) {
            return;
        }
        if (Bukkit.hasWhitelist()) {
            Bukkit.setWhitelist(false);
            getLogger().info("Vanilla whitelist has been disabled so IWhiteList can take over.");
        }
    }

    private void setupPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new PlaceholderHook(this).register();
            getLogger().info("Registered PlaceholderAPI expansion.");
        } catch (Throwable throwable) {
            getLogger().warning("Failed to register PlaceholderAPI expansion: " + throwable.getMessage());
        }
    }

    private void saveResourceIfMissing(String fileName) {
        if (!new File(getDataFolder(), fileName).exists()) {
            saveResource(fileName, false);
        }
    }

    public void reload() {
        reloadConfig();
        this.pluginConfig = new PluginConfig(getConfig());
        this.messages.reload();
    }

    public Component kickComponent(String playerName) {
        return Text.parse(pluginConfig.getKickMessage(), Map.of("player", playerName == null ? "" : playerName));
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public Messages getMessages() {
        return messages;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public MenuService getMenuService() {
        return menuService;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }
}
