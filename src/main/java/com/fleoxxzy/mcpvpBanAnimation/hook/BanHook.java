package com.fleoxxzy.mcpvpBanAnimation.hook;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;

/** Listens to one ban plugin's own API and reports its bans to the trigger service. */
interface BanHook {

    HookTarget target();

    /** The plugin's name as registered with Bukkit. */
    String pluginName();

    /** Starts listening. Throws if the plugin's API is not what we expect. */
    void enable() throws Exception;

    /** Stops listening. Never throws. */
    void disable();
}
