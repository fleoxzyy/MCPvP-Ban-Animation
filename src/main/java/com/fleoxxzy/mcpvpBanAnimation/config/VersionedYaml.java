package com.fleoxxzy.mcpvpBanAnimation.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads a YAML file from the data folder and upgrades it against the bundled default.
 *
 * <p>If the file's {@code config-version} is lower than the bundled one, the old file is backed up
 * and rewritten from the default, keeping the user's values (also for renamed options). A broken file is never overwritten: it is renamed to {@code .broken}
 * and the defaults are used.
 */
public final class VersionedYaml {

    public static final String VERSION_KEY = "config-version";

    private VersionedYaml() {
    }

    public static YamlConfiguration load(Plugin plugin, String name) throws IOException, InvalidConfigurationException {
        File file = new File(plugin.getDataFolder(), name);
        YamlConfiguration defaults = loadBundled(plugin, name);

        if (!file.exists()) {
            plugin.saveResource(name, false);
        }

        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        try {
            config.load(file);
        } catch (InvalidConfigurationException e) {
            File broken = new File(file.getParentFile(), name + ".broken");
            Files.move(file.toPath(), broken.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.saveResource(name, true);
            plugin.getLogger().log(Level.SEVERE, name + " is invalid YAML. It was moved to " + broken.getName()
                    + " and the default was restored.", e);
            config = new YamlConfiguration();
            config.options().parseComments(true);
            config.load(file);
        }

        int current = config.getInt(VERSION_KEY, 0);
        int latest = defaults.getInt(VERSION_KEY, 1);
        if (current < latest) {
            config = upgrade(plugin, file, config, current, latest);
        } else if (current > latest) {
            plugin.getLogger().warning(name + " has config-version " + current + ", but this build only knows version "
                    + latest + ". Was the plugin downgraded? Unknown keys are ignored.");
        }

        // Missing keys always fall back to the bundled defaults in memory, even without an upgrade.
        config.setDefaults(defaults);
        return config;
    }

    /**
     * Rewrites the file from the bundled default (so removed options disappear and the comments
     * are current) and carries over every value the user had, including renamed options.
     */
    private static YamlConfiguration upgrade(Plugin plugin, File file, YamlConfiguration old, int from, int to)
            throws IOException, InvalidConfigurationException {
        File backup = new File(file.getParentFile(), file.getName() + ".v" + from + ".bak");
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);

        YamlConfiguration fresh = loadBundled(plugin, file.getName());
        int kept = 0;
        for (String key : fresh.getKeys(true)) {
            if (fresh.isConfigurationSection(key) || key.equals(VERSION_KEY)) {
                continue;
            }
            String oldKey = old.isSet(key) ? key : RENAMED.get(key);
            if (oldKey != null && old.isSet(oldKey) && !old.isConfigurationSection(oldKey)) {
                fresh.set(key, old.get(oldKey));
                kept++;
            }
        }
        fresh.set(VERSION_KEY, to);
        fresh.save(file);
        plugin.getLogger().info("Upgraded " + file.getName() + " from v" + from + " to v" + to + " (" + kept
                + " of your settings kept, backup: " + backup.getName() + ").");
        return fresh;
    }

    /** New key → where it lived in older versions. */
    private static final Map<String, String> RENAMED = Map.of(
            "animation.mace-height", "mace.height",
            "animation.enchanted-mace", "mace.enchanted",
            "animation.look-up", "replica.look-up",
            "animation.camera-shake", "impact.camera-shake.enabled",
            "animation.replica", "replica.type",
            "animation.label", "replica.label.text",
            "detection.skip-silent-bans", "interception.command.skip-silent",
            "detection.match-kick-message", "interception.kick.match-reason");

    private static YamlConfiguration loadBundled(Plugin plugin, String name) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) {
                throw new IOException("Bundled resource " + name + " is missing from the jar");
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                yaml.load(reader);
            }
        }
        return yaml;
    }
}
