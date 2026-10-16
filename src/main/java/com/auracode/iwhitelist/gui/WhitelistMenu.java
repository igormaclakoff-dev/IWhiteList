package com.auracode.iwhitelist.gui;

import com.auracode.iwhitelist.IWhiteListPlugin;
import com.auracode.iwhitelist.data.WhitelistEntry;
import com.auracode.iwhitelist.util.ItemBuilder;
import com.auracode.iwhitelist.util.Placeholders;
import com.auracode.iwhitelist.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Paginated whitelist overview with add / toggle / remove actions.
 * The menu refreshes every second so temporary entries tick down in real time.
 */
public final class WhitelistMenu implements PluginMenu {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 28;
    private static final int REFRESH_TICKS = 20;
    private static final int SLOT_ADD = 4;
    private static final int SLOT_TOGGLE = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_CLOSE = 53;

    private final IWhiteListPlugin plugin;
    private final Player viewer;
    private final Inventory inventory;

    private List<WhitelistEntry> entries;
    private int page;
    private BukkitTask timer;

    public WhitelistMenu(IWhiteListPlugin plugin, Player viewer, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.entries = new ArrayList<>(plugin.getWhitelistManager().sortedEntries());
        this.page = clampPage(page);
        this.inventory = Bukkit.createInventory(this, SIZE, title());
        render();
        this.timer = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, REFRESH_TICKS, REFRESH_TICKS);
    }

    private Component title() {
        return Text.parseItem(plugin.getMessages().raw("menu.title"),
                Placeholders.of().set("page", page + 1).build());
    }

    private int clampPage(int requested) {
        return Math.min(Math.max(0, requested), totalPages() - 1);
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
    }

    /**
     * Live refresh: reloads entries and redraws the view once per second.
     */
    private void tick() {
        if (viewer.getOpenInventory().getTopInventory().getHolder() != this) {
            close();
            return;
        }
        entries = new ArrayList<>(plugin.getWhitelistManager().sortedEntries());
        int clamped = clampPage(page);
        if (clamped != page) {
            page = clamped;
            viewer.getOpenInventory().setTitle(Text.legacy(title()));
        }
        render();
    }

    private void render() {
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, border());
        }

        inventory.setItem(SLOT_ADD, new ItemBuilder(Material.EMERALD)
                .name(Text.parseItem(plugin.getMessages().raw("menu.add.name")))
                .lore(plugin.getMessages().componentList("menu.add.lore", Map.of()))
                .build());

        inventory.setItem(SLOT_TOGGLE, toggleItem());

        if (entries.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(Text.parseItem(plugin.getMessages().raw("menu.empty")))
                    .build());
        } else {
            int start = page * PAGE_SIZE;
            List<Integer> slots = contentSlots();
            for (int i = 0; i < PAGE_SIZE && start + i < entries.size(); i++) {
                inventory.setItem(slots.get(i), entryItem(entries.get(start + i)));
            }
        }

        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, new ItemBuilder(Material.ARROW)
                    .name(Text.parseItem(plugin.getMessages().raw("menu.previous")))
                    .build());
        }
        if (page < totalPages() - 1) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                    .name(Text.parseItem(plugin.getMessages().raw("menu.next")))
                    .build());
        }

        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.PAPER)
                .name(Text.parseItem(plugin.getMessages().raw("menu.info.name")))
                .lore(plugin.getMessages().componentList("menu.info.lore", Placeholders.of()
                        .set("count", entries.size())
                        .set("page", page + 1)
                        .build()))
                .build());

        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .name(Text.parseItem(plugin.getMessages().raw("menu.close")))
                .build());
    }

    private ItemStack toggleItem() {
        boolean enabled = plugin.getWhitelistManager().isEnabled();
        String base = enabled ? "menu.toggle-on" : "menu.toggle-off";
        return new ItemBuilder(enabled ? Material.LIME_DYE : Material.RED_DYE)
                .name(Text.parseItem(plugin.getMessages().raw(base + ".name")))
                .lore(plugin.getMessages().componentList(base + ".lore", Map.of()))
                .build();
    }

    private ItemStack entryItem(WhitelistEntry entry) {
        Map<String, String> placeholders = Placeholders.of()
                .set("player", entry.name())
                .set("actor", entry.addedByName() == null ? "-" : entry.addedByName())
                .set("duration", plugin.getMenuService().durationOf(entry))
                .build();

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.uuid());
            meta.setOwningPlayer(owner);
            meta.displayName(Text.parseItem(plugin.getMessages().raw("menu.entry.name"), placeholders));
            meta.lore(plugin.getMessages().componentList("menu.entry.lore", placeholders));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack border() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build();
    }

    private static List<Integer> contentSlots() {
        List<Integer> slots = new ArrayList<>(PAGE_SIZE);
        for (int row = 1; row <= 4; row++) {
            for (int column = 1; column <= 7; column++) {
                slots.add(row * 9 + column);
            }
        }
        return slots;
    }

    @Override
    public void handleClick(Player who, int slot) {
        switch (slot) {
            case SLOT_ADD -> plugin.getMenuService().beginAdd(who, page);
            case SLOT_TOGGLE -> {
                boolean enabled = plugin.getWhitelistManager().isEnabled();
                plugin.getWhitelistManager().setEnabled(!enabled, plugin.getMenuService().actorName(who));
                plugin.getMessages().send(who, !enabled ? "command.enabled" : "command.disabled");
                plugin.getMenuService().openList(who, page);
            }
            case SLOT_PREVIOUS -> plugin.getMenuService().openList(who, page - 1);
            case SLOT_NEXT -> plugin.getMenuService().openList(who, page + 1);
            case SLOT_CLOSE -> who.closeInventory();
            default -> handleEntryClick(who, slot);
        }
    }

    private void handleEntryClick(Player who, int slot) {
        int index = contentSlots().indexOf(slot);
        if (index < 0 || index >= PAGE_SIZE) {
            return;
        }
        int absolute = page * PAGE_SIZE + index;
        if (absolute < entries.size()) {
            plugin.getMenuService().openConfirm(who, entries.get(absolute), page);
        }
    }

    @Override
    public void close() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player viewer() {
        return viewer;
    }
}
