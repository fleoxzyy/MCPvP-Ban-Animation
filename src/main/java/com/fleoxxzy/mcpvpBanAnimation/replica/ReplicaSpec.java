package com.fleoxxzy.mcpvpBanAnimation.replica;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.util.UUID;

/**
 * Everything needed to build a replica, copied from the player on their own thread. It is
 * immutable afterwards, so it can be handed to the replica's region thread safely.
 */
public record ReplicaSpec(
        UUID uuid,
        String name,
        PlayerProfile profile,
        String skinUrl,
        MainHand mainHand,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack mainHandItem,
        ItemStack offHandItem
) {

    /** Must run on the player's owning thread. */
    public static ReplicaSpec capture(Player player) {
        PlayerProfile profile;
        String skinUrl = null;
        try {
            profile = player.getPlayerProfile().clone();
            URL skin = profile.getTextures().getSkin();
            skinUrl = skin == null ? null : skin.toString();
        } catch (Throwable t) {
            profile = null;
        }
        EntityEquipment eq = player.getEquipment();
        MainHand hand;
        try {
            hand = player.getMainHand();
        } catch (Throwable t) {
            hand = MainHand.RIGHT;
        }
        return new ReplicaSpec(
                player.getUniqueId(),
                player.getName(),
                profile,
                skinUrl,
                hand,
                copy(eq == null ? null : eq.getHelmet()),
                copy(eq == null ? null : eq.getChestplate()),
                copy(eq == null ? null : eq.getLeggings()),
                copy(eq == null ? null : eq.getBoots()),
                copy(eq == null ? null : eq.getItemInMainHand()),
                copy(eq == null ? null : eq.getItemInOffHand()));
    }

    static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private static ItemStack copy(ItemStack item) {
        return isEmpty(item) ? null : item.clone();
    }

    /** A fresh clone (ItemStacks are mutable, so never hand out the stored instance). */
    static ItemStack give(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
