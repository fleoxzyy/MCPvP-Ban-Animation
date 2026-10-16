package com.fleoxxzy.mcpvpBanAnimation.platform.spigot;

import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import com.fleoxxzy.mcpvpBanAnimation.platform.PlatformBinding;
import com.fleoxxzy.mcpvpBanAnimation.platform.Teleporter;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import org.bukkit.plugin.Plugin;

/** Plain Spigot: BukkitScheduler, legacy/Bungee text, synchronous teleports. */
public final class SpigotBinding implements PlatformBinding {

    private final SpigotScheduler scheduler;
    private final SpigotText text;
    private final SpigotTeleporter teleporter;

    public SpigotBinding(Plugin plugin, Platform platform) {
        this.scheduler = new SpigotScheduler(plugin);
        this.text = new SpigotText();
        this.teleporter = new SpigotTeleporter(scheduler);
    }

    @Override
    public TaskScheduler scheduler() {
        return scheduler;
    }

    @Override
    public TextAdapter text() {
        return text;
    }

    @Override
    public Teleporter teleporter() {
        return teleporter;
    }
}
