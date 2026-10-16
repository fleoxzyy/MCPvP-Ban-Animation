package com.fleoxxzy.mcpvpBanAnimation.hook;

import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.config.HookMode;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;
import com.fleoxxzy.mcpvpBanAnimation.trigger.BanDetails;
import com.fleoxxzy.mcpvpBanAnimation.trigger.BanSource;
import com.fleoxxzy.mcpvpBanAnimation.trigger.TriggerService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** What every hook shares: config checks, ID parsing, and reporting the ban. */
final class HookSupport {

    private final ConfigManager configs;
    private final TriggerService triggers;
    private final Logger logger;

    HookSupport(ConfigManager configs, TriggerService triggers, Logger logger) {
        this.configs = configs;
        this.triggers = triggers;
        this.logger = logger;
    }

    /** True if this plugin's bans should currently come from its API (re-read on every ban, so reloads apply). */
    boolean active(HookTarget target) {
        return configs.config().hookMode(target) == HookMode.API;
    }

    /**
     * Reports a ban from a plugin API. For IP bans without a player, every online player on that
     * address is reported. Any thread. Never throws.
     */
    void report(HookTarget target, UUID uuid, String name, InetAddress ip, String staff, String reason,
                String duration, boolean ipBan, boolean silent) {
        try {
            if (!active(target)) {
                return;
            }
            if (uuid == null && name == null && ip == null) {
                return;
            }
            if (uuid == null && name == null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (ip.equals(addressOf(p))) {
                        dispatch(new BanDetails(p.getUniqueId(), p.getName(), staff, reason, duration, true, BanSource.API),
                                silent);
                    }
                }
                return;
            }
            dispatch(new BanDetails(uuid, name, staff, reason, duration, ipBan, BanSource.API), silent);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Could not handle a ban from " + target.key(), t);
        }
    }

    private void dispatch(BanDetails details, boolean silent) {
        if (silent && configs.config().commandInterception().skipSilent()) {
            triggers.suppressNextBan(details);
        } else {
            triggers.banConfirmed(details);
        }
    }

    void debug(String message) {
        if (configs.config().debug()) {
            logger.info("[debug] " + message);
        }
    }

    Logger logger() {
        return logger;
    }

    /** Accepts {@code 069a79f4-44e9-4726-a5be-fca90e38aaf5} and the undashed form. Null if neither. */
    static UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.length() == 32 && s.matches("[0-9a-fA-F]{32}")) {
            s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-"
                    + s.substring(16, 20) + "-" + s.substring(20);
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Parses a literal IP address. Never does a DNS lookup. Null if it isn't one. */
    static InetAddress parseIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        boolean literal = s.matches("[0-9.]+") || (s.contains(":") && s.matches("[0-9a-fA-F:.]+"));
        if (!literal) {
            return null;
        }
        try {
            return InetAddress.getByName(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** A staff member's name from their UUID, from the local player cache only. */
    static String nameOf(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        try {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            return offline.getName();
        } catch (Throwable t) {
            return null;
        }
    }

    private static InetAddress addressOf(Player p) {
        InetSocketAddress address = p.getAddress();
        return address == null ? null : address.getAddress();
    }
}
