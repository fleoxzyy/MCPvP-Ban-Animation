package com.fleoxxzy.mcpvpBanAnimation.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Immutable, validated snapshot of {@code config.yml}. A reload builds a new instance and swaps it
 * in atomically, so tasks on other region threads always see one consistent config.
 */
public record PluginConfig(
        boolean debug,
        Animation animation,
        Kill kill,
        Spectator spectator,
        Replica replica,
        Mace mace,
        Impact impact,
        Bones bones,
        Broadcast broadcast,
        CommandInterception commandInterception,
        KickInterception kickInterception,
        Map<HookTarget, HookMode> hooks,
        boolean testSpectate
) {

    public record Animation(int maceSpawnDelay, int maceFallTicks, int releaseDelayAfterImpact,
                            int cleanupDelayAfterImpact, int hardTimeoutTicks) {
        /** Tick (from start, first tick = 1) at which the mace spawns. */
        public int maceSpawnTick() {
            return Math.max(1, maceSpawnDelay);
        }

        /** Tick at which the mace hits: one tick after its last fall pose, so the client has finished interpolating. */
        public int impactTick() {
            return maceSpawnTick() + maceFallTicks + 1;
        }
    }

    /** {@code deathMessage == null} = hidden. {@link #VANILLA_DEATH_MESSAGE} = keep the default. */
    public record Kill(boolean enabled, boolean dropItems, boolean dropExp, String deathMessage) {
    }

    public static final String VANILLA_DEATH_MESSAGE = "vanilla";

    public record Spectator(boolean enabled, double distance, double height, double sideAngle, int lockInterval) {
    }

    public record Label(String text, double offsetY, float scale, double bobAmplitude, double pulseAmplitude,
                        int period, boolean shadow, int backgroundArgb) {
    }

    public record Replica(ReplicaType type, boolean copyEquipment, boolean leatherFallback, Label label,
                          int squashTicks, float squashMinHeight) {
    }

    public record Mace(String material, double height, float scale, double impactHeight, double spinDegrees,
                       boolean trailEnabled, List<String> trailParticles, int trailCount) {
    }

    public record SoundSpec(String key, float volume, float pitch) {
    }

    /** {@code data} = block key for BLOCK-type particles. {@code color}/{@code size} are for DUST. */
    public record ParticleSpec(String key, int count, double offset, double speed, String data, int color, float size) {
    }

    public record Shockwave(boolean enabled, String particle, double maxRadius, int rings, int ringInterval,
                            int pointsPerRing) {
    }

    public record CameraShake(boolean enabled, double radius, int pulses, int interval) {
    }

    public record Impact(List<SoundSpec> sounds, List<ParticleSpec> particles, Shockwave shockwave,
                         CameraShake cameraShake) {
    }

    public record BonePhysics(double gravity, double drag, double bounce, double groundFriction,
                              double speedMin, double speedMax, double upwardMin, double upwardMax,
                              double highFlyChance, double highFlyMultiplier, float spin) {
    }

    public record Bones(int countMin, int countMax, int skulls, float scale, int lifetime, int fadeTicks,
                        BonePhysics physics) {
    }

    public record Broadcast(boolean enabled, String permission) {
    }

    /** {@code commands} are lower-case labels without the slash, e.g. {@code litebans:ban}. */
    public record CommandInterception(Set<String> commands, boolean respectCommandPermission, boolean skipSilent,
                                      Set<String> silentFlags) {
    }

    public record KickInterception(boolean enabled, List<Pattern> reasonPatterns, boolean checkBanList) {
    }

    /** Ban sources that can each have their own {@link HookMode}. */
    public enum HookTarget {
        VANILLA("vanilla", HookMode.COMMAND),
        ESSENTIALS("essentials", HookMode.COMMAND),
        LITEBANS("litebans", HookMode.API),
        ADVANCEDBAN("advancedban", HookMode.API),
        LIBERTYBANS("libertybans", HookMode.API),
        LIGHTBANS("lightbans", HookMode.COMMAND),
        UNKNOWN("unknown", HookMode.KICK);

        private final String key;
        private final HookMode defaultMode;

        HookTarget(String key, HookMode defaultMode) {
            this.key = key;
            this.defaultMode = defaultMode;
        }

        public String key() {
            return key;
        }

        public HookMode defaultMode() {
            return defaultMode;
        }
    }

    public HookMode hookMode(HookTarget target) {
        return hooks.getOrDefault(target, target.defaultMode());
    }

    // ---------------------------------------------------------------------------------------------
    // Parsing
    // ---------------------------------------------------------------------------------------------

    public static PluginConfig from(FileConfiguration c, Logger log) {
        Animation animation = new Animation(
                clampInt(c.getInt("animation.mace-spawn-delay"), 0, 200),
                clampInt(c.getInt("animation.mace-fall-ticks"), 2, 200),
                clampInt(c.getInt("animation.release-delay-after-impact"), 0, 400),
                clampInt(c.getInt("animation.cleanup-delay-after-impact"), 1, 600),
                clampInt(c.getInt("animation.hard-timeout-ticks"), 40, 6000));

        String deathMessage = c.getString("kill.death-message", "");
        Kill kill = new Kill(
                c.getBoolean("kill.enabled"),
                c.getBoolean("kill.drop-items"),
                c.getBoolean("kill.drop-exp"),
                deathMessage == null || deathMessage.isEmpty() ? null : deathMessage);

        Spectator spectator = new Spectator(
                c.getBoolean("spectator.enabled"),
                clamp(c.getDouble("spectator.distance"), 1.5, 20),
                clamp(c.getDouble("spectator.height"), -5, 20),
                clamp(c.getDouble("spectator.side-angle"), -180, 180),
                clampInt(c.getInt("spectator.lock-interval"), 1, 20));

        Label label = new Label(
                c.getString("replica.label.text", "<red>● <dark_red><bold>{username} BANNED</bold> <red>●"),
                clamp(c.getDouble("replica.label.offset-y"), 0, 10),
                (float) clamp(c.getDouble("replica.label.scale"), 0.1, 10),
                clamp(c.getDouble("replica.label.bob-amplitude"), 0, 2),
                clamp(c.getDouble("replica.label.pulse-amplitude"), 0, 1),
                clampInt(c.getInt("replica.label.period"), 2, 400),
                c.getBoolean("replica.label.shadow"),
                parseArgb(c.getString("replica.label.background", "0"), log));

        Replica replica = new Replica(
                ReplicaType.parse(c.getString("replica.type")),
                c.getBoolean("replica.copy-equipment"),
                c.getBoolean("replica.leather-fallback"),
                label,
                clampInt(c.getInt("replica.squash.ticks"), 1, 40),
                (float) clamp(c.getDouble("replica.squash.min-height-scale"), 0.01, 1));

        Mace mace = new Mace(
                c.getString("mace.material", "MACE"),
                clamp(c.getDouble("mace.height"), 2, 64),
                (float) clamp(c.getDouble("mace.scale"), 0.1, 10),
                clamp(c.getDouble("mace.impact-height"), -2, 10),
                c.getDouble("mace.spin-degrees"),
                c.getBoolean("mace.trail.enabled"),
                List.copyOf(c.getStringList("mace.trail.particles")),
                clampInt(c.getInt("mace.trail.count"), 0, 50));

        Impact impact = new Impact(
                parseSounds(c.getMapList("impact.sounds")),
                parseParticles(c.getMapList("impact.particles"), log),
                new Shockwave(
                        c.getBoolean("impact.shockwave.enabled"),
                        c.getString("impact.shockwave.particle", "minecraft:cloud"),
                        clamp(c.getDouble("impact.shockwave.max-radius"), 0.5, 32),
                        clampInt(c.getInt("impact.shockwave.rings"), 1, 40),
                        clampInt(c.getInt("impact.shockwave.ring-interval"), 1, 20),
                        clampInt(c.getInt("impact.shockwave.points-per-ring"), 4, 200)),
                new CameraShake(
                        c.getBoolean("impact.camera-shake.enabled"),
                        clamp(c.getDouble("impact.camera-shake.radius"), 0, 128),
                        clampInt(c.getInt("impact.camera-shake.pulses"), 1, 20),
                        clampInt(c.getInt("impact.camera-shake.interval"), 1, 20)));

        int countMin = clampInt(c.getInt("bones.count-min"), 0, 200);
        int countMax = Math.max(countMin, clampInt(c.getInt("bones.count-max"), 0, 200));
        double speedMin = clamp(c.getDouble("bones.physics.speed-min"), 0, 5);
        double upwardMin = clamp(c.getDouble("bones.physics.upward-min"), 0, 5);
        int lifetime = clampInt(c.getInt("bones.lifetime"), 1, 1200);
        Bones bones = new Bones(
                countMin,
                countMax,
                clampInt(c.getInt("bones.skulls"), 0, 50),
                (float) clamp(c.getDouble("bones.scale"), 0.05, 5),
                lifetime,
                clampInt(c.getInt("bones.fade-ticks"), 0, lifetime),
                new BonePhysics(
                        clamp(c.getDouble("bones.physics.gravity"), 0, 1),
                        clamp(c.getDouble("bones.physics.drag"), 0.5, 1),
                        clamp(c.getDouble("bones.physics.bounce"), 0, 1),
                        clamp(c.getDouble("bones.physics.ground-friction"), 0, 1),
                        speedMin,
                        Math.max(speedMin, clamp(c.getDouble("bones.physics.speed-max"), 0, 5)),
                        upwardMin,
                        Math.max(upwardMin, clamp(c.getDouble("bones.physics.upward-max"), 0, 5)),
                        clamp(c.getDouble("bones.physics.high-fly-chance"), 0, 1),
                        clamp(c.getDouble("bones.physics.high-fly-multiplier"), 1, 5),
                        (float) clamp(c.getDouble("bones.physics.spin"), 0, 180)));

        Broadcast broadcast = new Broadcast(
                c.getBoolean("broadcast.enabled"),
                c.getString("broadcast.permission", ""));

        Set<String> commands = new LinkedHashSet<>();
        for (String cmd : c.getStringList("interception.command.commands")) {
            String normalized = normalizeCommand(cmd);
            if (!normalized.isEmpty()) {
                commands.add(normalized);
            }
        }
        Set<String> silentFlags = new LinkedHashSet<>();
        for (String flag : c.getStringList("interception.command.silent-flags")) {
            if (flag != null && !flag.isBlank()) {
                silentFlags.add(flag.trim().toLowerCase(Locale.ROOT));
            }
        }
        CommandInterception commandInterception = new CommandInterception(
                Collections.unmodifiableSet(commands),
                c.getBoolean("interception.command.respect-command-permission"),
                c.getBoolean("interception.command.skip-silent"),
                Collections.unmodifiableSet(silentFlags));

        List<Pattern> patterns = new ArrayList<>();
        for (String regex : c.getStringList("interception.kick.reason-patterns")) {
            try {
                patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                log.warning("Ignoring invalid kick reason pattern '" + regex + "': " + e.getDescription());
            }
        }
        KickInterception kickInterception = new KickInterception(
                c.getBoolean("interception.kick.enabled"),
                List.copyOf(patterns),
                c.getBoolean("interception.kick.check-banlist"));

        Map<HookTarget, HookMode> hooks = new EnumMap<>(HookTarget.class);
        ConfigurationSection hookSection = c.getConfigurationSection("hooks");
        for (HookTarget target : HookTarget.values()) {
            String raw = hookSection == null ? null : hookSection.getString(target.key());
            HookMode mode = HookMode.parse(raw, target.defaultMode());
            if (raw != null && HookMode.parse(raw, null) == null) {
                log.warning("Unknown hook mode '" + raw + "' for hooks." + target.key() + ". Using " + mode + ".");
            }
            hooks.put(target, mode);
        }

        return new PluginConfig(
                c.getBoolean("debug"),
                animation, kill, spectator, replica, mace, impact, bones, broadcast,
                commandInterception, kickInterception,
                Collections.unmodifiableMap(hooks),
                c.getBoolean("test.spectate"));
    }

    /** {@code "/LiteBans:Ban"} → {@code "litebans:ban"}. */
    public static String normalizeCommand(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        int space = s.indexOf(' ');
        return space >= 0 ? s.substring(0, space) : s;
    }

    private static List<SoundSpec> parseSounds(List<Map<?, ?>> raw) {
        List<SoundSpec> out = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            String key = str(m.get("key"));
            if (key == null || key.isBlank()) {
                continue;
            }
            out.add(new SoundSpec(key.trim(),
                    (float) clamp(num(m.get("volume"), 1), 0, 10),
                    (float) clamp(num(m.get("pitch"), 1), 0.5, 2)));
        }
        return List.copyOf(out);
    }

    private static List<ParticleSpec> parseParticles(List<Map<?, ?>> raw, Logger log) {
        List<ParticleSpec> out = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            String key = str(m.get("key"));
            if (key == null || key.isBlank()) {
                continue;
            }
            out.add(new ParticleSpec(key.trim(),
                    (int) clamp(num(m.get("count"), 10), 0, 2000),
                    clamp(num(m.get("offset"), 0.5), 0, 16),
                    clamp(num(m.get("speed"), 0), 0, 10),
                    str(m.get("data")),
                    parseRgb(str(m.get("color")), log),
                    (float) clamp(num(m.get("size"), 1), 0.01, 4)));
        }
        return List.copyOf(out);
    }

    private static int parseRgb(String hex, Logger log) {
        if (hex == null || hex.isBlank()) {
            return 0xFF0000;
        }
        String h = hex.trim().replace("#", "");
        try {
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            log.warning("Invalid particle color '" + hex + "'. Using red.");
            return 0xFF0000;
        }
    }

    private static int parseArgb(String hex, Logger log) {
        if (hex == null || hex.isBlank()) {
            return 0;
        }
        String h = hex.trim().replace("#", "");
        try {
            return (int) Long.parseLong(h, 16);
        } catch (NumberFormatException e) {
            log.warning("Invalid label background '" + hex + "'. Using none.");
            return 0;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static double num(Object o, double fallback) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o != null) {
            try {
                return Double.parseDouble(String.valueOf(o).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static double clamp(double v, double min, double max) {
        if (Double.isNaN(v)) {
            return min;
        }
        return Math.max(min, Math.min(max, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
