package com.fleoxxzy.mcpvpBanAnimation.animation;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The giant mace. The ItemDisplay entity itself never moves: it sits at the anchor, so it always
 * belongs to the anchor's region (Folia forbids synchronous teleports). The fall is animated
 * entirely through the display's translation with client-side interpolation.
 */
final class FallingMace {

    /** In the FIXED item transform the item points to the top-right. -135° around Z points the head down. */
    private static final float HEAD_DOWN = (float) Math.toRadians(-135);

    private final AnimationContext ctx;
    private final PluginConfig.Mace cfg;
    private final int fallTicks;
    private ItemDisplay display;
    private float yaw;

    FallingMace(AnimationContext ctx, int fallTicks) {
        this.ctx = ctx;
        this.cfg = ctx.config().mace();
        this.fallTicks = fallTicks;
    }

    void spawn() {
        ItemStack item = createItem();
        float facing = ctx.anchor().getYaw();
        // The display entity takes the player's yaw, so every rotation below is relative to where
        // they were facing. The spin unwinds during the fall and ends exactly on that direction.
        this.yaw = (float) -cfg.spinDegrees();
        display = ctx.spawn(ctx.anchor(), ItemDisplay.class, d -> {
            d.setRotation(facing, 0f);
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.FIXED);
            d.setViewRange(2.5f);
            d.setShadowRadius(0.6f);
            d.setShadowStrength(0.4f);
            Displays.pose(d, offset(cfg.height()), rotation(yaw), Displays.uniform(cfg.scale()), 0);
        });
        ctx.every(this::fallTick);
    }

    private ItemStack createItem() {
        ItemStack item = new ItemStack(resolveMaterial(cfg.material()));
        if (cfg.enchanted()) {
            try {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setEnchantmentGlintOverride(true);
                    item.setItemMeta(meta);
                }
            } catch (Throwable ignored) {
                // No glint override on this server: the mace just won't shimmer.
            }
        }
        return item;
    }

    private boolean fallTick(int age) {
        if (display == null || !display.isValid()) {
            return false;
        }
        double t = Math.min(1.0, (double) age / fallTicks);
        double y = cfg.impactHeight() + (cfg.height() - cfg.impactHeight()) * (1.0 - Displays.easeInFall(t));
        yaw = (float) (-cfg.spinDegrees() * (1.0 - t));
        Displays.pose(display, offset(y), rotation(yaw), Displays.uniform(cfg.scale()), 1);

        if (cfg.trailEnabled() && cfg.trailCount() > 0 && age < fallTicks) {
            Location at = ctx.anchor().add(0, y + 0.4 * cfg.scale(), 0);
            for (String key : cfg.trailParticles()) {
                ctx.effects().simple(at, key, cfg.trailCount(), 0.12, 0.25, 0.12, 0.01);
            }
        }
        return age < fallTicks;
    }

    /** After impact: sink in slightly, then shrink away just before cleanup. */
    void settle(int ticksUntilCleanup) {
        if (display == null || !display.isValid()) {
            return;
        }
        float scale = cfg.scale();
        Displays.pose(display, offset(cfg.impactHeight() - 0.25), rotation(yaw), Displays.uniform(scale), 3);
        int shrink = Math.min(10, Math.max(1, ticksUntilCleanup - 1));
        ctx.schedule(Math.max(1, ticksUntilCleanup - shrink), () -> {
            if (display.isValid()) {
                Displays.pose(display, offset(cfg.impactHeight() - 0.6), rotation(yaw), Displays.uniform(0.01f), shrink);
            }
        });
    }

    private static Vector3f offset(double y) {
        return new Vector3f(0f, (float) y, 0f);
    }

    /** {@code yawDegrees} is relative to the player's facing; {@code mace.yaw-offset} turns the whole mace. */
    private Quaternionf rotation(float yawDegrees) {
        return new Quaternionf().rotateY((float) Math.toRadians(-(yawDegrees + cfg.yawOffset()))).rotateZ(HEAD_DOWN);
    }

    static Material resolveMaterial(String configured) {
        Material m = configured == null ? null : Material.matchMaterial(configured);
        if (m != null && m.isItem() && m != Material.AIR) {
            return m;
        }
        Material mace = Material.matchMaterial("MACE");
        return mace != null ? mace : Material.IRON_AXE;
    }
}
