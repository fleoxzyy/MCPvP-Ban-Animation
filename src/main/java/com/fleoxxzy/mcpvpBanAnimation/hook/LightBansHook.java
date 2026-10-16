package com.fleoxxzy.mcpvpBanAnimation.hook;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;

/**
 * LightBans has no public event API yet, so this hook does nothing. Its bans are caught by the
 * command and kick detection instead ({@code hooks.lightbans: COMMAND}). It is here so a real
 * listener can be dropped in once LightBans offers one.
 */
final class LightBansHook implements BanHook {

    @Override
    public HookTarget target() {
        return HookTarget.LIGHTBANS;
    }

    @Override
    public String pluginName() {
        return "LightBans";
    }

    @Override
    public void enable() {
        throw new UnsupportedOperationException("LightBans has no event API. Use hooks.lightbans: COMMAND.");
    }

    @Override
    public void disable() {
    }
}
