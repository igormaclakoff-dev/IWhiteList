package com.auracode.iwhitelist.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Map;

/**
 * Thin wrapper around Adventure's {@link MiniMessage} used across the plugin.
 */
public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static String prefix = "";

    private Text() {
    }

    /**
     * Sets the global {@code <prefix>} value (MiniMessage is applied to it).
     */
    public static void setPrefix(String value) {
        prefix = value == null ? "" : value;
    }

    public static Component parse(String input) {
        return parse(input, Map.of());
    }

    public static Component parse(String input, Map<String, String> placeholders) {
        if (input == null) {
            return Component.empty();
        }
        TagResolver.Builder builder = TagResolver.builder();
        if (!prefix.isEmpty() && (placeholders == null || !placeholders.containsKey("prefix"))) {
            builder.resolver(Placeholder.parsed("prefix", prefix));
        }
        if (placeholders != null && !placeholders.isEmpty()) {
            placeholders.forEach((key, value) -> builder.resolver(Placeholder.unparsed(key, value)));
        }
        return MINI.deserialize(input, builder.build());
    }

    /**
     * Parses text and removes the default italic decoration that Minecraft items apply.
     */
    public static Component parseItem(String input, Map<String, String> placeholders) {
        return parse(input, placeholders).decoration(TextDecoration.ITALIC, false);
    }

    public static Component parseItem(String input) {
        return parseItem(input, Map.of());
    }
}
