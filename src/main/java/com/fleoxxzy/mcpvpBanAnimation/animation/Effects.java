package com.fleoxxzy.mcpvpBanAnimation.animation;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Plays sounds and particles by namespaced key ({@code minecraft:...}), so renamed or removed
 * enum constants never break the plugin across versions. Missing or unsupported particles are
 * skipped and logged once.
 */
public final class Effects {

    private final Logger logger;
    private final Map<String, Optional<Particle>> particles = new ConcurrentHashMap<>();
    private final Map<String, Boolean> warned = new ConcurrentHashMap<>();

    public Effects(Logger logger) {
        this.logger = logger;
    }

    /**
     * Plays a sound by key. Sending the key as a string makes the client resolve it, so no Sound
     * enum or registry lookup is needed and an unknown key is simply silent.
     */
    public void sound(Location at, PluginConfig.SoundSpec spec) {
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        try {
            world.playSound(at, spec.key(), SoundCategory.PLAYERS, spec.volume(), spec.pitch());
        } catch (Throwable t) {
            warnOnce("sound:" + spec.key(), "Could not play sound '" + spec.key() + "': " + t);
        }
    }

    public void burst(Location at, PluginConfig.ParticleSpec spec) {
        spawn(at, spec.key(), spec.count(), spec.offset(), spec.offset(), spec.offset(), spec.speed(),
                spec.data(), spec.color(), spec.size());
    }

    /** A simple data-less particle (or one with default data). Returns false if it is unavailable. */
    public boolean simple(Location at, String key, int count, double dx, double dy, double dz, double speed) {
        return spawn(at, key, count, dx, dy, dz, speed, null, 0xFFFFFF, 1f);
    }

    private boolean spawn(Location at, String key, int count, double dx, double dy, double dz, double speed,
                          String data, int rgb, float size) {
        World world = at.getWorld();
        Particle particle = resolve(key);
        if (world == null || particle == null) {
            return false;
        }
        Object particleData;
        try {
            particleData = buildData(particle, data, rgb, size);
        } catch (Throwable t) {
            warnOnce("data:" + key, "Particle '" + key + "' needs data that could not be built (" + t + "). Skipping it.");
            return false;
        }
        if (particleData == Unsupported.INSTANCE) {
            Class<?> type = dataType(particle);
            warnOnce("type:" + key, "Particle '" + key + "' needs an unsupported data type ("
                    + (type == null ? "?" : type.getSimpleName()) + "). Skipping it.");
            return false;
        }
        try {
            world.spawnParticle(particle, at, count, dx, dy, dz, speed, particleData, true);
            return true;
        } catch (Throwable t) {
            warnOnce("spawn:" + key, "Could not spawn particle '" + key + "': " + t);
            return false;
        }
    }

    private Particle resolve(String key) {
        return particles.computeIfAbsent(key, k -> {
            Particle found = null;
            try {
                NamespacedKey nk = NamespacedKey.fromString(k.toLowerCase(java.util.Locale.ROOT));
                if (nk != null) {
                    found = Registry.PARTICLE_TYPE.get(nk);
                }
            } catch (Throwable ignored) {
                found = null;
            }
            if (found == null) {
                warnOnce("missing:" + k, "Particle '" + k + "' does not exist on this server version. Skipping it.");
            }
            return Optional.ofNullable(found);
        }).orElse(null);
    }

    private static Object buildData(Particle particle, String data, int rgb, float size) {
        Class<?> type = dataType(particle);
        if (type == null || type == Void.class) {
            return null;
        }
        if (type == Particle.DustOptions.class) {
            return new Particle.DustOptions(Color.fromRGB(rgb & 0xFFFFFF), size);
        }
        if (type == Particle.DustTransition.class) {
            Color c = Color.fromRGB(rgb & 0xFFFFFF);
            return new Particle.DustTransition(c, c, size);
        }
        if (type == BlockData.class) {
            Material material = data == null ? null : Material.matchMaterial(data);
            if (material == null || !material.isBlock()) {
                material = Material.BONE_BLOCK;
            }
            return material.createBlockData();
        }
        if (type == ItemStack.class) {
            Material material = data == null ? null : Material.matchMaterial(data);
            return new ItemStack(material == null || !material.isItem() ? Material.BONE : material);
        }
        if (type == Color.class) {
            return Color.fromRGB(rgb & 0xFFFFFF);
        }
        if (type == Float.class) {
            return 0f;
        }
        if (type == Integer.class) {
            return 0;
        }
        return Unsupported.INSTANCE;
    }

    /**
     * {@code Particle} is an enum in the 1.21 API we compile against, but newer versions may turn it
     * into an interface/class. A direct virtual call would then fail to link, so call it reflectively.
     */
    private static Class<?> dataType(Particle particle) {
        try {
            Method m = GET_DATA_TYPE;
            return m == null ? null : (Class<?>) m.invoke(particle);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final Method GET_DATA_TYPE = findDataTypeMethod();

    private static Method findDataTypeMethod() {
        try {
            return Particle.class.getMethod("getDataType");
        } catch (Throwable t) {
            return null;
        }
    }

    private void warnOnce(String id, String message) {
        if (warned.putIfAbsent(id, Boolean.TRUE) == null) {
            logger.warning(message);
        }
    }

    private enum Unsupported {
        INSTANCE
    }
}
