package com.fleoxxzy.mcpvpBanAnimation.core;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationCallbacks;
import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationContext;
import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationManager;
import com.fleoxxzy.mcpvpBanAnimation.animation.EndCause;
import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.config.Messages;
import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import com.fleoxxzy.mcpvpBanAnimation.replica.ReplicaSpec;
import com.fleoxxzy.mcpvpBanAnimation.replica.ReplicaStage;
import com.fleoxxzy.mcpvpBanAnimation.replica.SkinColors;
import com.fleoxxzy.mcpvpBanAnimation.text.Placeholders;
import com.fleoxxzy.mcpvpBanAnimation.trigger.PlayerSnapshot;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Builds and starts ban animations from player snapshots. Used by {@code /mcpvpban test} and by
 * the ban trigger. The animation is purely visual: it never touches the player.
 */
public final class AnimationService {

    public static final String BYPASS_PERMISSION = "mcpvpban.bypass";

    private final Platform platform;
    private final ConfigManager configs;
    private final AnimationManager animations;
    private final SkinColors skinColors;
    private final Logger logger;

    public AnimationService(Plugin plugin, Platform platform, ConfigManager configs, AnimationManager animations) {
        this.platform = platform;
        this.configs = configs;
        this.animations = animations;
        this.logger = plugin.getLogger();
        this.skinColors = new SkinColors(platform.scheduler(), logger);
    }

    /** Copies everything the animation needs from the player. Must run on the player's thread. */
    public PlayerSnapshot snapshot(Player player) {
        ReplicaSpec spec = ReplicaSpec.capture(player);
        // Pre-fetch the skin colors for the ArmorStand leather fallback.
        skinColors.get(spec.skinUrl());
        boolean bypass;
        try {
            bypass = player.hasPermission(BYPASS_PERMISSION);
        } catch (Throwable t) {
            bypass = false;
        }
        return new PlayerSnapshot(player.getUniqueId(), player.getName(), anchorFor(player), spec, bypass,
                System.currentTimeMillis());
    }

    /**
     * Plays the animation for a snapshot. Safe to call from any thread.
     *
     * @param key            one animation per key at a time
     * @param placeholders   {@code username, reason, staff, duration}
     * @param ignoreCooldown skip the duplicate-detection cooldown (tests)
     * @return true if it started, false if a duplicate (already running or started too recently)
     */
    public boolean play(UUID key, PlayerSnapshot snapshot, Map<String, String> placeholders, boolean ignoreCooldown) {
        String label = Placeholders.apply(configs.config().replica().label().text(), placeholders);
        ReplicaStage stage = new ReplicaStage(snapshot.spec(), label, platform.text(), skinColors, logger);
        String name = snapshot.name();
        return animations.start(key, snapshot.anchor(), new AnimationCallbacks() {
            @Override
            public void onSetup(AnimationContext context) {
                stage.setup(context);
            }

            @Override
            public void onImpact(AnimationContext context) {
                stage.impact(context);
            }

            @Override
            public void onFinish(EndCause cause) {
                if (configs.config().debug()) {
                    logger.info("Ban animation for " + name + " finished (" + cause + ").");
                }
            }
        }, ignoreCooldown).isPresent();
    }

    /**
     * {@code /mcpvpban test}: plays the animation at the player's position. There is no ban,
     * and the player is not changed in any way. Safe to call from any thread.
     */
    public void playTest(Player target, CommandSender requester) {
        Messages messages = configs.messages();
        String staff = requester == null ? "Console" : requester.getName();
        platform.scheduler().runFor(target, () -> {
            PlayerSnapshot snapshot = snapshot(target);
            Map<String, String> values = Placeholders.of(
                    "username", target.getName(),
                    "reason", "Test",
                    "staff", staff,
                    "duration", "test");
            // Separate key, so a test never blocks (or is mistaken for) a real ban of the same player.
            UUID key = UUID.nameUUIDFromBytes(("test:" + target.getUniqueId()).getBytes(StandardCharsets.UTF_8));
            if (play(key, snapshot, values, true)) {
                reply(requester, messages.get("test-started", "username", target.getName()));
            } else {
                reply(requester, messages.get("already-animating", "username", target.getName()));
            }
        }, () -> reply(requester, messages.get("player-not-found", "player", target.getName())));
    }

    /** The player's feet snapped onto the ground, facing their yaw, level pitch. Player thread only. */
    public Location anchorFor(Player player) {
        Location loc = player.getLocation();
        loc.setPitch(0f);
        return Ground.snap(loc, platform.scheduler(), 6);
    }

    private void reply(CommandSender to, String message) {
        if (to != null) {
            platform.text().send(to, message);
        }
    }
}
