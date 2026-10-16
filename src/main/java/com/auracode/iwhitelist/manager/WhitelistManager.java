package com.auracode.iwhitelist.manager;

import com.auracode.iwhitelist.IWhiteListPlugin;
import com.auracode.iwhitelist.data.WhitelistEntry;
import com.auracode.iwhitelist.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the whitelist state: entries, the on/off switch, temporary expiry and persistence.
 */
public final class WhitelistManager {

    private static final String ROOT = "entries";
    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IWhiteListPlugin plugin;
    private final File file;
    private final File logFile;
    private final Map<UUID, WhitelistEntry> entries = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();

    private boolean enabled;
    private BukkitTask task;
    private volatile boolean saveScheduled;

    public WhitelistManager(IWhiteListPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "whitelist.yml");
        this.logFile = new File(plugin.getDataFolder(), "actions.log");
        this.enabled = plugin.getPluginConfig().isWhitelistEnabled();
        load();
    }

    public void load() {
        entries.clear();
        if (!file.exists()) {
            saveSync();
            return;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            this.enabled = yaml.getBoolean("enabled", plugin.getPluginConfig().isWhitelistEnabled());
            ConfigurationSection root = yaml.getConfigurationSection(ROOT);
            if (root != null) {
                for (String key : root.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        entries.put(uuid, WhitelistEntry.from(uuid, root.getConfigurationSection(key)));
                    } catch (IllegalArgumentException exception) {
                        plugin.getLogger().warning("Skipping malformed whitelist entry: " + key);
                    }
                }
            }
            plugin.getLogger().info("Loaded " + entries.size() + " whitelist entries (enabled=" + enabled + ").");
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to load whitelist: " + exception.getMessage());
        }
    }

    public void start() {
        long interval = plugin.getPluginConfig().getExpiryCheckSeconds() * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpired, interval, interval);
        rescheduleAll();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (BukkitTask expiryTask : expiryTasks.values()) {
            expiryTask.cancel();
        }
        expiryTasks.clear();
    }

    /**
     * Schedules a precise, real-time expiry task for every temporary entry
     * (used after a (re)load so timers continue exactly where they left off).
     */
    private void rescheduleAll() {
        for (WhitelistEntry entry : new ArrayList<>(entries.values())) {
            if (!entry.isPermanent()) {
                scheduleExpiry(entry);
            }
        }
    }

    private void scheduleExpiry(WhitelistEntry entry) {
        cancelExpiry(entry.uuid());
        long remaining = entry.remaining(System.currentTimeMillis());
        long ticks = Math.max(1L, remaining / 50L);
        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> expire(entry.uuid()), ticks);
        expiryTasks.put(entry.uuid(), expiryTask);
    }

    private void cancelExpiry(UUID uuid) {
        BukkitTask expiryTask = expiryTasks.remove(uuid);
        if (expiryTask != null) {
            expiryTask.cancel();
        }
    }

    private void expire(UUID uuid) {
        WhitelistEntry entry = entries.get(uuid);
        if (entry == null) {
            cancelExpiry(uuid);
            return;
        }
        if (!entry.isExpired(System.currentTimeMillis())) {
            scheduleExpiry(entry);
            return;
        }
        if (entries.remove(uuid) == null) {
            return;
        }
        cancelExpiry(uuid);
        logAction("CONSOLE", "expire", entry.name());
        notifyStaff("notify.expired", Placeholders.of().set("player", entry.name()).build());
        if (plugin.getPluginConfig().isKickOnExpiry()) {
            Player online = Bukkit.getPlayer(entry.uuid());
            if (online != null && !online.hasPermission("iwhitelist.bypass")) {
                online.kick(plugin.kickComponent(online.getName()));
            }
        }
        save();
    }

    /**
     * Fallback sweep (in case a scheduled task was missed, e.g. after a TPS spike).
     */
    private void checkExpired() {
        long now = System.currentTimeMillis();
        for (WhitelistEntry entry : new ArrayList<>(entries.values())) {
            if (entry.isExpired(now)) {
                expire(entry.uuid());
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value, String actor) {
        this.enabled = value;
        save();
        logAction(actor, value ? "enable" : "disable", "-");
    }

    public boolean contains(UUID uuid) {
        return entries.containsKey(uuid);
    }

    public boolean isWhitelisted(UUID uuid) {
        WhitelistEntry entry = entries.get(uuid);
        return entry != null && !entry.isExpired(System.currentTimeMillis());
    }

    public WhitelistEntry get(UUID uuid) {
        return entries.get(uuid);
    }

    public WhitelistEntry getByName(String name) {
        if (name == null) {
            return null;
        }
        String needle = name.toLowerCase(Locale.ROOT);
        return entries.values().stream()
                .filter(entry -> entry.name().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElse(null);
    }

    public int count() {
        return entries.size();
    }

    public List<WhitelistEntry> sortedEntries() {
        List<WhitelistEntry> list = new ArrayList<>(entries.values());
        list.sort(Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return list;
    }

    public boolean add(UUID uuid, String name, UUID addedBy, String addedByName, long durationMillis) {
        long now = System.currentTimeMillis();
        long expiresAt = durationMillis <= 0L ? WhitelistEntry.PERMANENT : now + durationMillis;
        WhitelistEntry entry = new WhitelistEntry(uuid, name, now, addedBy, addedByName, expiresAt);
        if (entries.putIfAbsent(uuid, entry) != null) {
            return false;
        }
        if (!entry.isPermanent()) {
            scheduleExpiry(entry);
        }
        save();
        logAction(addedByName, "add", name);
        return true;
    }

    public WhitelistEntry remove(UUID uuid, String actor) {
        WhitelistEntry removed = entries.remove(uuid);
        cancelExpiry(uuid);
        if (removed != null) {
            save();
            logAction(actor, "remove", removed.name());
        }
        return removed;
    }

    public void updateName(UUID uuid, String name) {
        WhitelistEntry entry = entries.get(uuid);
        if (entry != null) {
            entry.setName(name);
            save();
        }
    }

    public void save() {
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveScheduled = false;
            saveSync();
        });
    }

    public void saveSync() {
        synchronized (saveLock) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("enabled", enabled);
            ConfigurationSection root = yaml.createSection(ROOT);
            for (WhitelistEntry entry : entries.values()) {
                entry.write(root.createSection(entry.uuid().toString()));
            }
            try {
                yaml.save(file);
            } catch (IOException exception) {
                plugin.getLogger().severe("Failed to save whitelist: " + exception.getMessage());
            }
        }
    }

    public void notifyStaff(String key, Map<String, String> placeholders) {
        if (!plugin.getPluginConfig().isNotifyAdmins()) {
            return;
        }
        Bukkit.getConsoleSender().sendMessage(plugin.getMessages().component(key, placeholders));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("iwhitelist.notify")) {
                player.sendMessage(plugin.getMessages().component(key, placeholders));
            }
        }
    }

    private void logAction(String actor, String action, String target) {
        plugin.getLogger().info("[IWhiteList] " + actor + " -> " + action + " " + target);
        if (!plugin.getPluginConfig().isLogActions()) {
            return;
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(LOG_FORMAT.format(LocalDateTime.now()) + " | " + actor + " | " + action + " | " + target);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write action log: " + exception.getMessage());
        }
    }
}
