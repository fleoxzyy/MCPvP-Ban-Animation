package com.fleoxxzy.mcpvpBanAnimation;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationManager;
import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.core.EntityTags;
import com.fleoxxzy.mcpvpBanAnimation.core.LeftoverCleaner;
import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class MCPvPBanAnimation extends JavaPlugin {

    private Platform platform;
    private ConfigManager configManager;
    private AnimationManager animations;
    private LeftoverCleaner leftoverCleaner;

    @Override
    public void onEnable() {
        try {
            platform = Platform.initialize(this);
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Unsupported server platform. Disabling.", t);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Platform: " + platform.describe());
        if (!platform.version().isKnown()) {
            getLogger().warning("Could not parse the server version '" + platform.rawVersion()
                    + "'. Relying on feature detection only.");
        } else if (!platform.version().isAtLeast(1, 21, 0)) {
            getLogger().warning("This plugin targets Minecraft 1.21+. Detected " + platform.version() + ". Things may break.");
        }

        configManager = new ConfigManager(this);
        try {
            configManager.load();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load the configuration. Disabling.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (configManager.config().debug()) {
            getLogger().info("Features: " + platform.features().asMap());
        }

        animations = new AnimationManager(this, platform, configManager, new EntityTags(this));
        leftoverCleaner = new LeftoverCleaner(platform, animations, getLogger());
        getServer().getPluginManager().registerEvents(leftoverCleaner, this);
        // Worlds may already be loaded (e.g. /reload). Scan once the server has finished starting.
        platform.scheduler().runGlobal(leftoverCleaner::scanLoadedWorlds);
    }

    @Override
    public void onDisable() {
        if (animations != null) {
            // Release every held ban before anything else shuts down.
            animations.shutdownAll();
        }
        if (leftoverCleaner != null) {
            try {
                leftoverCleaner.scanLoadedWorlds();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error while removing animation entities", t);
            }
        }
        if (platform != null) {
            try {
                platform.binding().shutdown();
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Error while shutting down the platform layer", t);
            }
        }
    }

    public Platform platform() {
        return platform;
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public AnimationManager animations() {
        return animations;
    }
}
