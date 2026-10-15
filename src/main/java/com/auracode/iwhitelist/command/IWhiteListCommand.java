package com.auracode.iwhitelist.command;

import com.auracode.iwhitelist.IWhiteListPlugin;
import com.auracode.iwhitelist.data.WhitelistEntry;
import com.auracode.iwhitelist.util.DurationUtil;
import com.auracode.iwhitelist.util.Placeholders;
import com.auracode.iwhitelist.util.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /iwl} - whitelist administration.
 */
public final class IWhiteListCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "menu", "list", "add", "remove", "info", "on", "off", "reload");
    private static final List<String> DURATION_SUGGESTIONS = List.of("1h", "12h", "1d", "7d", "30d", "permanent");
    private static final int LIST_PAGE_SIZE = 10;

    private final IWhiteListPlugin plugin;

    public IWhiteListCommand(IWhiteListPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("iwhitelist.admin")) {
            plugin.getMessages().send(sender, "command.no-permission");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player && player.hasPermission("iwhitelist.command.menu")) {
                plugin.getMenuService().openList(player, 0);
            } else {
                help(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender);
            case "menu" -> menu(sender);
            case "list" -> list(sender, args);
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);
            case "info", "player" -> playerInfo(sender, args);
            case "on", "enable" -> setEnabled(sender, true);
            case "off", "disable" -> setEnabled(sender, false);
            case "reload" -> reload(sender);
            default -> {
                plugin.getMessages().send(sender, "command.unknown-subcommand",
                        Placeholders.of().set("arg", args[0]).build());
                plugin.getMessages().send(sender, "command.usage");
            }
        }
        return true;
    }

    private boolean require(CommandSender sender, String node) {
        if (sender.hasPermission(node)) {
            return true;
        }
        plugin.getMessages().send(sender, "command.no-permission");
        return false;
    }

    private void help(CommandSender sender) {
        if (!require(sender, "iwhitelist.command.help")) {
            return;
        }
        plugin.getMessages().sendList(sender, "command.help", java.util.Map.of());
    }

    private void menu(CommandSender sender) {
        if (!require(sender, "iwhitelist.command.menu")) {
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "command.player-only");
            return;
        }
        plugin.getMenuService().openList(player, 0);
    }

    private void reload(CommandSender sender) {
        if (!require(sender, "iwhitelist.command.reload")) {
            return;
        }
        plugin.reload();
        plugin.getMessages().send(sender, "command.reloaded");
    }

    private void setEnabled(CommandSender sender, boolean value) {
        if (!require(sender, "iwhitelist.command.toggle")) {
            return;
        }
        plugin.getWhitelistManager().setEnabled(value, actor(sender));
        plugin.getMessages().send(sender, value ? "command.enabled" : "command.disabled");
    }

    private void list(CommandSender sender, String[] args) {
        if (!require(sender, "iwhitelist.command.list")) {
            return;
        }
        List<WhitelistEntry> entries = new ArrayList<>(plugin.getWhitelistManager().sortedEntries());
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) LIST_PAGE_SIZE));
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.min(Math.max(1, Integer.parseInt(args[1])), totalPages);
            } catch (NumberFormatException exception) {
                page = 1;
            }
        }
        plugin.getMessages().send(sender, "command.list-header", Placeholders.of()
                .set("page", page)
                .set("total", totalPages)
                .set("count", entries.size())
                .build());
        if (entries.isEmpty()) {
            plugin.getMessages().send(sender, "command.list-empty");
            return;
        }
        int start = (page - 1) * LIST_PAGE_SIZE;
        for (int i = 0; i < LIST_PAGE_SIZE && start + i < entries.size(); i++) {
            WhitelistEntry entry = entries.get(start + i);
            plugin.getMessages().send(sender, "command.list-entry", Placeholders.of()
                    .set("index", start + i + 1)
                    .set("player", entry.name())
                    .set("duration", plugin.getMenuService().durationOf(entry))
                    .build());
        }
    }

    private void add(CommandSender sender, String[] args) {
        if (!require(sender, "iwhitelist.command.add")) {
            return;
        }
        if (args.length < 2) {
            plugin.getMessages().send(sender, "command.usage");
            return;
        }
        String name = args[1];
        long duration = 0L;
        if (args.length >= 3) {
            Long parsed = DurationUtil.parse(args[2]);
            if (parsed == null) {
                plugin.getMessages().send(sender, "command.bad-duration");
                return;
            }
            duration = parsed;
        }
        UUID uuid = PlayerResolver.resolveUuid(name);
        if (uuid == null) {
            plugin.getMessages().send(sender, "command.unknown-player",
                    Placeholders.of().set("player", name).build());
            return;
        }
        String display = PlayerResolver.displayName(name, uuid);
        if (!plugin.getWhitelistManager().add(uuid, display, senderUuid(sender), actor(sender), duration)) {
            plugin.getMessages().send(sender, "command.already",
                    Placeholders.of().set("player", display).build());
            return;
        }
        plugin.getMessages().send(sender, "command.added", Placeholders.of()
                .set("player", display)
                .set("duration", duration <= 0L ? plugin.getMessages().permanent() : plugin.getMessages().duration(duration))
                .build());
    }

    private void remove(CommandSender sender, String[] args) {
        if (!require(sender, "iwhitelist.command.remove")) {
            return;
        }
        if (args.length < 2) {
            plugin.getMessages().send(sender, "command.usage");
            return;
        }
        UUID uuid = PlayerResolver.resolveUuid(args[1]);
        WhitelistEntry entry = uuid == null ? null : plugin.getWhitelistManager().remove(uuid, actor(sender));
        if (entry == null) {
            plugin.getMessages().send(sender, "command.not-in-list",
                    Placeholders.of().set("player", args[1]).build());
            return;
        }
        plugin.getMessages().send(sender, "command.removed",
                Placeholders.of().set("player", entry.name()).build());
    }

    private void playerInfo(CommandSender sender, String[] args) {
        if (!require(sender, "iwhitelist.command.info")) {
            return;
        }
        if (args.length < 2) {
            plugin.getMessages().send(sender, "command.usage");
            return;
        }
        UUID uuid = PlayerResolver.resolveUuid(args[1]);
        WhitelistEntry entry = uuid == null ? null : plugin.getWhitelistManager().get(uuid);
        if (entry == null) {
            plugin.getMessages().send(sender, "command.not-in-list",
                    Placeholders.of().set("player", args[1]).build());
            return;
        }
        plugin.getMessages().send(sender, "info.header",
                Placeholders.of().set("player", entry.name()).build());
        plugin.getMessages().send(sender, "info.added-by", Placeholders.of()
                .set("actor", entry.addedByName() == null ? "-" : entry.addedByName()).build());
        plugin.getMessages().send(sender, "info.added-at",
                Placeholders.of().set("date", plugin.getMessages().date(entry.addedAt())).build());
        plugin.getMessages().send(sender, "info.expires",
                Placeholders.of().set("duration", plugin.getMenuService().durationOf(entry)).build());
    }

    private String actor(CommandSender sender) {
        return sender instanceof Player player ? player.getName() : "CONSOLE";
    }

    private UUID senderUuid(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("iwhitelist.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (root) {
                case "add", "remove", "player" -> filter(onlineNames(), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3 && root.equals("add")) {
            return filter(DURATION_SUGGESTIONS, args[2]);
        }
        return List.of();
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String needle = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                result.add(option);
            }
        }
        return result;
    }
}
