package com.fleoxxzy.mcpvpBanAnimation.config;

import com.fleoxxzy.mcpvpBanAnimation.text.Placeholders;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of {@code messages.yml}. {@code {prefix}} is substituted automatically.
 * Other placeholders come from the caller.
 */
public final class Messages {

    private final Map<String, String> values;
    private final String prefix;

    private Messages(Map<String, String> values) {
        this.values = values;
        this.prefix = values.getOrDefault("prefix", "");
    }

    public static Messages from(FileConfiguration yaml) {
        Map<String, String> map = new HashMap<>();
        Configuration defaults = yaml.getDefaults();
        if (defaults != null) {
            for (String key : defaults.getKeys(true)) {
                if (defaults.isString(key)) {
                    map.put(key, defaults.getString(key));
                }
            }
        }
        for (String key : yaml.getKeys(true)) {
            if (yaml.isString(key)) {
                map.put(key, yaml.getString(key));
            }
        }
        return new Messages(Map.copyOf(map));
    }

    /** Raw MiniMessage template, or the key itself if missing (makes a missing key easy to spot). */
    public String raw(String key) {
        return values.getOrDefault(key, key);
    }

    /** Template with {@code {prefix}} and the given {@code key, value, key, value...} placeholders filled in. */
    public String get(String key, String... placeholders) {
        Map<String, String> map = placeholders.length == 0 ? new HashMap<>() : new HashMap<>(Placeholders.of(placeholders));
        return format(raw(key), map);
    }

    public String format(String template, Map<String, String> placeholders) {
        // The prefix is MiniMessage itself, so insert it unescaped before the user placeholders.
        String withPrefix = template.replace("{prefix}", prefix);
        return Placeholders.apply(withPrefix, placeholders);
    }
}
