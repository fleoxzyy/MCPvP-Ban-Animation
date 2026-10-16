package com.fleoxxzy.mcpvpBanAnimation.replica;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationContext;
import org.bukkit.entity.LivingEntity;

/** The frozen copy of the banned player, standing at the anchor. */
public interface Replica {

    /** Spawns the entity at the anchor. Returns false if spawning failed. */
    boolean spawn(AnimationContext ctx);

    /** The spawned entity, or null. */
    LivingEntity entity();

    /** Eye height above the anchor (used to place the label and aim the camera). */
    double height();
}
