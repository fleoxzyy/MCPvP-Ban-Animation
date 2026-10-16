package com.fleoxxzy.mcpvpBanAnimation.command;

import com.fleoxxzy.mcpvpBanAnimation.config.ConfigManager;
import com.fleoxxzy.mcpvpBanAnimation.config.HookMode;
import com.fleoxxzy.mcpvpBanAnimation.config.Messages;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;
import com.fleoxxzy.mcpvpBanAnimation.core.AnimationService;
import com.fleoxxzy.mcpvpBanAnimation.hook.HookManager;
import com.fleoxxzy.mcpvpBanAnimation.platform.Platform;
import com.fleoxxzy.mcpvpBanAnimation.replica.ReplicaStage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * {@code /mcpvpban}. Registered through plugin.yml, so it works on Spigot, Paper and Folia alike.
 */
public final class MCPvPBanCommand implements TabExecutor {

    private static final String PERM_TEST = "mcpvpban.test";
    private static final String PERM_ADMIN = "mcpvpban.admin";

    private final Plugin plugin;
    private final Platform platform;
    private final ConfigManager configs;
    private final AnimationService service;
    private final HookManager hooks;

    public MCPvPBanCommand(Plugin plugin, Platform platform, ConfigManager configs, AnimationService service,
                           HookManager hooks) {
        this.plugin = plugin;
        this.platform = platform;
        this.configs = configs;
        this.service = service;
        this.hooks = hooks;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = configs.messages();
        if (args.length == 0) {
            send(sender, messages.get("usage.main", "label", label));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "test" -> test(sender, label, args);
            case "reload" -> reload(sender);
            case "info" -> info(sender);
            default -> send(sender, messages.get("usage.main", "label", label));
        }
        return true;
    }

    private void test(CommandSender sender, String label, String[] args) {
        Messages messages = configs.messages();
        if (!sender.hasPermission(PERM_TEST)) {
            send(sender, messages.get("no-permission"));
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                send(sender, messages.get("player-not-found", "player", args[1]));
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            send(sender, messages.get("usage.test", "label", label));
            return;
        }
        service.playTest(target, sender);
    }

    private void reload(CommandSender sender) {
        Messages messages = configs.messages();
        if (!sender.hasPermission(PERM_ADMIN)) {
            send(sender, messages.get("no-permission"));
            return;
        }
        try {
            configs.load();
            platform.scheduler().executeGlobal(hooks::enableAll);
            send(sender, configs.messages().get("reloaded"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Reload failed", e);
            send(sender, messages.get("reload-failed", "error", String.valueOf(e.getMessage())));
        }
    }

    private void info(CommandSender sender) {
        Messages messages = configs.messages();
        if (!sender.hasPermission(PERM_ADMIN)) {
            send(sender, messages.get("no-permission"));
            return;
        }
        PluginConfig cfg = configs.config();
        List<String> hooked = hooks.activeHooks();
        String detection = cfg.hookMode(HookTarget.UNKNOWN) == HookMode.OFF ? "off" : "by ban command + kick";

        send(sender, messages.get("info.header"));
        line(sender, "Version", plugin.getDescription().getVersion());
        line(sender, "Server", platform.type().displayName() + " " + platform.version()
                + " (Java " + platform.javaVersion() + ")");
        line(sender, "Replica", ReplicaStage.usesMannequin(cfg.replica().type())
                ? "Mannequin (player model)" : "Armor stand");
        line(sender, "Hooked", hooked.isEmpty() ? "none" : String.join(", ", hooked));
        line(sender, "Other ban plugins", detection);
    }

    private void line(CommandSender sender, String key, String value) {
        send(sender, configs.messages().get("info.line", "key", key, "value", value));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission(PERM_TEST)) {
                out.add("test");
            }
            if (sender.hasPermission(PERM_ADMIN)) {
                out.add("reload");
                out.add("info");
            }
            return filter(out, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("test") && sender.hasPermission(PERM_TEST)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!(sender instanceof Player self) || self.canSee(p)) {
                    out.add(p.getName());
                }
            }
            return filter(out, args[1]);
        }
        return out;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }

    private void send(CommandSender to, String message) {
        platform.text().send(to, message);
    }
}
