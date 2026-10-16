package com.fleoxxzy.mcpvpBanAnimation.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Immutable, validated snapshot of {@code config.yml}. A reload builds a new instance and swaps it
 * in atomically, so tasks on other region threads always see one consistent config.
 */
public record PluginConfig(
        boolean debug,
        Animation animation,
        Trigger trigger,
        Replica replica,
        Mace mace,
        Impact impact,
        Bones bones,
        CommandInterception commandInterception,
        KickInterception kickInterception,
        Map<HookTarget, HookMode> hooks
) {

    public record Animation(int maceSpawnDelay, int maceFallTicks, int cleanupDelayAfterImpact,
                            int hardTimeoutTicks) {
        /** Tick (from start, first tick = 1) at which the mace spawns. */
        public int maceSpawnTick() {
            return Math.max(1, maceSpawnDelay);
        }

        /** Tick at which the mace hits: one tick after its last fall pose, so the client has finished interpolating. */
        public int impactTick() {
            return maceSpawnTick() + maceFallTicks + 1;
        }
    }

    /** All values in ticks. */
    public record Trigger(int snapshotLifetimeTicks, int duplicateCooldownTicks, int pendingBanLifetimeTicks) {
    }

    public record Label(String text, double offsetY, float scale, double bobAmplitude, double pulseAmplitude,
                        int period, boolean shadow, int backgroundArgb) {
    }

    public record Replica(ReplicaType type, boolean copyEquipment, boolean leatherFallback, float lookUpDegrees,
                          Label label, int squashTicks, float squashMinHeight) {
    }

    public record Mace(String material, boolean enchanted, double height, float scale, double impactHeight,
                       double spinDegrees, float yawOffset, boolean trailEnabled, List<String> trailParticles,
                       int trailCount) {
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

    /** {@code commands} are lower-case labels without the slash, e.g. {@code ban} or {@code litebans:ban}. */
    public record CommandInterception(Set<String> commands, boolean respectCommandPermission, boolean skipSilent,
                                      Set<String> silentFlags) {
        /** True for a watched label, also when it is namespaced ({@code anyplugin:tempban}). */
        public boolean isBanCommand(String label) {
            if (commands.contains(label)) {
                return true;
            }
            int colon = label.indexOf(':');
            return colon > 0 && commands.contains(label.substring(colon + 1));
        }
    }

    public record KickInterception(boolean enabled, boolean checkBanList, boolean matchReason,
                                   List<Pattern> reasonPatterns) {
    }

    /** Ban sources that can each have their own {@link HookMode}. */
    public enum HookTarget {
        VANILLA("vanilla", HookMode.COMMAND),
        ESSENTIALS("essentials", HookMode.COMMAND),
        LITEBANS("litebans", HookMode.API),
        ADVANCEDBAN("advancedban", HookMode.API),
        LIBERTYBANS("libertybans", HookMode.API),
        LIGHTBANS("lightbans", HookMode.COMMAND),
        UNKNOWN("other", HookMode.COMMAND);

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

        /** The ban source for a plugin name (e.g. "LiteBans"), or {@link #UNKNOWN}. */
        public static HookTarget fromPluginName(String pluginName) {
            if (pluginName == null) {
                return UNKNOWN;
            }
            return switch (pluginName.toLowerCase(Locale.ROOT)) {
                case "minecraft", "bukkit", "spigot", "paper" -> VANILLA;
                case "essentials", "essentialsx" -> ESSENTIALS;
                case "litebans" -> LITEBANS;
                case "advancedban" -> ADVANCEDBAN;
                case "libertybans" -> LIBERTYBANS;
                case "lightbans" -> LIGHTBANS;
                default -> UNKNOWN;
            };
        }
    }

    public HookMode hookMode(HookTarget target) {
        return hooks.getOrDefault(target, target.defaultMode());
    }

    // ---------------------------------------------------------------------------------------------
    // Parsing. Only the options a server owner really needs are read from config.yml. The rest
    // is the fixed MCPvP look and lives below as constants.
    // ---------------------------------------------------------------------------------------------

    private static final String DEFAULT_LABEL = "<red>â— <dark_red><bold>{username} BANNED</bold> <red>â—";

    /** Ban commands that are always watched, under any plugin's namespace. {@code detection.extra-commands} adds more. */
    private static final List<String> BAN_COMMANDS = List.of(
            "ban", "ban-ip", "banip", "ipban", "tempban", "tban", "tempipban", "tipban", "tempbanip",
            "eban", "etempban", "ebanip", "etempbanip", "permban", "pban");

    private static final Set<String> SILENT_FLAGS = Set.of("-s", "--silent", "-silent");

    private static final List<Pattern> KICK_PATTERNS = List.of(
            Pattern.compile("you (are|have been) (perm(anently)?|temp(orarily)?)?\\s*banned", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bban(ned)? (until|expires)\\b", Pattern.CASE_INSENSITIVE));

    private static final Trigger TRIGGER = new Trigger(200, 100, 100);

    private static final List<SoundSpec> IMPACT_SOUNDS = List.of(
            new SoundSpec("minecraft:item.mace.smash_ground_heavy", 1.6f, 0.9f),
            new SoundSpec("minecraft:entity.generic.explode", 1.0f, 1.1f),
            new SoundSpec("minecraft:entity.skeleton.death", 1.2f, 0.8f),
            new SoundSpec("minecraft:block.bone_block.break", 1.4f, 0.7f));

    private static final List<ParticleSpec> IMPACT_PARTICLES = List.of(
            new ParticleSpec("minecraft:explosion", 3, 0.6, 0.0, null, 0, 1f),
            new ParticleSpec("minecraft:crit", 60, 0.8, 0.6, null, 0, 1f),
            new ParticleSpec("minecraft:cloud", 40, 0.9, 0.08, null, 0, 1f),
            new ParticleSpec("minecraft:block", 70, 0.7, 0.15, "minecraft:bone_block", 0, 1f),
            new ParticleSpec("minecraft:dust", 45, 0.9, 0.0, null, 0x8B0000, 1.6f));

    private static final Shockwave SHOCKWAVE = new Shockwave(true, "minecraft:cloud", 4.5, 6, 1, 36);

    private static final Bones BONES = new Bones(20, 30, 3, 0.7f, 50, 10,
            new BonePhysics(0.055, 0.975, 0.45, 0.7, 0.25, 0.6, 0.35, 0.75, 0.2, 1.8, 25f));

    private static final int CLEANUP_AFTER_IMPACT = 60;

    public static PluginConfig from(FileConfiguration c, Logger log) {
        int spawnDelay = clampInt(c.getInt("animation.mace-spawn-delay", 4), 0, 200);
        int fallTicks = clampInt(c.getInt("animation.mace-fall-ticks", 20), 2, 200);
        // Safety net well past the normal end of the animation.
        int hardTimeout = spawnDelay + fallTicks + CLEANUP_AFTER_IMPACT + 100;
        Animation animation = new Animation(spawnDelay, fallTicks, CLEANUP_AFTER_IMPACT, hardTimeout);

        Label label = new Label(c.getString("animation.label", DEFAULT_LABEL),
                2.35, 1.25f, 0.06, 0.08, 30, true, 0x40000000);

        Replica replica = new Replica(
                ReplicaType.parse(c.getString("animation.replica", "auto")),
                true,
                true,
                (float) clamp(c.getDouble("animation.look-up", 55), 0, 90),
                label,
                4,
                0.08f);

        Mace mace = new Mace(
                "MACE",
                c.getBoolean("animation.enchanted-mace", true),
                clamp(c.getDouble("animation.mace-height", 12), 2, 64),
                2.0f,
                1.3,
                35.0,
                0f,
                true,
                List.of("minecraft:cloud", "minecraft:smoke"),
                3);

        Impact impact = new Impact(IMPACT_SOUNDS, IMPACT_PARTICLES, SHOCKWAVE,
                new CameraShake(c.getBoolean("animation.camera-shake", true), 16.0, 3, 3));

        Set<String> commands = new LinkedHashSet<>(BAN_COMMANDS);
        for (String cmd : c.getStringList("detection.extra-commands")) {
            String normalized = normalizeCommand(cmd);
            if (!normalized.isEmpty()) {
                commands.add(normalized);
            }
        }
        CommandInterception commandInterception = new CommandInterception(
                Collections.unmodifiableSet(commands),
                true,
                c.getBoolean("detection.skip-silent-bans", true),
                SILENT_FLAGS);

        KickInterception kickInterception = new KickInterception(
                true,
                true,
                c.getBoolean("detection.match-kick-message", false),
                KICK_PATTERNS);

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
                animation, TRIGGER, replica, mace, impact, BONES,
                commandInterception, kickInterception,
                Collections.unmodifiableMap(hooks));
    }

    /** {@code "/LiteBans:Ban"} â†’ {@code "litebans:ban"}. */
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
