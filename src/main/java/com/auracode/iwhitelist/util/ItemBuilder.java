package com.auracode.iwhitelist.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for inventory icons. Uses Adventure components for name/lore.
 */
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, amount));
        this.meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
    }

    public ItemBuilder name(Component name) {
        if (meta != null) {
            meta.displayName(name);
        }
        return this;
    }

    public ItemBuilder addLore(Component line) {
        lore.add(line);
        return this;
    }

    public ItemBuilder lore(List<Component> lines) {
        lore.addAll(lines);
        return this;
    }

    public ItemBuilder glow(boolean glowing) {
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glowing);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
