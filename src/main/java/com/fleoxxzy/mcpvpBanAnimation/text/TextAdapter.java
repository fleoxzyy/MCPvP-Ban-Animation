package com.fleoxxzy.mcpvpBanAnimation.text;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Text output across platforms. Every {@code message} argument is MiniMessage (users write
 * MiniMessage in the config). Legacy {@code &} codes and {@code &#RRGGBB} are accepted too.
 * Implementations convert it to Adventure components (Paper/Folia) or legacy section text (Spigot).
 */
public interface TextAdapter {

    void send(CommandSender target, String message);

    /** Sends to every online player (and the console). If {@code permission} is not blank, only to holders. */
    void broadcast(String message, String permission);

    /** Kicks with a MiniMessage reason. Must be called on the player's owning thread. */
    void kick(Player player, String message);

    /** Kicks with an already-rendered legacy reason (e.g. a reason captured from another plugin). */
    void kickLegacy(Player player, String legacyReason);

    void setDisplayText(TextDisplay display, String message);

    /** Sets the death message. {@code null} hides it. */
    void setDeathMessage(PlayerDeathEvent event, String message);

    /** Renders MiniMessage to legacy section text, for APIs that only take strings (BanList reasons). */
    String toLegacy(String message);

    /** Renders MiniMessage to plain text without any formatting. */
    String toPlain(String message);
}
