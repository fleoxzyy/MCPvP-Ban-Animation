package com.fleoxxzy.mcpvpBanAnimation.config;

import org.bukkit.configuration.ConfigurationSection;
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
import java.util.logging.Level;

/**
 * Loads a YAML file from the data folder and upgrades it against the bundled default.
 *
 * <p>If the file's {@code config-version} is lower than the bundled one, the old file is backed up,
 * missing keys are added from the default (with their comments), user values are kept, and
 * the version is bumped. A broken file is never overwritten: it is renamed to {@code .broken}
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
            upgrade(plugin, file, config, defaults, current, latest);
        } else if (current > latest) {
            plugin.getLogger().warning(name + " has config-version " + current + ", but this build only knows version "
                    + latest + ". Was the plugin downgraded? Unknown keys are ignored.");
        }

        // Missing keys always fall back to the bundled defaults in memory, even without an upgrade.
        config.setDefaults(defaults);
        return config;
    }

    private static void upgrade(Plugin plugin, File file, YamlConfiguration config, YamlConfiguration defaults,
                                int from, int to) throws IOException {
        File backup = new File(file.getParentFile(), file.getName() + ".v" + from + ".bak");
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);

        int added = mergeMissing(defaults, config);
        config.set(VERSION_KEY, to);
        config.save(file);
        plugin.getLogger().info("Upgraded " + file.getName() + " from v" + from + " to v" + to + " (" + added
                + " new keys, backup: " + backup.getName() + ").");
    }

    /** Copies every leaf key present in {@code source} but missing in {@code target}. Returns the count. */
    private static int mergeMissing(ConfigurationSection source, ConfigurationSection target) {
        int added = 0;
        for (String key : source.getKeys(true)) {
            if (source.isConfigurationSection(key) || target.isSet(key)) {
                continue;
            }
            target.set(key, source.get(key));
            try {
                target.setComments(key, source.getComments(key));
                target.setInlineComments(key, source.getInlineComments(key));
            } catch (Throwable ignored) {
                // Comment API missing on very old builds. The values still merge.
            }
            added++;
        }
        return added;
    }

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
