package com.fleoxxzy.mcpvpBanAnimation.hook;

import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.config.HookMode;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;
import com.fleoxxzy.mcpvpBanAnimation.trigger.TriggerService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects to the API of each installed ban plugin. A hook that is missing or fails to start is
 * not fatal: that plugin's bans are then caught by the command and kick detection, so the
 * animation still plays (only with less precise details).
 */
public final class HookManager {

    private final Plugin plugin;
    private final ConfigManager configs;
    private final HookSupport support;
    private final Logger logger;
    private final Map<HookTarget, BanHook> active = new EnumMap<>(HookTarget.class);

    public HookManager(Plugin plugin, ConfigManager configs, TriggerService triggers) {
        this.plugin = plugin;
        this.configs = configs;
        this.logger = plugin.getLogger();
        this.support = new HookSupport(configs, triggers, logger);
    }

    /** Starts every hook whose plugin is enabled and whose mode is API. Safe to call again after a reload. */
    public synchronized void enableAll() {
        tryEnable(HookTarget.LITEBANS, "LiteBans", s -> new LiteBansHook(s));
        tryEnable(HookTarget.ADVANCEDBAN, "AdvancedBan", s -> new AdvancedBanHook(plugin, s));
        tryEnable(HookTarget.LIBERTYBANS, "LibertyBans", s -> new LibertyBansHook(s));
        tryEnable(HookTarget.LIGHTBANS, "LightBans", s -> new LightBansHook());
    }

    private void tryEnable(HookTarget target, String pluginName, Function<HookSupport, BanHook> factory) {
        if (active.containsKey(target) || configs.config().hookMode(target) != HookMode.API) {
            return;
        }
        Plugin other = Bukkit.getPluginManager().getPlugin(pluginName);
        if (other == null || !other.isEnabled()) {
            return;
        }
        BanHook hook = null;
        try {
            // Created here, not earlier: the hook classes reference the ban plugin's API.
            hook = factory.apply(support);
            hook.enable();
            active.put(target, hook);
            logger.info("Hooked into " + pluginName + " " + other.getDescription().getVersion() + ".");
        } catch (Throwable t) {
            if (hook != null) {
                hook.disable();
            }
            logger.log(Level.WARNING, "Could not hook into " + pluginName + ". Its bans are detected by command and kick instead.",
                    configs.config().debug() ? t : null);
            if (!configs.config().debug()) {
                logger.warning("Reason: " + t);
            }
        }
    }

    /** The hooks that are listening right now, e.g. for {@code /mcpvpban info}. */
    public synchronized List<String> activeHooks() {
        List<String> out = new ArrayList<>();
        for (BanHook hook : active.values()) {
            out.add(hook.pluginName());
        }
        return Collections.unmodifiableList(out);
    }

    /** True if bans from {@code target} come from a live API hook. */
    public synchronized boolean isActive(HookTarget target) {
        return active.containsKey(target);
    }

    public synchronized void disableAll() {
        for (BanHook hook : active.values()) {
            try {
                hook.disable();
            } catch (Throwable ignored) {
                // Shutting down anyway.
            }
        }
        active.clear();
    }
}
