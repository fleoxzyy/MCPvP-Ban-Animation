package com.fleoxxzy.mcpvpBanAnimation.animation;

import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.core.EntityTags;
import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Tracks running animations (one per banned player UUID). It is thread-safe because on Folia
 * ban events arrive on many different threads.
 */
public final class AnimationManager {

    private final Plugin plugin;
    private final Platform platform;
    private final ConfigManager configs;
    private final EntityTags tags;
    private final Effects effects;
    private final Map<UUID, BanAnimation> active = new ConcurrentHashMap<>();
    /** Entities owned by a live animation. Tagged entities not in here are leftovers. */
    private final Set<UUID> liveEntities = ConcurrentHashMap.newKeySet();

    public AnimationManager(Plugin plugin, Platform platform, ConfigManager configs, EntityTags tags) {
        this.plugin = plugin;
        this.platform = platform;
        this.configs = configs;
        this.tags = tags;
        this.effects = new Effects(plugin.getLogger());
    }

    /**
     * Starts an animation for {@code key} at {@code anchor}. Safe to call from any thread.
     *
     * @return the animation, or empty if one is already running for this key (the caller decides
     *         what to do with the ban; the callbacks are <b>not</b> invoked in that case)
     */
    public Optional<BanAnimation> start(UUID key, Location anchor, AnimationCallbacks callbacks) {
        if (anchor == null || anchor.getWorld() == null) {
            throw new IllegalArgumentException("anchor must have a world");
        }
        BanAnimation animation = new BanAnimation(plugin, platform, this, tags, effects, configs.config(),
                key, anchor, callbacks);
        if (active.putIfAbsent(key, animation) != null) {
            return Optional.empty();
        }
        try {
            animation.start();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start the ban animation. Banning right away.", t);
            animation.shutdown();
        }
        return Optional.of(animation);
    }

    public boolean isAnimating(UUID key) {
        return active.containsKey(key);
    }

    public Optional<BanAnimation> get(UUID key) {
        return Optional.ofNullable(active.get(key));
    }

    public int activeCount() {
        return active.size();
    }

    public EntityTags tags() {
        return tags;
    }

    /** True if the tagged entity belongs to a live animation (and so must not be cleaned up as a leftover). */
    public boolean isLive(UUID entityId) {
        return liveEntities.contains(entityId);
    }

    /** Disable: every held ban is released right now. */
    public void shutdownAll() {
        for (BanAnimation animation : new ArrayList<>(active.values())) {
            try {
                animation.shutdown();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Error while stopping a ban animation", t);
            }
        }
        active.clear();
        liveEntities.clear();
    }

    void remove(UUID key, BanAnimation animation) {
        active.remove(key, animation);
    }

    void track(UUID entityId) {
        liveEntities.add(entityId);
    }

    void untrack(UUID entityId) {
        liveEntities.remove(entityId);
    }
}
