package com.fleoxxzy.mcpvpBanAnimation.trigger;

import java.util.UUID;

/**
 * What we know about a ban. Any text field may be null when the detector could not tell
 * (for example a kick only knows its reason). Missing values fall back to messages.yml defaults.
 *
 * @param uuid     the banned player, or null if only the name is known
 * @param name     the banned player's name, or null if only the UUID is known
 * @param ipBan    true for {@code ban-ip} style bans
 */
public record BanDetails(UUID uuid, String name, String staff, String reason, String duration,
                         boolean ipBan, BanSource source) {

    /** Fills every null field of this one from {@code other}. */
    public BanDetails mergeFrom(BanDetails other) {
        if (other == null) {
            return this;
        }
        return new BanDetails(
                uuid != null ? uuid : other.uuid,
                name != null ? name : other.name,
                blank(staff) ? other.staff : staff,
                blank(reason) ? other.reason : reason,
                blank(duration) ? other.duration : duration,
                ipBan || other.ipBan,
                source);
    }

    public BanDetails withPlayer(UUID uuid, String name) {
        return new BanDetails(uuid, name, staff, reason, duration, ipBan, source);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
