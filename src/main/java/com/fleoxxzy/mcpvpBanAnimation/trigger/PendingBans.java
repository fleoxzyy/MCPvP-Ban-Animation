package com.fleoxxzy.mcpvpBanAnimation.trigger;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Bans we saw coming (a ban command was run) but whose kick has not arrived yet. When the kick
 * arrives it is recognised as a ban, and these details fill in the placeholders. A pending ban
 * that is never followed by a kick (the command failed or was denied) just expires. Thread-safe.
 */
public final class PendingBans {

    private record Entry(BanDetails details, long createdAt) {
    }

    private final Map<String, Entry> byName = new ConcurrentHashMap<>();
    private final Map<UUID, Entry> byUuid = new ConcurrentHashMap<>();
    private final LongSupplier lifetimeMillis;

    public PendingBans(LongSupplier lifetimeMillis) {
        this.lifetimeMillis = lifetimeMillis;
    }

    public void add(BanDetails details) {
        prune();
        Entry e = new Entry(details, System.currentTimeMillis());
        if (details.name() != null) {
            byName.put(details.name().toLowerCase(Locale.ROOT), e);
        }
        if (details.uuid() != null) {
            byUuid.put(details.uuid(), e);
        }
    }

    /** Removes and returns the pending ban for this player, or null if there is none (or it expired). */
    public BanDetails take(UUID uuid, String name) {
        Entry a = uuid == null ? null : byUuid.remove(uuid);
        Entry b = name == null ? null : byName.remove(name.toLowerCase(Locale.ROOT));
        Entry newest = a == null ? b : (b == null || a.createdAt >= b.createdAt ? a : b);
        return newest != null && fresh(newest) ? newest.details : null;
    }

    /** True if a fresh pending ban exists, without consuming it. */
    public boolean has(UUID uuid, String name) {
        Entry a = uuid == null ? null : byUuid.get(uuid);
        Entry b = name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
        return (a != null && fresh(a)) || (b != null && fresh(b));
    }

    public void clear() {
        byName.clear();
        byUuid.clear();
    }

    private boolean fresh(Entry e) {
        return System.currentTimeMillis() - e.createdAt <= lifetimeMillis.getAsLong();
    }

    private void prune() {
        byName.values().removeIf(e -> !fresh(e));
        byUuid.values().removeIf(e -> !fresh(e));
    }
}
