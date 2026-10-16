package com.fleoxxzy.mcpvpBanAnimation.animation;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

/**
 * The bone burst, with simple physics of our own: gravity, air drag, block collision with bouncing
 * and ground friction, tumbling spin, and a shrink-out fade.
 *
 * <p>Like the mace, each bone entity stays at the anchor and only its translation changes, so every
 * bone stays inside the anchor's region on Folia. Block collision only reads blocks the current
 * thread owns. Elsewhere the anchor's ground level is used as the floor.
 */
final class BoneBurst {

    /** Bones never fly further than this from the anchor (keeps them inside the region on Folia). */
    private static final double MAX_RADIUS = 14.0;
    private static final double MAX_HEIGHT = 24.0;
    private static final double REST_SPEED = 0.02;

    private BoneBurst() {
    }

    static void launch(AnimationContext ctx) {
        PluginConfig.Bones cfg = ctx.config().bones();
        Random random = ctx.random();
        int count = cfg.countMin() + (cfg.countMax() > cfg.countMin() ? random.nextInt(cfg.countMax() - cfg.countMin() + 1) : 0);
        Location origin = ctx.anchor().add(0, 0.6, 0);
        for (int i = 0; i < count + cfg.skulls(); i++) {
            boolean skull = i >= count;
            Bone bone = new Bone(ctx, cfg, origin, skull, random);
            if (bone.spawn()) {
                ctx.every(bone::tick);
            }
        }
    }

    private static final class Bone {
        private final AnimationContext ctx;
        private final PluginConfig.Bones cfg;
        private final PluginConfig.BonePhysics phys;
        private final boolean skull;
        private final World world;
        private final Location anchor;
        private final double groundFallback;
        private final float baseScale;
        private final Quaternionf rotation = new Quaternionf();
        private final Vector3f spinAxis;
        private float spinSpeed;

        private double x;
        private double y;
        private double z;
        private double vx;
        private double vy;
        private double vz;
        private boolean resting;
        private ItemDisplay display;

        Bone(AnimationContext ctx, PluginConfig.Bones cfg, Location origin, boolean skull, Random r) {
            this.ctx = ctx;
            this.cfg = cfg;
            this.phys = cfg.physics();
            this.skull = skull;
            this.world = ctx.world();
            this.anchor = ctx.anchor();
            this.groundFallback = anchor.getY();
            this.baseScale = cfg.scale() * (skull ? 1.15f : 0.8f + r.nextFloat() * 0.4f);

            x = origin.getX() + (r.nextDouble() - 0.5) * 0.5;
            y = origin.getY() + r.nextDouble() * 0.6;
            z = origin.getZ() + (r.nextDouble() - 0.5) * 0.5;

            double angle = r.nextDouble() * Math.PI * 2;
            double speed = lerp(phys.speedMin(), phys.speedMax(), r.nextDouble());
            double up = lerp(phys.upwardMin(), phys.upwardMax(), r.nextDouble());
            if (r.nextDouble() < phys.highFlyChance()) {
                up *= phys.highFlyMultiplier();
                speed *= 0.6;
            }
            vx = Math.cos(angle) * speed;
            vz = Math.sin(angle) * speed;
            vy = up;

            spinAxis = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
            if (spinAxis.lengthSquared() < 1e-4f) {
                spinAxis.set(0, 1, 0);
            }
            spinAxis.normalize();
            spinSpeed = (float) Math.toRadians(phys.spin() * (0.4 + r.nextDouble() * 0.6));
            rotation.rotateY(r.nextFloat() * (float) Math.PI * 2);
        }

        boolean spawn() {
            ItemStack item = new ItemStack(skull ? Material.SKELETON_SKULL : Material.BONE);
            display = ctx.spawn(anchor, ItemDisplay.class, d -> {
                d.setItemStack(item);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setBillboard(Display.Billboard.FIXED);
                d.setViewRange(1.5f);
                Displays.pose(d, translation(), rotation, Displays.uniform(baseScale), 0);
            });
            return display != null;
        }

        boolean tick(int age) {
            if (display == null || !display.isValid()) {
                return false;
            }
            if (age >= cfg.lifetime()) {
                display.remove();
                return false;
            }
            if (!resting) {
                integrate();
                rotation.rotateAxis(spinSpeed, spinAxis.x, spinAxis.y, spinAxis.z);
            }
            int fadeStart = cfg.lifetime() - cfg.fadeTicks();
            float scale = baseScale;
            if (cfg.fadeTicks() > 0 && age > fadeStart) {
                scale *= Math.max(0.01f, 1f - (float) (age - fadeStart) / cfg.fadeTicks());
            }
            // Resting bones only need an update while they fade.
            if (!resting || age > fadeStart) {
                Displays.pose(display, translation(), rotation, Displays.uniform(scale), 1);
            }
            return true;
        }

        private void integrate() {
            vy -= phys.gravity();
            vx *= phys.drag();
            vy *= phys.drag();
            vz *= phys.drag();

            double nx = clampAround(x + vx, anchor.getX());
            double nz = clampAround(z + vz, anchor.getZ());
            double ny = Math.min(y + vy, anchor.getY() + MAX_HEIGHT);

            // Horizontal collision: bounce off walls.
            if (isSolid(nx, y, z)) {
                vx = -vx * phys.bounce();
                nx = x;
            }
            if (isSolid(nx, y, nz)) {
                vz = -vz * phys.bounce();
                nz = z;
            }

            // Vertical collision.
            double floor = floorBelow(nx, y, nz);
            if (ny <= floor && vy < 0) {
                ny = floor;
                vy = -vy * phys.bounce();
                vx *= phys.groundFriction();
                vz *= phys.groundFriction();
                // Slow down the tumbling on every bounce.
                spinSpeed *= 0.6f;
                if (Math.abs(vy) < 0.06) {
                    vy = 0;
                }
                if (vy == 0 && Math.abs(vx) < REST_SPEED && Math.abs(vz) < REST_SPEED) {
                    resting = true;
                    // Lay the bone flat on the ground.
                    float flatYaw = ctx.random().nextFloat() * (float) Math.PI * 2;
                    rotation.identity().rotateY(flatYaw).rotateX((float) Math.toRadians(90));
                    ny = floor + 0.03;
                }
            } else if (vy > 0 && isSolid(nx, ny + 0.2, nz)) {
                vy = -vy * phys.bounce() * 0.5;
                ny = y;
            }

            x = nx;
            y = ny;
            z = nz;
        }

        /** Top surface of the first solid block at or below {@code fromY}, plus half the item's thickness. */
        private double floorBelow(double bx, double fromY, double bz) {
            double rest = 0.06 * baseScale;
            Location probe = new Location(world, bx, fromY, bz);
            if (!ctx.platform().scheduler().isOwnedByCurrentThread(probe)) {
                return groundFallback + rest;
            }
            int startY = (int) Math.floor(fromY);
            int minY = Math.max(world.getMinHeight(), startY - 24);
            for (int by = startY; by >= minY; by--) {
                Block block = world.getBlockAt((int) Math.floor(bx), by, (int) Math.floor(bz));
                if (!block.isPassable()) {
                    BoundingBox box = block.getBoundingBox();
                    double top = box.getVolume() > 0 ? box.getMaxY() : by + 1.0;
                    return top + rest;
                }
            }
            return groundFallback + rest;
        }

        private boolean isSolid(double bx, double by, double bz) {
            Location probe = new Location(world, bx, by, bz);
            if (!ctx.platform().scheduler().isOwnedByCurrentThread(probe)) {
                return false;
            }
            Block block = world.getBlockAt((int) Math.floor(bx), (int) Math.floor(by), (int) Math.floor(bz));
            if (block.isPassable()) {
                return false;
            }
            BoundingBox box = block.getBoundingBox();
            // Partial blocks (slabs, carpets): solid only if we are inside their box.
            return box.getVolume() <= 0 || by < box.getMaxY();
        }

        private Vector3f translation() {
            return new Vector3f((float) (x - anchor.getX()), (float) (y - anchor.getY()), (float) (z - anchor.getZ()));
        }

        private static double clampAround(double v, double center) {
            return Math.max(center - MAX_RADIUS, Math.min(center + MAX_RADIUS, v));
        }

        private static double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
    }
}
