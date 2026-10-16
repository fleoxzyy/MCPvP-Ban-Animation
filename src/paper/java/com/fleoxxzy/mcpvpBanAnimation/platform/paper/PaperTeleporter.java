package com.fleoxxzy.mcpvpBanAnimation.platform.paper;

import com.fleoxxzy.mcpvpBanAnimation.platform.Teleporter;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.CompletableFuture;

/**
 * {@code teleportAsync} for Paper and Folia. The call is made from the entity's own thread
 * (Folia requires this), and the destination chunk is loaded asynchronously.
 */
public final class PaperTeleporter implements Teleporter {

    private final TaskScheduler scheduler;

    public PaperTeleporter(TaskScheduler scheduler) {
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
                entity.teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
                        .whenComplete((ok, err) -> result.complete(err == null && Boolean.TRUE.equals(ok)));
            } catch (Throwable t) {
                result.complete(false);
            }
        }, () -> result.complete(false));
        return result;
    }
}
