package com.fleoxxzy.mcpvpBanAnimation.platform;

import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;

/**
 * The set of platform-specific services. Exactly one implementation is loaded at runtime:
 * {@code SpigotBinding} or the Paper/Folia {@code PaperBinding}.
 *
 * <p>Implementations must have a public constructor taking {@code (org.bukkit.plugin.Plugin, Platform)}.
 */
public interface PlatformBinding {

    TaskScheduler scheduler();

    TextAdapter text();

    Teleporter teleporter();

    /** Called on disable. Cancels tasks and releases resources. */
    default void shutdown() {
        scheduler().cancelAll();
    }
}
