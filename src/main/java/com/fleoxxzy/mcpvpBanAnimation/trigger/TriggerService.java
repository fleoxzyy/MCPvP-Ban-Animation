package com.fleoxxzy.mcpvpBanAnimation.trigger;

import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.config.Messages;
import com.fleoxxzy.mcpvpBanAnimation.core.AnimationService;
import com.fleoxxzy.mcpvpBanAnimation.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns detected bans into animations. Every detector (command, kick, API hook) reports here. This class never bans, kicks, cancels or delays anything: the ban has already
 * happened (or is about to). It only decides whether and where to play the visuals.
 *
 * <ul>
 *   <li>{@link #banCommandSeen}: a ban command was run. Not confirmed yet (it may fail), so it is only
 *       remembered as a pending ban until the kick arrives.</li>
 *   <li>{@link #banConfirmed}: a ban plugin's API reported a ban. Plays from the cached quit
 *       snapshot if the player is already gone, otherwise waits for their kick/quit.</li>
 *   <li>{@link #banKick}: the player is being kicked because of a ban. Plays right away.</li>
 *   <li>{@link #playerQuit}: caches the snapshot, and plays if a confirmed ban is pending
 *       (bans enforced by a proxy disconnect the player without a kick event).</li>
 * </ul>
 */
public final class TriggerService {

    private final ConfigManager configs;
    private final AnimationService animations;
    private final Logger logger;
    private final SnapshotCache snapshots;
    private final PendingBans pendingCommands;
    private final PendingBans pendingConfirmed;
    /** Bans that must not animate (silent bans, plugins set to OFF), waiting for their kick. */
    private final PendingBans suppressed;

    public TriggerService(Plugin plugin, ConfigManager configs, AnimationService animations) {
        this.configs = configs;
        this.animations = animations;
        this.logger = plugin.getLogger();
        this.snapshots = new SnapshotCache(() -> configs.config().trigger().snapshotLifetimeTicks() * 50L);
        this.pendingCommands = new PendingBans(() -> configs.config().trigger().pendingBanLifetimeTicks() * 50L);
        this.pendingConfirmed = new PendingBans(() -> configs.config().trigger().pendingBanLifetimeTicks() * 50L);
        this.suppressed = new PendingBans(() -> configs.config().trigger().pendingBanLifetimeTicks() * 50L);
    }

    // ---------------------------------------------------------------------------------------------
    // Detector entry points
    // ---------------------------------------------------------------------------------------------

    /** A ban command naming this player was run. Any thread. */
    public void banCommandSeen(BanDetails details) {
        pendingCommands.add(details);
        debug("Pending ban from a command: " + details);
    }

    /**
     * The next ban of this player must not animate (a silent ban, or its plugin is set to OFF).
     * The kick that follows would otherwise still look like a ban. Any thread.
     */
    public void suppressNextBan(BanDetails details) {
        suppressed.add(details);
        debug("Not animating the next ban of " + describe(details) + ".");
    }

    /** True if the next ban of this player must not animate. Any thread. */
    public boolean isSuppressed(java.util.UUID uuid, String name) {
        return suppressed.has(uuid, name);
    }

    /** True if a ban command for this player was seen moments ago. Any thread. */
    public boolean hasPendingBan(java.util.UUID uuid, String name) {
        return pendingCommands.has(uuid, name) || pendingConfirmed.has(uuid, name);
    }

    /** A ban plugin's API reported a ban. Any thread. */
    public void banConfirmed(BanDetails details) {
        if (isSuppressed(details.uuid(), details.name())) {
            debug("Confirmed ban for " + describe(details) + " is suppressed.");
            return;
        }
        Player online = findOnline(details);
        if (online != null) {
            // Still online: the kick that follows plays it (with the player's latest position).
            pendingConfirmed.add(details.withPlayer(online.getUniqueId(), online.getName()));
            debug("Confirmed ban for online player " + online.getName() + ". Waiting for the kick.");
            return;
        }
        PlayerSnapshot snapshot = snapshots.find(details.uuid(), details.name());
        if (snapshot == null) {
            debug("Confirmed ban for " + describe(details) + ", but they are offline and no recent snapshot exists.");
            return;
        }
        start(snapshot, details);
    }

    /** The player is being kicked for a ban. Must run on the player's thread (the kick event). */
    public void banKick(Player player, BanDetails fromKick) {
        // Keep the mark until it expires: an API hook may still report this ban after the kick.
        if (suppressed.has(player.getUniqueId(), player.getName())) {
            pendingCommands.take(player.getUniqueId(), player.getName());
            pendingConfirmed.take(player.getUniqueId(), player.getName());
            debug("Ban kick of " + player.getName() + " is suppressed (silent ban or plugin set to OFF).");
            return;
        }
        BanDetails details = merge(player, fromKick);
        PlayerSnapshot snapshot = animations.snapshot(player);
        snapshots.put(snapshot);
        start(snapshot, details);
    }

    /** The player left. Must run on the player's thread (the quit event). */
    public void playerQuit(Player player) {
        PlayerSnapshot snapshot;
        try {
            snapshot = animations.snapshot(player);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Could not snapshot " + player.getName() + " on quit", t);
            return;
        }
        snapshots.put(snapshot);
        BanDetails confirmed = pendingConfirmed.take(player.getUniqueId(), player.getName());
        if (confirmed != null && !isSuppressed(player.getUniqueId(), player.getName())) {
            start(snapshot, confirmed.mergeFrom(pendingCommands.take(player.getUniqueId(), player.getName())));
        }
    }

    public void shutdown() {
        snapshots.clear();
        pendingCommands.clear();
        pendingConfirmed.clear();
        suppressed.clear();
    }

    // ---------------------------------------------------------------------------------------------

    private BanDetails merge(Player player, BanDetails fromKick) {
        BanDetails details = fromKick.withPlayer(player.getUniqueId(), player.getName());
        // The most specific source wins: API details, then the command, then what the kick said.
        BanDetails confirmed = pendingConfirmed.take(player.getUniqueId(), player.getName());
        BanDetails command = pendingCommands.take(player.getUniqueId(), player.getName());
        if (command != null) {
            details = command.withPlayer(player.getUniqueId(), player.getName()).mergeFrom(details);
        }
        if (confirmed != null) {
            details = confirmed.withPlayer(player.getUniqueId(), player.getName()).mergeFrom(details);
        }
        return details;
    }

    private void start(PlayerSnapshot snapshot, BanDetails details) {
        try {
            if (snapshot.bypass()) {
                debug(snapshot.name() + " has " + AnimationService.BYPASS_PERMISSION + ". No animation.");
                return;
            }
            Messages messages = configs.messages();
            Map<String, String> values = Placeholders.of(
                    "username", snapshot.name(),
                    "staff", orDefault(details.staff(), messages.raw("unknown-staff")),
                    "reason", orDefault(details.reason(), messages.raw("default-reason")),
                    "duration", orDefault(details.duration(), messages.raw("permanent")));
            if (!animations.play(snapshot.uuid(), snapshot, values, false)) {
                debug("Ignoring duplicate ban detection for " + snapshot.name() + " (" + details.source() + ").");
                return;
            }
            debug("Playing the ban animation for " + snapshot.name() + " (detected by " + details.source() + ").");
        } catch (Throwable t) {
            // Visuals only. The ban itself is not affected.
            logger.log(Level.WARNING, "Could not play the ban animation for " + snapshot.name(), t);
        }
    }

    private static Player findOnline(BanDetails details) {
        Player p = details.uuid() == null ? null : Bukkit.getPlayer(details.uuid());
        if (p == null && details.name() != null) {
            p = Bukkit.getPlayerExact(details.name());
        }
        return p != null && p.isOnline() ? p : null;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String describe(BanDetails d) {
        return d.name() != null ? d.name() : String.valueOf(d.uuid());
    }

    private void debug(String message) {
        if (configs.config().debug()) {
            logger.info("[debug] " + message);
        }
    }
}
