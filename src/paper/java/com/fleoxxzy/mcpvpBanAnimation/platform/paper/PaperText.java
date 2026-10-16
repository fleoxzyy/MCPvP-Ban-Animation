package com.fleoxxzy.mcpvpBanAnimation.platform.paper;

import com.fleoxxzy.mcpvpBanAnimation.text.LegacyBridge;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Adventure / MiniMessage text for Paper and Folia. Legacy codes in the input are converted to tags first. */
public final class PaperText implements TextAdapter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public Component render(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        String mm = LegacyBridge.toMiniMessage(message);
        try {
            return miniMessage.deserialize(mm);
        } catch (Throwable t) {
            // Strict-mode or malformed input: never fail to show *something*.
            return Component.text(mm);
        }
    }

    @Override
    public void send(CommandSender target, String message) {
        if (target == null || message == null || message.isEmpty()) {
            return;
        }
        target.sendMessage(render(message));
    }

    @Override
    public void broadcast(String message, String permission) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Component component = render(message);
        boolean filtered = permission != null && !permission.isBlank();
        // Sending a chat message is thread-safe on Paper and Folia, so no scheduler hop is needed.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!filtered || player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    @Override
    public void kick(Player player, String message) {
        player.kick(render(message));
    }

    @Override
    public void kickLegacy(Player player, String legacyReason) {
        player.kick(LEGACY.deserialize(legacyReason == null ? "" : legacyReason));
    }

    @Override
    public void setDisplayText(TextDisplay display, String message) {
        display.text(render(message));
    }

    @Override
    public void setDeathMessage(PlayerDeathEvent event, String message) {
        event.deathMessage(message == null ? null : render(message));
    }

    @Override
    public String toLegacy(String message) {
        return LEGACY.serialize(render(message));
    }

    @Override
    public String toPlain(String message) {
        return PlainTextComponentSerializer.plainText().serialize(render(message));
    }
}
