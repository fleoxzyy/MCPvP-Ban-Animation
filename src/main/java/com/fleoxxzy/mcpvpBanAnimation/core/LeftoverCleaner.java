package com.fleoxxzy.mcpvpBanAnimation.core;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationManager;
import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.List;
import java.util.logging.Logger;

/**
 * Removes tagged leftovers (from a crash or a hard stop) when their chunk's entities load, and
 * scans loaded worlds on enable where that is allowed.
 *
 * <p>On Folia there is no safe global entity scan. Cleanup happens chunk by chunk through
 * {@link EntitiesLoadEvent}, which fires on the thread that owns that chunk. Spawned entities are
 * also non-persistent, so they are normally never saved in the first place.
 */
public final class LeftoverCleaner implements Listener {

    private final Platform platform;
    private final AnimationManager animations;
    private final EntityTags tags;
    private final Logger logger;

    public LeftoverCleaner(Platform platform, AnimationManager animations, Logger logger) {
        this.platform = platform;
        this.animations = animations;
        this.tags = animations.tags();
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        int removed = removeLeftovers(event.getEntities());
        if (removed > 0) {
            Chunk chunk = event.getChunk();
            logger.info("Removed " + removed + " leftover animation entities in chunk " + chunk.getX() + "," + chunk.getZ() + ".");
        }
    }

    /** Full scan of every loaded world. Main thread only, so Spigot/Paper only. */
    public void scanLoadedWorlds() {
        if (platform.isFolia()) {
            return;
        }
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            removed += removeLeftovers(world.getEntities());
        }
        if (removed > 0) {
            logger.info("Removed " + removed + " leftover animation entities.");
        }
    }

    private int removeLeftovers(List<Entity> entities) {
        int removed = 0;
        for (Entity entity : entities) {
            if (tags.isTagged(entity) && !animations.isLive(entity.getUniqueId())) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }
}
