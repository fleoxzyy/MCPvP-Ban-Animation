package com.fleoxxzy.mcpvpBanAnimation.intercept;

import com.fleoxxzy.mcpvpBanAnimation.config.HookMode;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.trigger.BanDetails;
import com.fleoxxzy.mcpvpBanAnimation.trigger.BanSource;
import com.fleoxxzy.mcpvpBanAnimation.trigger.TriggerService;
import org.bukkit.BanEntry;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Starts the animation when a player is kicked <b>because of a ban</b>. A normal kick never
 * plays it. The kick is never cancelled or changed: we only take the snapshot (we are on the
 * player's own thread here, also on Folia) and start the visuals.
 *
 * <p>A kick counts as a ban if, in this order:
 * <ol>
 *   <li>a ban command or ban plugin hook for this player was seen moments ago, or</li>
 *   <li>Paper/Folia reports the kick cause as {@code BANNED} / {@code IP_BANNED}, or</li>
 *   <li>the player or their IP is on the server's ban list ({@code check-banlist}), or</li>
 *   <li>the kick message matches a configured pattern ({@code match-reason}, off by default).</li>
 * </ol>
 * Rules 2–4 can be turned off with {@code interception.kick.enabled: false}, or for unknown ban
 * sources with {@code hooks.unknown: OFF}.
 */
public final class KickInterceptor implements Listener {

    private final ConfigManager configs;
    private final TriggerService triggers;
    private final Logger logger;

    public KickInterceptor(ConfigManager configs, TriggerService triggers, Logger logger) {
        this.configs = configs;
        this.triggers = triggers;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        try {
            inspect(event);
        } catch (Throwable t) {
            // Never let our listener disturb the kick.
            logger.log(Level.WARNING, "Error while inspecting a kick", t);
        }
    }

    private void inspect(PlayerKickEvent event) {
        Player player = event.getPlayer();
        PluginConfig cfg = configs.config();
        PluginConfig.KickInterception kick = cfg.kickInterception();

        if (triggers.isSuppressed(player.getUniqueId(), player.getName())
                || triggers.hasPendingBan(player.getUniqueId(), player.getName())) {
            // Seen as a ban command/hook already (or explicitly not animated). The trigger sorts it out.
            triggers.banKick(player, fromKick(player, null, BanSource.KICK));
            return;
        }
        if (!kick.enabled() || cfg.hookMode(PluginConfig.HookTarget.UNKNOWN) == HookMode.OFF) {
            return;
        }

        String cause = kickCause(event);
        BanEntry<?> entry = null;
        boolean ban = "BANNED".equals(cause) || "IP_BANNED".equals(cause);
        if (ban || kick.checkBanList()) {
            entry = ServerBanLists.find(player);
            ban |= entry != null;
        }
        if (!ban && kick.matchReason() && matches(kick, event.getReason())) {
            ban = true;
        }
        if (!ban) {
            return;
        }
        if (cfg.debug()) {
            logger.info("[debug] Kick of " + player.getName() + " recognised as a ban (cause: " + cause
                    + ", ban list: " + (entry != null) + ").");
        }
        BanDetails details = fromKick(player, entry, BanSource.KICK);
        if ("IP_BANNED".equals(cause)) {
            details = new BanDetails(details.uuid(), details.name(), details.staff(), details.reason(),
                    details.duration(), true, details.source());
        }
        triggers.banKick(player, details);
    }

    private static BanDetails fromKick(Player player, BanEntry<?> entry, BanSource source) {
        if (entry == null) {
            return new BanDetails(player.getUniqueId(), player.getName(), null, null, null, false, source);
        }
        String staff = entry.getSource();
        // Vanilla uses "(Unknown)" / "Server" when no staff is known.
        if (staff != null && (staff.equalsIgnoreCase("(Unknown)") || staff.isBlank())) {
            staff = null;
        }
        String reason = entry.getReason();
        if (reason != null && reason.equalsIgnoreCase("Banned by an operator.")) {
            reason = null;
        }
        return new BanDetails(player.getUniqueId(), player.getName(), staff, reason, ServerBanLists.remaining(entry),
                ServerBanLists.isIpBanned(player), source);
    }

    /** Paper/Folia only ({@code PlayerKickEvent#getCause}). Null on Spigot. */
    private static String kickCause(PlayerKickEvent event) {
        try {
            Object cause = event.getClass().getMethod("getCause").invoke(event);
            return cause instanceof Enum<?> e ? e.name() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean matches(PluginConfig.KickInterception kick, String reason) {
        if (reason == null || reason.isEmpty()) {
            return false;
        }
        String plain = ChatColor.stripColor(reason);
        for (Pattern p : kick.reasonPatterns()) {
            if (p.matcher(plain).find()) {
                return true;
            }
        }
        return false;
    }
}
