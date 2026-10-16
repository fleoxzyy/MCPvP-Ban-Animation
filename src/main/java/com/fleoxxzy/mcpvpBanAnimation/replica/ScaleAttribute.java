package com.fleoxxzy.mcpvpBanAnimation.replica;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * The entity scale attribute (1.20.5+). It is {@code generic.scale} on 1.21–1.21.1 and
 * {@code scale} from 1.21.2. It is looked up through the registry by key, so we never touch an
 * enum constant (Attribute changes from an enum to an interface in newer versions).
 */
final class ScaleAttribute {

    private static final Attribute SCALE = resolve();

    private ScaleAttribute() {
    }

    static boolean available() {
        return SCALE != null;
    }

    static boolean set(LivingEntity entity, double value) {
        if (SCALE == null) {
            return false;
        }
        try {
            AttributeInstance instance = entity.getAttribute(SCALE);
            if (instance == null) {
                return false;
            }
            instance.setBaseValue(value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Attribute resolve() {
        for (String key : new String[]{"scale", "generic.scale"}) {
            try {
                Attribute a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
                if (a != null) {
                    return a;
                }
            } catch (Throwable ignored) {
                // Try the next key.
            }
        }
        return null;
    }
}
