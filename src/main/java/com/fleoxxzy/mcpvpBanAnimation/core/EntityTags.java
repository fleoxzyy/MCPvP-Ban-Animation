package com.fleoxxzy.mcpvpBanAnimation.core;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Marks every entity this plugin spawns, so leftovers (e.g. after a crash) can be found and removed.
 */
public final class EntityTags {

    private final NamespacedKey key;

    public EntityTags(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "spawned");
    }

    /** Tags the entity and makes it non-persistent, so it is never saved with the chunk. */
    public void tag(Entity entity) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        entity.setPersistent(false);
    }

    public boolean isTagged(Entity entity) {
        try {
            return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
        } catch (Throwable t) {
            return false;
        }
    }
}
