package com.fleoxxzy.mcpvpBanAnimation.platform.spigot;

import com.fleoxxzy.mcpvpBanAnimation.text.MiniMessageLite;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.TextDisplay;

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
    public void setDisplayText(TextDisplay display, String message) {
        display.setText(toLegacy(message));
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
