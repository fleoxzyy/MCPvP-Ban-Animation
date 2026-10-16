package com.fleoxxzy.mcpvpBanAnimation.replica;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationContext;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import com.fleoxxzy.mcpvpBanAnimation.config.ReplicaType;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.logging.Logger;

/**
 * The replica part of an animation: spawns the replica and its label on setup, then squashes and
 * removes the replica on impact. Region thread only.
 */
public final class ReplicaStage {

    private final ReplicaSpec spec;
    private final String labelText;
    private final TextAdapter text;
    private final SkinColors skinColors;
    private final Logger logger;
    private Replica replica;
    private BanLabel label;

    /** @param labelText the label MiniMessage with placeholders already filled in */
    public ReplicaStage(ReplicaSpec spec, String labelText, TextAdapter text, SkinColors skinColors, Logger logger) {
        this.spec = spec;
        this.labelText = labelText;
        this.text = text;
        this.skinColors = skinColors;
        this.logger = logger;
    }

    public void setup(AnimationContext ctx) {
        PluginConfig.Replica cfg = ctx.config().replica();
        replica = create(cfg);
        if (!replica.spawn(ctx) && replica instanceof MannequinReplica) {
            logger.warning("Spawning the Mannequin replica failed. Using an ArmorStand instead.");
            replica = new ArmorStandReplica(spec, cfg.copyEquipment(), cfg.leatherFallback(), skinColors);
            replica.spawn(ctx);
        }
        label = new BanLabel(cfg.label(), labelText, text);
        label.spawn(ctx);
    }

    public void impact(AnimationContext ctx) {
        if (label != null) {
            label.freeze();
        }
        squash(ctx);
    }

    /** The replica entity, if it is still there. */
    public LivingEntity entity() {
        return replica == null ? null : replica.entity();
    }

    /** True if replicas are Mannequins with this setting on this server, false for ArmorStands. */
    public static boolean usesMannequin(ReplicaType type) {
        return MannequinReplica.supported() && type != ReplicaType.ARMORSTAND;
    }

    private Replica create(PluginConfig.Replica cfg) {
        ReplicaType type = cfg.type();
        boolean mannequin = usesMannequin(type);
        if (type == ReplicaType.MANNEQUIN && !MannequinReplica.supported()) {
            logger.fine("animation.replica is 'mannequin', but this server has no Mannequin. Using an ArmorStand.");
        }
        return mannequin
                ? new MannequinReplica(spec, cfg.copyEquipment(), logger)
                : new ArmorStandReplica(spec, cfg.copyEquipment(), cfg.leatherFallback(), skinColors);
    }

    /**
     * Shrinks the replica into the ground with the scale attribute, while a flattened copy of the
     * player's head is left lying on the ground: the "squashed on Y" look. Both are removed at cleanup.
     */
    private void squash(AnimationContext ctx) {
        LivingEntity entity = entity();
        if (entity == null || !entity.isValid()) {
            return;
        }
        PluginConfig.Replica cfg = ctx.config().replica();
        int ticks = cfg.squashTicks();
        float min = cfg.squashMinHeight();
        if (ScaleAttribute.available()) {
            ctx.every(age -> {
                if (!entity.isValid()) {
                    return false;
                }
                double t = Math.min(1.0, (double) age / ticks);
                ScaleAttribute.set(entity, 1.0 - (1.0 - min) * t);
                if (age >= ticks) {
                    entity.remove();
                    return false;
                }
                return true;
            });
        } else {
            entity.remove();
        }
        spawnSplat(ctx);
    }

    private void spawnSplat(AnimationContext ctx) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (spec.profile() != null && head.getItemMeta() instanceof SkullMeta meta) {
            try {
                meta.setOwnerProfile(spec.profile());
                head.setItemMeta(meta);
            } catch (Throwable ignored) {
                // Default head.
            }
        }
        // The display takes the anchor's yaw, so the head already faces the player's direction.
        ItemDisplay splat = ctx.spawn(ctx.anchor(), ItemDisplay.class, d -> {
            d.setRotation(ctx.anchor().getYaw(), 0f);
            d.setItemStack(head);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            d.setBillboard(Display.Billboard.FIXED);
            d.setViewRange(1.5f);
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(0);
            // Starts as a normal head, then flattens over 3 ticks.
            d.setTransformation(new Transformation(new Vector3f(0, 0.25f, 0), new Quaternionf(),
                    new Vector3f(0.9f, 0.9f, 0.9f), new Quaternionf()));
        });
        ctx.schedule(1, () -> {
            if (splat.isValid()) {
                splat.setInterpolationDelay(0);
                splat.setInterpolationDuration(3);
                splat.setTransformation(new Transformation(new Vector3f(0, 0.03f, 0), new Quaternionf(),
                        new Vector3f(1.25f, 0.07f, 1.25f), new Quaternionf()));
            }
        });
    }
}
