package com.fleoxxzy.mcpvpBanAnimation.platform.spigot;

import com.fleoxxzy.mcpvpBanAnimation.text.MiniMessageLite;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Spigot text: MiniMessage is rendered to legacy section codes (including §x hex) by {@link MiniMessageLite}.
 */
public final class SpigotText implements TextAdapter {

    @Override
    public void send(CommandSender target, String message) {
        if (target == null || message == null || message.isEmpty()) {
            return;
        }
        String legacy = toLegacy(message);
        // Spigot renders section hex (§x§r§r§g§g§b§b) in plain sendMessage, so components aren't needed.
        target.sendMessage(legacy);
    }

    @Override
    public void broadcast(String message, String permission) {
        if (message == null || message.isEmpty()) {
            return;
        }
        boolean filtered = permission != null && !permission.isBlank();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!filtered || player.hasPermission(permission)) {
                send(player, message);
            }
        }
        send(Bukkit.getConsoleSender(), message);
    }

    @Override
    public void kick(Player player, String message) {
        player.kickPlayer(toLegacy(message));
    }

    @Override
    public void kickLegacy(Player player, String legacyReason) {
        player.kickPlayer(legacyReason == null ? "" : legacyReason);
    }

    @Override
    public void setDisplayText(TextDisplay display, String message) {
        display.setText(toLegacy(message));
    }

    @Override
    public void setDeathMessage(PlayerDeathEvent event, String message) {
        event.setDeathMessage(message == null ? null : toLegacy(message));
    }

    @Override
    public String toLegacy(String message) {
        return MiniMessageLite.toLegacy(message);
    }

    @Override
    public String toPlain(String message) {
        return MiniMessageLite.toPlain(message);
    }
}
