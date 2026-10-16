package com.fleoxxzy.mcpvpBanAnimation.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;

/**
 * Moves entities in a platform-safe way: a synchronous teleport on Spigot, {@code teleportAsync}
 * on the entity's own thread on Paper/Folia.
 */
public interface Teleporter {

    /**
     * Teleports {@code entity}. Safe to call from any thread: the implementation hops to the
     * correct thread itself. The future completes with {@code false} if the entity is gone or
     * the teleport failed. It never completes exceptionally.
     */
    CompletableFuture<Boolean> teleport(Entity entity, Location location);
}
