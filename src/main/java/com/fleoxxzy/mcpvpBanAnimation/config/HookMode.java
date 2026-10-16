package com.fleoxxzy.mcpvpBanAnimation.config;

import java.util.Locale;

/** How a ban plugin's bans are detected. */
public enum HookMode {
    /** Intercept the plugin's ban commands. */
    COMMAND,
    /** Only catch the ban kick. */
    KICK,
    /** Use the plugin's own event API. */
    API,
    /** Never animate this plugin's bans. */
    OFF;

    public static HookMode parse(String raw, HookMode fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
