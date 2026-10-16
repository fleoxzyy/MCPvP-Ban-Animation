package com.fleoxxzy.mcpvpBanAnimation.trigger;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Snapshots of players who recently left, by UUID and by name. Ban events that arrive after the
 * kick (async API hooks, or a ban command run moments after the player logged out) use these.
 * Thread-safe.
 */
public final class SnapshotCache {

    private final Map<UUID, PlayerSnapshot> byUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();
    private final LongSupplier lifetimeMillis;

    public SnapshotCache(LongSupplier lifetimeMillis) {
        this.lifetimeMillis = lifetimeMillis;
    }

    public void put(PlayerSnapshot snapshot) {
        prune();
        byUuid.put(snapshot.uuid(), snapshot);
        if (snapshot.name() != null) {
            byName.put(snapshot.name().toLowerCase(Locale.ROOT), snapshot.uuid());
        }
    }

    public PlayerSnapshot get(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        PlayerSnapshot s = byUuid.get(uuid);
        return s != null && fresh(s) ? s : null;
    }

    public PlayerSnapshot get(String name) {
        if (name == null) {
            return null;
        }
        return get(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    /** Looks up by UUID first, then by name. */
    public PlayerSnapshot find(UUID uuid, String name) {
        PlayerSnapshot s = get(uuid);
        return s != null ? s : get(name);
    }

    public void remove(UUID uuid) {
        PlayerSnapshot s = byUuid.remove(uuid);
        if (s != null && s.name() != null) {
            byName.remove(s.name().toLowerCase(Locale.ROOT), uuid);
        }
    }

    public void clear() {
        byUuid.clear();
        byName.clear();
    }

    private boolean fresh(PlayerSnapshot s) {
        return System.currentTimeMillis() - s.takenAtMillis() <= lifetimeMillis.getAsLong();
    }

    private void prune() {
        for (Iterator<PlayerSnapshot> it = byUuid.values().iterator(); it.hasNext(); ) {
            PlayerSnapshot s = it.next();
            if (!fresh(s)) {
                it.remove();
                if (s.name() != null) {
                    byName.remove(s.name().toLowerCase(Locale.ROOT), s.uuid());
                }
            }
        }
    }
}
