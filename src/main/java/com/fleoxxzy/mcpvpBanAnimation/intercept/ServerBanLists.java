package com.fleoxxzy.mcpvpBanAnimation.intercept;

import com.fleoxxzy.mcpvpBanAnimation.core.Durations;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Date;

/**
 * Reads the server's own ban lists (used by vanilla and EssentialsX). Every call is wrapped,
 * because ban list implementations differ between versions and forks.
 */
final class ServerBanLists {

    private ServerBanLists() {
    }

    /** The player's name or IP ban entry, or null if they are on neither list. */
    static BanEntry<?> find(Player player) {
        BanEntry<?> entry = profileEntry(player);
        return entry != null ? entry : ipEntry(player);
    }

    static boolean isIpBanned(Player player) {
        return ipEntry(player) != null;
    }

    /** "7d 3h", "45m", or null for a permanent ban. */
    static String remaining(BanEntry<?> entry) {
        Date expires = entry.getExpiration();
        return expires == null ? null : Durations.remaining(expires.getTime());
    }

    private static BanEntry<?> profileEntry(Player player) {
        try {
            ProfileBanList list = Bukkit.getBanList(BanList.Type.PROFILE);
            return active(list == null ? null : list.getBanEntry(player.getPlayerProfile()));
        } catch (Throwable t) {
            return null;
        }
    }

    private static BanEntry<?> ipEntry(Player player) {
        try {
            InetSocketAddress address = player.getAddress();
            if (address == null || address.getAddress() == null) {
                return null;
            }
            IpBanList list = Bukkit.getBanList(BanList.Type.IP);
            return active(list == null ? null : list.getBanEntry(address.getAddress()));
        } catch (Throwable t) {
            return null;
        }
    }

    private static BanEntry<?> active(BanEntry<?> entry) {
        if (entry == null) {
            return null;
        }
        Date expires = entry.getExpiration();
        return expires == null || expires.after(new Date()) ? entry : null;
    }
}
