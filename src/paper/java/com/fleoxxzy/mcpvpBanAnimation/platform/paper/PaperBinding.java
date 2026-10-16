package com.fleoxxzy.mcpvpBanAnimation.platform.paper;

import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import com.fleoxxzy.mcpvpBanAnimation.platform.PlatformBinding;
import com.fleoxxzy.mcpvpBanAnimation.platform.Teleporter;
import com.fleoxxzy.mcpvpBanAnimation.platform.folia.FoliaScheduler;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import org.bukkit.plugin.Plugin;

/**
 * Paper + Folia binding: region/entity schedulers, Adventure text, async teleports.
 * Loaded reflectively by {@link Platform} only after detection.
 */
public final class PaperBinding implements PlatformBinding {

    private final FoliaScheduler scheduler;
    private final PaperText text;
    private final PaperTeleporter teleporter;

    public PaperBinding(Plugin plugin, Platform platform) {
        this.scheduler = new FoliaScheduler(plugin, platform.isFolia());
        this.text = new PaperText();
        this.teleporter = new PaperTeleporter(scheduler);
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
