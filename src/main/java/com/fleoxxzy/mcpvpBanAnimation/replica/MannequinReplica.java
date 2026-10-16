package com.fleoxxzy.mcpvpBanAnimation.replica;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationContext;
import com.fleoxxzy.mcpvpBanAnimation.core.Reflect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.MainHand;
import org.bukkit.profile.PlayerProfile;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * A {@code Mannequin} (1.21.9+) wearing the player's skin. Mannequin is newer than our 1.21 compile
 * target, so it is only touched through reflection. The API also differs between platforms:
 * Spigot uses {@code setPlayerProfile(PlayerProfile)}, Paper uses {@code setProfile(ResolvableProfile)}.
 */
final class MannequinReplica implements Replica {

    static final String CLASS_NAME = "org.bukkit.entity.Mannequin";
    private static final Class<?> MANNEQUIN = Reflect.findClass(CLASS_NAME);

    private final ReplicaSpec spec;
    private final boolean copyEquipment;
    private final Logger logger;
    private LivingEntity entity;

    MannequinReplica(ReplicaSpec spec, boolean copyEquipment, Logger logger) {
        this.spec = spec;
        this.copyEquipment = copyEquipment;
        this.logger = logger;
    }

    static boolean supported() {
        return MANNEQUIN != null && LivingEntity.class.isAssignableFrom(MANNEQUIN);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean spawn(AnimationContext ctx) {
        if (!supported()) {
            return false;
        }
        Class<? extends LivingEntity> type = (Class<? extends LivingEntity>) MANNEQUIN;
        Location at = ctx.anchor();
        entity = ctx.spawn(at, type, m -> {
            m.setGravity(false);
            m.setInvulnerable(true);
            m.setSilent(true);
            m.setCollidable(false);
            m.setRemoveWhenFarAway(false);
            m.setCanPickupItems(false);
            m.setCustomNameVisible(false);
            m.setRotation(at.getYaw(), -ctx.config().replica().lookUpDegrees());
            invoke(m, "setImmovable", new Class<?>[]{boolean.class}, true);
            applyProfile(m);
            hideDescription(m);
            if (spec.mainHand() != null) {
                invoke(m, "setMainHand", new Class<?>[]{MainHand.class}, spec.mainHand());
            }
            if (copyEquipment) {
                equip(m.getEquipment());
            }
        });
        return entity != null;
    }

    @Override
    public LivingEntity entity() {
        return entity;
    }

    @Override
    public double height() {
        return 1.8;
    }

    private void equip(EntityEquipment eq) {
        if (eq == null) {
            return;
        }
        eq.setHelmet(ReplicaSpec.give(spec.helmet()));
        eq.setChestplate(ReplicaSpec.give(spec.chestplate()));
        eq.setLeggings(ReplicaSpec.give(spec.leggings()));
        eq.setBoots(ReplicaSpec.give(spec.boots()));
        eq.setItemInMainHand(ReplicaSpec.give(spec.mainHandItem()));
        eq.setItemInOffHand(ReplicaSpec.give(spec.offHandItem()));
    }

    private void applyProfile(Entity m) {
        PlayerProfile profile = spec.profile();
        if (profile == null) {
            return;
        }
        // Spigot: Mannequin#setPlayerProfile(org.bukkit.profile.PlayerProfile)
        if (invoke(m, "setPlayerProfile", new Class<?>[]{PlayerProfile.class}, profile)) {
            return;
        }
        // Paper: Mannequin#setProfile(ResolvableProfile.resolvableProfile(paperProfile))
        Class<?> resolvable = Reflect.findClass("io.papermc.paper.datacomponent.item.ResolvableProfile");
        Class<?> paperProfile = Reflect.findClass("com.destroystokyo.paper.profile.PlayerProfile");
        if (resolvable != null && paperProfile != null && paperProfile.isInstance(profile)) {
            try {
                Object rp = resolvable.getMethod("resolvableProfile", paperProfile).invoke(null, profile);
                if (invoke(m, "setProfile", new Class<?>[]{resolvable}, rp)) {
                    return;
                }
            } catch (Throwable t) {
                logger.fine("Paper ResolvableProfile failed: " + t);
            }
        }
        logger.warning("Could not apply the player's skin to the Mannequin. It will use the default skin.");
    }

    private void hideDescription(Entity m) {
        // Spigot: setHideDescription(true). Paper: setDescription(null) hides the "NPC" line.
        if (invoke(m, "setHideDescription", new Class<?>[]{boolean.class}, true)) {
            return;
        }
        Class<?> component = Reflect.findClass("net.kyori.adventure.text.Component");
        if (component != null) {
            invoke(m, "setDescription", new Class<?>[]{component}, (Object) null);
        }
    }

    /** Calls a method declared on the Mannequin interface. Returns false if it is missing or fails. */
    private static boolean invoke(Object target, String name, Class<?>[] params, Object... args) {
        Method method = Reflect.findMethod(MANNEQUIN, name, params);
        if (method == null) {
            return false;
        }
        try {
            method.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
