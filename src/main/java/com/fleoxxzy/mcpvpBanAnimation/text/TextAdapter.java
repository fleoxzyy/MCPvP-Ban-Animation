package com.fleoxxzy.mcpvpBanAnimation.text;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.TextDisplay;

/**
 * Text output across platforms. Every {@code message} argument is MiniMessage (users write
 * MiniMessage in the config). Legacy {@code &} codes and {@code &#RRGGBB} are accepted too.
 * Implementations convert it to Adventure components (Paper/Folia) or legacy section text (Spigot).
 */
public interface TextAdapter {

    void send(CommandSender target, String message);

    void setDisplayText(TextDisplay display, String message);

    /** Renders MiniMessage to legacy section text, for APIs that only take strings. */
    String toLegacy(String message);

    /** Renders MiniMessage to plain text without any formatting. */
    String toPlain(String message);
}
