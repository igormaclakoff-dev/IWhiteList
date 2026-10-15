package com.auracode.iwhitelist.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

/**
 * Common contract for clickable inventories owned by the plugin.
 */
public interface PluginMenu extends InventoryHolder {

    void handleClick(Player who, int slot);

    /**
     * Called when the inventory is closed, letting menus release repeating tasks.
     */
    default void close() {
    }
}
