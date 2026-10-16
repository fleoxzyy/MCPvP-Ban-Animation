package com.fleoxxzy.mcpvpBanAnimation.intercept;

import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.config.HookMode;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;
import com.fleoxxzy.mcpvpBanAnimation.core.Reflect;
import com.fleoxxzy.mcpvpBanAnimation.trigger.BanDetails;
import com.fleoxxzy.mcpvpBanAnimation.trigger.BanSource;
import com.fleoxxzy.mcpvpBanAnimation.trigger.TriggerService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches ban commands from players and the console. It never cancels or changes a command: it
 * only remembers a short-lived pending ban, so the kick that follows is recognised as a ban
 * and the placeholders get the staff, reason and duration.
 *
 * <p>Runs at MONITOR and ignores cancelled commands, so we only see commands that will really run.
 */
public final class CommandInterceptor implements Listener {

    private final ConfigManager configs;
    private final TriggerService triggers;
    private final Logger logger;

    public CommandInterceptor(ConfigManager configs, TriggerService triggers, Logger logger) {
        this.configs = configs;
        this.triggers = triggers;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        handle(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        handle(event.getSender(), event.getCommand());
    }

    private void handle(CommandSender sender, String commandLine) {
        try {
            inspect(sender, commandLine);
        } catch (Throwable t) {
            // Never let our listener disturb the command.
            logger.log(Level.WARNING, "Error while inspecting a command", t);
        }
    }

    private void inspect(CommandSender sender, String commandLine) {
        PluginConfig cfg = configs.config();
        BanCommandLine line = BanCommandLine.parse(commandLine);
        if (line == null || !cfg.commandInterception().isBanCommand(line.label)) {
            return;
        }

        Command command = lookup(line.label);
        HookTarget source = sourceOf(command, line.label);
        HookMode mode = cfg.hookMode(source);

        List<Player> targets = resolveTargets(line);
        if (targets.isEmpty()) {
            debug(cfg, "Ban command /" + line.label + " for '" + line.target + "': no online target. Ignored.");
            return;
        }

        if (command != null && cfg.commandInterception().respectCommandPermission()
                && !command.testPermissionSilent(sender)) {
            debug(cfg, sender.getName() + " lacks the permission for /" + line.label + ". Ignored.");
            return;
        }

        boolean silent = cfg.commandInterception().skipSilent()
                && line.hasAnyFlag(cfg.commandInterception().silentFlags());
        for (Player target : targets) {
            BanDetails details = new BanDetails(target.getUniqueId(), target.getName(), sender.getName(),
                    line.reason, line.duration, line.ipBan, BanSource.COMMAND);
            if (mode == HookMode.OFF || silent) {
                triggers.suppressNextBan(details);
            } else if (mode != HookMode.KICK) {
                // COMMAND, and API (the command still carries the details if the hook misses).
                triggers.banCommandSeen(details);
            }
        }
    }

    /** The online players a ban command targets: by name, or everyone on that IP for IP bans. */
    private static List<Player> resolveTargets(BanCommandLine line) {
        List<Player> out = new ArrayList<>();
        if (BanCommandLine.isIp(line.target)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                InetSocketAddress address = p.getAddress();
                if (address != null && address.getAddress() != null
                        && line.target.equals(address.getAddress().getHostAddress())) {
                    out.add(p);
                }
            }
            return out;
        }
        Player p = Bukkit.getPlayerExact(line.target);
        if (p != null) {
            out.add(p);
        }
        return out;
    }

    /** The registered command for a label, via the server's command map (public on Paper and CraftBukkit). */
    private static Command lookup(String label) {
        try {
            Object map = Reflect.invokeNoArgs(Bukkit.getServer(), "getCommandMap");
            return map instanceof CommandMap cm ? cm.getCommand(label) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static HookTarget sourceOf(Command command, String label) {
        if (command instanceof PluginIdentifiableCommand owned) {
            try {
                return HookTarget.fromPluginName(owned.getPlugin().getName());
            } catch (Throwable ignored) {
                // Fall through to the label.
            }
        }
        int colon = label.indexOf(':');
        if (colon > 0) {
            return HookTarget.fromPluginName(label.substring(0, colon));
        }
        // No owning plugin: a vanilla (Brigadier) command such as /ban or /ban-ip.
        return command != null ? HookTarget.VANILLA : HookTarget.UNKNOWN;
    }

    private void debug(PluginConfig cfg, String message) {
        if (cfg.debug()) {
            logger.info("[debug] " + message);
        }
    }
}
