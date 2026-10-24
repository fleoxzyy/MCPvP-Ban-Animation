package com.fleoxxzy.mcpvpBanAnimation.animation;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import com.fleoxxzy.mcpvpBanAnimation.core.EntityTags;
import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskHandle;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * One running ban animation. It is purely visual: the ban itself has already happened.
 * All world work runs on the anchor's region thread, driven by a one-tick region timer.
 * Any failure only ends the visuals early and cleans up.
 */
public final class BanAnimation implements AnimationContext {

    private final Plugin plugin;
    private final Platform platform;
    private final AnimationManager manager;
    private final EntityTags tags;
    private final Effects effects;
    private final PluginConfig config;
    private final UUID key;
    private final Location anchor;
    private final World world;
    private final int chunkX;
    private final int chunkZ;
    private final AnimationCallbacks callbacks;
    private final Random random = new Random();
    private final Timeline timeline = new Timeline();
    private final List<Entity> spawned = new ArrayList<>();
    private final AtomicBoolean finished = new AtomicBoolean();

    private volatile TaskHandle timer = TaskHandle.NONE;
    private volatile TaskHandle timeout = TaskHandle.NONE;
    private boolean chunkTicket;
    private boolean cleanedUp;
    private FallingMace mace;

    BanAnimation(Plugin plugin, Platform platform, AnimationManager manager, EntityTags tags, Effects effects,
                 PluginConfig config, UUID key, Location anchor, AnimationCallbacks callbacks) {
        this.plugin = plugin;
        this.platform = platform;
        this.manager = manager;
        this.tags = tags;
        this.effects = effects;
        this.config = config;
        this.key = key;
        this.anchor = anchor.clone();
        this.world = anchor.getWorld();
        this.chunkX = anchor.getBlockX() >> 4;
        this.chunkZ = anchor.getBlockZ() >> 4;
        this.callbacks = callbacks;
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------------

    void start() {
        PluginConfig.Animation a = config.animation();
        int impact = a.impactTick();
        int cleanup = impact + a.cleanupDelayAfterImpact();

        timeline.at(1, this::setup);
        timeline.at(a.maceSpawnTick(), this::spawnMace);
        timeline.at(impact, this::impact);
        timeline.at(cleanup, () -> end(EndCause.COMPLETED));

        long hardTimeout = Math.max(a.hardTimeoutTicks(), cleanup + 40L);
        timeout = platform.scheduler().runGlobalLater(this::onTimeout, hardTimeout);
        timer = platform.scheduler().runTimerAt(anchor, this::tick, 1L, 1L);
        if (timer == TaskHandle.NONE) {
            plugin.getLogger().warning("Could not schedule the ban animation at " + describeAnchor() + ". Skipping it.");
            finish(EndCause.ABORTED);
        }
    }

    private void tick(TaskHandle self) {
        if (finished.get()) {
            self.cancel();
            return;
        }
        try {
            if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
                abort("the chunk at " + describeAnchor() + " was unloaded", null);
                return;
            }
            timeline.step();
            if (timeline.isIdle()) {
                end(EndCause.COMPLETED);
            }
        } catch (Throwable t) {
            abort("an error occurred", t);
        }
    }

    private void setup() {
        try {
            chunkTicket = world.addPluginChunkTicket(chunkX, chunkZ, plugin);
        } catch (Throwable t) {
            chunkTicket = false;
        }
        callbacks.onSetup(this);
    }

    private void spawnMace() {
        mace = new FallingMace(this, config.animation().maceFallTicks());
        mace.spawn();
    }

    private void impact() {
        ImpactEffects.play(this);
        BoneBurst.launch(this);
        if (mace != null) {
            mace.settle(config.animation().cleanupDelayAfterImpact());
        }
        callbacks.onImpact(this);
    }

    /** Ends on the anchor thread: remove entities, then stop. */
    private void end(EndCause cause) {
        cleanup();
        finish(cause);
    }

    /** Only call this on the anchor thread. */
    void abort(String reason, Throwable error) {
        if (finished.get()) {
            return;
        }
        if (error != null) {
            plugin.getLogger().log(Level.SEVERE, "Ban animation stopped (" + reason + ").", error);
        } else {
            plugin.getLogger().warning("Ban animation stopped (" + reason + ").");
        }
        end(EndCause.ABORTED);
    }

    /** Runs on the global thread. */
    private void onTimeout() {
        if (finished.get()) {
            return;
        }
        plugin.getLogger().warning("Ban animation at " + describeAnchor() + " hit the hard timeout. Cleaning up.");
        platform.scheduler().runAt(anchor, () -> end(EndCause.TIMEOUT));
        // If the region task cannot run (e.g. its chunk is gone), still free the slot.
        manager.remove(key, this);
    }

    /**
     * Plugin shutdown. Clean up only if we may touch the anchor's region from here (always on
     * Spigot/Paper, rarely on Folia). The entities are non-persistent either way, so they are never saved.
     */
    void shutdown() {
        if (platform.scheduler().isOwnedByCurrentThread(anchor)) {
            cleanup();
        }
        finish(EndCause.SHUTDOWN);
    }

    private void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        timeline.clear();
        for (Entity entity : spawned) {
            try {
                if (platform.scheduler().isOwnedByCurrentThread(entity)) {
                    entity.remove();
                } else {
                    // Pushed into another region (Folia): remove it on its own thread.
                    platform.scheduler().runFor(entity, entity::remove, null);
                }
            } catch (Throwable ignored) {
                // Already gone, or not ours to touch any more.
            } finally {
                manager.untrack(entity.getUniqueId());
            }
        }
        spawned.clear();
        if (chunkTicket) {
            try {
                world.removePluginChunkTicket(chunkX, chunkZ, plugin);
            } catch (Throwable ignored) {
                // World unloaded.
            }
            chunkTicket = false;
        }
    }

    /** Stops the timers, frees the slot and notifies the callbacks. Exactly once, from any thread. */
    private void finish(EndCause cause) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        timer.cancel();
        timeout.cancel();
        manager.remove(key, this);
        try {
            callbacks.onFinish(cause);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Error in animation finish callback", t);
        }
    }

    public boolean isFinished() {
        return finished.get();
    }

    public UUID key() {
        return key;
    }

    private String describeAnchor() {
        return (world == null ? "?" : world.getName()) + " " + anchor.getBlockX() + "," + anchor.getBlockY() + "," + anchor.getBlockZ();
    }

    // ---------------------------------------------------------------------------------------------
    // AnimationContext
    // ---------------------------------------------------------------------------------------------

    @Override
    public Location anchor() {
        return anchor.clone();
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public PluginConfig config() {
        return config;
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public Effects effects() {
        return effects;
    }

    @Override
    public Random random() {
        return random;
    }

    @Override
    public int tick() {
        return timeline.currentTick();
    }

    @Override
    public <T extends Entity> T spawn(Location at, Class<T> type, Consumer<? super T> setup) {
        T entity = world.spawn(at, type, e -> {
            tags.tag(e);
            if (setup != null) {
                setup.accept(e);
            }
        });
        spawned.add(entity);
        manager.track(entity.getUniqueId());
        return entity;
    }

    @Override
    public void schedule(int delay, Runnable action) {
        timeline.at(timeline.currentTick() + Math.max(0, delay), action);
    }

    @Override
    public void every(Ticker ticker) {
        timeline.add(ticker);
    }
}
