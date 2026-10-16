package com.fleoxxzy.mcpvpBanAnimation.trigger;

import com.fleoxxzy.mcpvpBanAnimation.replica.ReplicaSpec;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Everything the animation needs from a player, taken on their own thread at the moment of the
 * ban (or when they quit). Immutable, so it can be used after the player is gone.
 *
 * @param anchor  where the replica stands (feet snapped to the ground, facing the player's yaw)
 * @param bypass  the player had {@code mcpvpban.bypass}
 */
public record PlayerSnapshot(UUID uuid, String name, Location anchor, ReplicaSpec spec, boolean bypass,
                             long takenAtMillis) {

    @Override
    public Location anchor() {
        return anchor.clone();
    }
}
