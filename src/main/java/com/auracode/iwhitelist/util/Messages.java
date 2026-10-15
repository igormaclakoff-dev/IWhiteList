package com.auracode.iwhitelist.util;

import com.auracode.iwhitelist.IWhiteListPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and dispatches MiniMessage messages from messages.yml.
 */
public final class Messages {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final IWhiteListPlugin plugin;
    private FileConfiguration config;

    public Messages(IWhiteListPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        Text.setPrefix(raw("prefix"));
    }

    public String raw(String key) {
        String value = config.getString(key);
        if (value == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return "<red>" + key;
        }
        return value;
    }

    public Component component(String key) {
        return component(key, Map.of());
    }

    public Component component(String key, Map<String, String> placeholders) {
        return Text.parse(raw(key), placeholders);
    }

    public List<Component> componentList(String key, Map<String, String> placeholders) {
        List<Component> lines = new ArrayList<>();
        for (String line : config.getStringList(key)) {
            lines.add(Text.parseItem(line, placeholders));
        }
        return lines;
    }

    /**
     * Formats a positive duration as text, limited to the two largest units.
     */
    public String duration(long millis) {
        long seconds = Math.max(0L, millis / 1_000L);
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        long secs = seconds % 60L;

        StringBuilder builder = new StringBuilder();
        appendUnit(builder, days, raw("duration.days"));
        appendUnit(builder, hours, raw("duration.hours"));
        appendUnit(builder, minutes, raw("duration.minutes"));
        appendUnit(builder, secs, raw("duration.seconds"));
        if (builder.isEmpty()) {
            builder.append('0').append(raw("duration.seconds"));
        }
        return builder.toString();
    }

    private void appendUnit(StringBuilder builder, long value, String unit) {
        if (value <= 0L || builder.length() >= 2) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value).append(unit);
    }

    /**
     * Formats a remaining time as a real-time {@code [Nд ]HH:MM:SS} clock,
     * so the value visibly ticks down every second.
     */
    public String clock(long millis) {
        long total = Math.max(0L, millis) / 1_000L;
        long days = total / 86_400L;
        long hours = (total % 86_400L) / 3_600L;
        long minutes = (total % 3_600L) / 60L;
        long seconds = total % 60L;
        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return days > 0L ? days + raw("duration.days") + " " + time : time;
    }

    public String permanent() {
        return raw("duration.permanent");
    }

    public String date(long epochMillis) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    public void send(CommandSender target, String key) {
        send(target, key, Map.of());
    }

    public void send(CommandSender target, String key, Map<String, String> placeholders) {
        if (target == null) {
            return;
        }
        target.sendMessage(component(key, placeholders));
    }

    public void sendList(CommandSender target, String key, Map<String, String> placeholders) {
        if (target == null) {
            return;
        }
        for (Component line : componentList(key, placeholders)) {
            target.sendMessage(line);
        }
    }

    public void broadcast(String key, Map<String, String> placeholders) {
        Component message = component(key, placeholders);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
}
