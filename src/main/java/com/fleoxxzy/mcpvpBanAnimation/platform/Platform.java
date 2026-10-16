package com.fleoxxzy.mcpvpBanAnimation.platform;

import com.fleoxxzy.mcpvpBanAnimation.core.Reflect;
import com.fleoxxzy.mcpvpBanAnimation.platform.spigot.SpigotBinding;
import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Detects the platform once, then loads the matching {@link PlatformBinding}.
 *
 * <p>The Paper/Folia binding is loaded by <b>class name</b> only after detection, so a Spigot
 * server never resolves any class that references Adventure or the Folia scheduler API.
 */
public final class Platform {

    private static final String PAPER_BINDING = "com.fleoxxzy.mcpvpBanAnimation.platform.paper.PaperBinding";

    private final PlatformType type;
    private final MinecraftVersion version;
    private final String rawVersion;
    private final int javaVersion;
    private final FeatureFlags features;
    private final PlatformBinding binding;

    private Platform(Plugin plugin) {
        this.type = detectType();
        this.rawVersion = readRawVersion(type);
        this.version = MinecraftVersion.parse(rawVersion).orElse(MinecraftVersion.UNKNOWN);
        this.javaVersion = Runtime.version().feature();
        this.features = FeatureFlags.detect();
        this.binding = loadBinding(plugin);
    }

    public static Platform initialize(Plugin plugin) {
        return new Platform(plugin);
    }

    // ---------------------------------------------------------------------------------------------

    static PlatformType detectType() {
        if (Reflect.hasClass("io.papermc.paper.threadedregions.RegionizedServer")) {
            return PlatformType.FOLIA;
        }
        if (Reflect.hasClass("io.papermc.paper.configuration.Configuration")
                || Reflect.hasClass("com.destroystokyo.paper.PaperConfig")) {
            return PlatformType.PAPER;
        }
        return PlatformType.SPIGOT;
    }

    private static String readRawVersion(PlatformType type) {
        if (type.isPaperBased()) {
            // Server#getMinecraftVersion exists on Paper only. Call it reflectively so this class stays Spigot-safe.
            Object mc = Reflect.invokeNoArgs(Bukkit.getServer(), "getMinecraftVersion");
            if (mc instanceof String s && MinecraftVersion.parse(s).isPresent()) {
                return s;
            }
        }
        return Bukkit.getBukkitVersion();
    }

    private PlatformBinding loadBinding(Plugin plugin) {
        if (type.isPaperBased() && features.hasRegionScheduler() && features.hasAdventure()) {
            try {
                Class<?> cls = Class.forName(PAPER_BINDING, true, Platform.class.getClassLoader());
                return (PlatformBinding) cls.getConstructor(Plugin.class, Platform.class).newInstance(plugin, this);
            } catch (Throwable t) {
                if (type == PlatformType.FOLIA) {
                    // Spigot's BukkitScheduler throws on Folia. There is no safe fallback here.
                    throw new IllegalStateException("Failed to load the Folia platform binding", t);
                }
                plugin.getLogger().log(Level.WARNING,
                        "Failed to load the Paper platform binding. Falling back to the Spigot implementation.", t);
            }
        } else if (type == PlatformType.FOLIA) {
            throw new IllegalStateException("Folia detected, but its scheduler API is missing. Unsupported server build.");
        }
        return new SpigotBinding(plugin, this);
    }

    // ---------------------------------------------------------------------------------------------

    public PlatformType type() {
        return type;
    }

    public boolean isFolia() {
        return type == PlatformType.FOLIA;
    }

    public boolean isPaperBased() {
        return type.isPaperBased();
    }

    /** True if the Paper/Folia binding is active (false if Paper fell back to Spigot code). */
    public boolean usesPaperBinding() {
        return !(binding instanceof SpigotBinding);
    }

    public MinecraftVersion version() {
        return version;
    }

    public String rawVersion() {
        return rawVersion;
    }

    public int javaVersion() {
        return javaVersion;
    }

    public FeatureFlags features() {
        return features;
    }

    public PlatformBinding binding() {
        return binding;
    }

    public TaskScheduler scheduler() {
        return binding.scheduler();
    }

    public TextAdapter text() {
        return binding.text();
    }

    public Teleporter teleporter() {
        return binding.teleporter();
    }

    public String describe() {
        return type.displayName() + " " + version + " (" + rawVersion + "), Java " + javaVersion
                + ", binding=" + (usesPaperBinding() ? "paper" : "spigot");
    }
}
