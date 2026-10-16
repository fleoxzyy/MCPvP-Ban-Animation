package com.fleoxxzy.mcpvpBanAnimation.platform;

import com.fleoxxzy.mcpvpBanAnimation.core.Reflect;
import org.bukkit.BanList;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime feature checks. Everything newer than the 1.21 compile target is detected here,
 * by name or by reflection, never by referencing the type directly.
 */
public record FeatureFlags(
        boolean hasMannequin,
        boolean hasMace,
        boolean hasKickCause,
        boolean hasProfileBanList,
        boolean hasPaperBanListType,
        boolean hasTeleportDuration,
        boolean hasTeleportAsync,
        boolean hasRegionScheduler,
        boolean hasAdventure,
        boolean hasPaperProfile,
        boolean hasEntitiesLoadEvent
) {

    public static FeatureFlags detect() {
        Class<?> displayClass = Display.class;
        return new FeatureFlags(
                Reflect.hasClass("org.bukkit.entity.Mannequin"),
                Material.matchMaterial("MACE") != null,
                Reflect.hasMethod(PlayerKickEvent.class, "getCause"),
                Reflect.hasEnumConstant(BanList.Type.class, "PROFILE"),
                Reflect.hasClass("io.papermc.paper.ban.BanListType"),
                Reflect.hasMethod(displayClass, "setTeleportDuration", int.class),
                Reflect.hasMethod(Entity.class, "teleportAsync", org.bukkit.Location.class),
                Reflect.hasClass("io.papermc.paper.threadedregions.scheduler.RegionScheduler"),
                Reflect.hasClass("net.kyori.adventure.text.Component")
                        && Reflect.hasClass("net.kyori.adventure.text.minimessage.MiniMessage"),
                Reflect.hasClass("com.destroystokyo.paper.profile.PlayerProfile"),
                Reflect.hasClass("org.bukkit.event.world.EntitiesLoadEvent")
        );
    }

    /** Ordered name → value view, used by {@code /mcpvpban info} and the startup log. */
    public Map<String, Boolean> asMap() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("mannequin", hasMannequin);
        map.put("mace", hasMace);
        map.put("kick-cause", hasKickCause);
        map.put("profile-banlist", hasProfileBanList);
        map.put("paper-banlist-type", hasPaperBanListType);
        map.put("teleport-duration", hasTeleportDuration);
        map.put("teleport-async", hasTeleportAsync);
        map.put("region-scheduler", hasRegionScheduler);
        map.put("adventure", hasAdventure);
        map.put("paper-profile", hasPaperProfile);
        map.put("entities-load-event", hasEntitiesLoadEvent);
        return map;
    }
}
