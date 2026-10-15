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

import java.util.Map;

/**
 * Confirmation screen for removing a whitelist entry.
 */
public final class ConfirmMenu implements PluginMenu {

    private static final int SIZE = 27;
    private static final int SLOT_YES = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_NO = 15;

    private final IWhiteListPlugin plugin;
    private final Player viewer;
    private final WhitelistEntry entry;
    private final int page;
    private final Inventory inventory;

    public ConfirmMenu(IWhiteListPlugin plugin, Player viewer, WhitelistEntry entry, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.entry = entry;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, SIZE, Text.parseItem(plugin.getMessages().raw("confirm.title")));
        render();
    }

    private void render() {
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, border());
        }

        Map<String, String> placeholders = Placeholders.of()
                .set("player", entry.name())
                .set("duration", plugin.getMenuService().durationOf(entry))
                .build();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.uuid());
            meta.setOwningPlayer(owner);
            meta.displayName(Text.parseItem(plugin.getMessages().raw("confirm.name"), placeholders));
            meta.lore(plugin.getMessages().componentList("confirm.lore", placeholders));
            head.setItemMeta(meta);
        }
        inventory.setItem(SLOT_INFO, head);

        inventory.setItem(SLOT_YES, new ItemBuilder(Material.LIME_DYE)
                .name(Text.parseItem(plugin.getMessages().raw("confirm.yes")))
                .build());
        inventory.setItem(SLOT_NO, new ItemBuilder(Material.RED_DYE)
                .name(Text.parseItem(plugin.getMessages().raw("confirm.no")))
                .build());
    }

    private ItemStack border() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build();
    }

    @Override
    public void handleClick(Player who, int slot) {
        switch (slot) {
            case SLOT_YES -> {
                plugin.getWhitelistManager().remove(entry.uuid(), plugin.getMenuService().actorName(who));
                plugin.getMessages().send(who, "command.removed",
                        Placeholders.of().set("player", entry.name()).build());
                plugin.getMenuService().openList(who, page);
            }
            case SLOT_NO -> plugin.getMenuService().openList(who, page);
            default -> {
                // decorative slot, ignore
            }
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
