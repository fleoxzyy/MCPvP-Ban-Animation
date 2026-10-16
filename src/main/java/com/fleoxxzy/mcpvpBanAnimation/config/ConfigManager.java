package com.fleoxxzy.mcpvpBanAnimation.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Owns the current {@link PluginConfig} + {@link Messages}. Reads are lock-free (volatile), and a
 * reload either fully succeeds or leaves the previous snapshot in place.
 */
public final class ConfigManager {

    private final Plugin plugin;
    private volatile PluginConfig config;
    private volatile Messages messages;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Loads both files. Throws if they cannot be read at all (only possible on first load). */
    public void load() throws Exception {
        YamlConfiguration configYaml = VersionedYaml.load(plugin, "config.yml");
        YamlConfiguration messagesYaml = VersionedYaml.load(plugin, "messages.yml");
        PluginConfig newConfig = PluginConfig.from(configYaml, plugin.getLogger());
        Messages newMessages = Messages.from(messagesYaml);
        this.config = newConfig;
        this.messages = newMessages;
    }

    public PluginConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }
}
