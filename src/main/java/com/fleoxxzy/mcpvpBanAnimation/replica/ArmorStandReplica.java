package com.fleoxxzy.mcpvpBanAnimation.replica;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.EulerAngle;

/**
 * Fallback replica for servers without Mannequin: an armor stand wearing the player's head and armor.
 * If the player wears no armor, it gets leather armor dyed to match their skin.
 */
final class ArmorStandReplica implements Replica {

    private final ReplicaSpec spec;
    private final boolean copyEquipment;
    private final boolean leatherFallback;
    private final SkinColors skinColors;
    private ArmorStand entity;

    ArmorStandReplica(ReplicaSpec spec, boolean copyEquipment, boolean leatherFallback, SkinColors skinColors) {
        this.spec = spec;
        this.copyEquipment = copyEquipment;
        this.leatherFallback = leatherFallback;
        this.skinColors = skinColors;
    }

    @Override
    public boolean spawn(AnimationContext ctx) {
        Location at = ctx.anchor();
        boolean wearsArmor = copyEquipment && (spec.chestplate() != null || spec.leggings() != null || spec.boots() != null);
        boolean useLeather = leatherFallback && !wearsArmor;
        SkinColors.Palette known = useLeather ? skinColors.getNow(spec.skinUrl()) : null;

        entity = ctx.spawn(at, ArmorStand.class, stand -> {
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setBasePlate(false);
            stand.setArms(true);
            stand.setCollidable(false);
            stand.setCustomNameVisible(false);
            stand.setRotation(at.getYaw(), 0f);
            stand.setHeadPose(new EulerAngle(Math.toRadians(-ctx.config().replica().lookUpDegrees()), 0, 0));
            stand.setLeftArmPose(new EulerAngle(Math.toRadians(-10), 0, Math.toRadians(-8)));
            stand.setRightArmPose(new EulerAngle(Math.toRadians(-15), 0, Math.toRadians(10)));
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND}) {
                stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
                stand.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
            EntityEquipment eq = stand.getEquipment();
            if (eq == null) {
                return;
            }
            eq.setHelmet(playerHead());
            if (wearsArmor) {
                eq.setChestplate(ReplicaSpec.give(spec.chestplate()));
                eq.setLeggings(ReplicaSpec.give(spec.leggings()));
                eq.setBoots(ReplicaSpec.give(spec.boots()));
            } else if (useLeather) {
                dress(eq, known == null ? SkinColors.DEFAULT : known);
            }
            if (copyEquipment) {
                eq.setItemInMainHand(ReplicaSpec.give(spec.mainHandItem()));
                eq.setItemInOffHand(ReplicaSpec.give(spec.offHandItem()));
            }
        });

        // The skin was not downloaded yet: recolor the leather as soon as it is.
        if (useLeather && known == null && entity != null) {
            ArmorStand stand = entity;
            skinColors.get(spec.skinUrl()).thenAccept(palette -> {
                if (palette != SkinColors.DEFAULT) {
                    ctx.platform().scheduler().runAt(at, () -> {
                        if (stand.isValid() && stand.getEquipment() != null) {
                            dress(stand.getEquipment(), palette);
                        }
                    });
                }
            });
        }
        return entity != null;
    }

    @Override
    public ArmorStand entity() {
        return entity;
    }

    @Override
    public double height() {
        return 1.8;
    }

    private ItemStack playerHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (spec.profile() != null && head.getItemMeta() instanceof SkullMeta meta) {
            try {
                meta.setOwnerProfile(spec.profile());
                head.setItemMeta(meta);
            } catch (Throwable ignored) {
                // Invalid profile: keep the default head.
            }
        }
        return head;
    }

    private static void dress(EntityEquipment eq, SkinColors.Palette palette) {
        eq.setChestplate(leather(Material.LEATHER_CHESTPLATE, palette.chest()));
        eq.setLeggings(leather(Material.LEATHER_LEGGINGS, palette.legs()));
        eq.setBoots(leather(Material.LEATHER_BOOTS, palette.boots()));
    }

    private static ItemStack leather(Material type, int rgb) {
        ItemStack item = new ItemStack(type);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(rgb & 0xFFFFFF));
            item.setItemMeta(meta);
        }
        return item;
    }
}
