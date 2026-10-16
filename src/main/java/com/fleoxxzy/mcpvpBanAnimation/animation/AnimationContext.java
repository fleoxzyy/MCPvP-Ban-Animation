package com.fleoxxzy.mcpvpBanAnimation.animation;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Random;
import java.util.function.Consumer;

/**
 * What the animation's parts (mace, bones, replica, label) see. Only valid on the anchor's region thread.
 */
public interface AnimationContext {

    /** A copy of the anchor (the replica's feet). */
    Location anchor();

    World world();

    PluginConfig config();

    Platform platform();

    Effects effects();

    Random random();

    /** Ticks since the animation started. */
    int tick();

    /** Spawns a tagged, non-persistent entity that is removed automatically on cleanup. */
    <T extends Entity> T spawn(Location at, Class<T> type, Consumer<? super T> setup);

    /** Runs {@code action} {@code delay} ticks from now. */
    void schedule(int delay, Runnable action);

    /** Runs {@code ticker} every tick until it returns false or the animation ends. */
    void every(Ticker ticker);
}
