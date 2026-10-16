package com.fleoxxzy.mcpvpBanAnimation.platform.spigot;

import com.fleoxxzy.mcpvpBanAnimation.platform.Teleporter;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.CompletableFuture;

/** Synchronous teleport on the main thread. Spigot has no {@code teleportAsync}. */
public final class SpigotTeleporter implements Teleporter {

    private final TaskScheduler scheduler;

    public SpigotTeleporter(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (entity == null || location == null || location.getWorld() == null) {
            result.complete(false);
            return result;
        }
        scheduler.executeFor(entity, () -> {
            try {
                result.complete(entity.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN));
            } catch (Throwable t) {
                result.complete(false);
            }
        }, () -> result.complete(false));
        return result;
    }
}
