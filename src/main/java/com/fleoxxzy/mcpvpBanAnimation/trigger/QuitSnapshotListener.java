package com.fleoxxzy.mcpvpBanAnimation.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Remembers every leaving player for a short time, so bans reported after the disconnect
 * (async API hooks) can still play the animation where the player stood. The quit event runs on
 * the player's own thread (also on Folia), so reading their location and equipment here is safe.
 */
public final class QuitSnapshotListener implements Listener {

    private final TriggerService triggers;

    public QuitSnapshotListener(TriggerService triggers) {
        this.triggers = triggers;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        triggers.playerQuit(event.getPlayer());
    }
}
