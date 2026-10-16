package com.fleoxxzy.mcpvpBanAnimation.core;

import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

/** Ground lookups that only read blocks the current thread is allowed to touch. */
public final class Ground {

    private Ground() {
    }

    /**
     * Moves {@code location} down onto the first solid surface within {@code maxDepth} blocks
     * (or up, if it is inside a block). Returns a copy. If the current thread does not own the
     * location (Folia), or no ground is found, the location is returned unchanged.
     */
    public static Location snap(Location location, TaskScheduler scheduler, int maxDepth) {
        Location out = location.clone();
        World world = out.getWorld();
        if (world == null || !scheduler.isOwnedByCurrentThread(out)) {
            return out;
        }
        int bx = out.getBlockX();
        int bz = out.getBlockZ();
        int startY = out.getBlockY();

        // Inside a block: step up until the space is free.
        for (int up = 0; up <= 3; up++) {
            Block block = world.getBlockAt(bx, startY + up, bz);
            if (block.isPassable()) {
                if (up > 0) {
                    out.setY(startY + up);
                }
                break;
            }
        }
        int fromY = out.getBlockY();
        int minY = Math.max(world.getMinHeight(), fromY - maxDepth);
        for (int y = fromY; y >= minY; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (!block.isPassable()) {
                BoundingBox box = block.getBoundingBox();
                double top = box.getVolume() > 0 ? box.getMaxY() : y + 1.0;
                if (top <= out.getY() + 1e-6 || y < fromY) {
                    out.setY(top);
                    return out;
                }
            }
        }
        return out;
    }
}
