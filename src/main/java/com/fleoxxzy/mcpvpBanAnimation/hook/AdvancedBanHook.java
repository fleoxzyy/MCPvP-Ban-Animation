package com.fleoxxzy.mcpvpBanAnimation.hook;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;
import com.fleoxxzy.mcpvpBanAnimation.core.Durations;
import com.fleoxxzy.mcpvpBanAnimation.core.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * AdvancedBan: its Bukkit {@code PunishmentEvent}, fired right after a punishment is created.
 * Reached by reflection, so no AdvancedBan classes are needed at compile time.
 */
final class AdvancedBanHook implements BanHook {

    private static final String EVENT = "me.leoko.advancedban.bukkit.event.PunishmentEvent";

    private final Plugin plugin;
    private final HookSupport support;
    private final Listener listener = new Listener() {
    };

    AdvancedBanHook(Plugin plugin, HookSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    @Override
    public HookTarget target() {
        return HookTarget.ADVANCEDBAN;
    }

    @Override
    public String pluginName() {
        return "AdvancedBan";
    }

    @Override
    public void enable() {
        Class<?> type = Reflect.findClass(EVENT);
        if (type == null || !Event.class.isAssignableFrom(type)) {
            throw new IllegalStateException(EVENT + " not found");
        }
        Bukkit.getPluginManager().registerEvent(type.asSubclass(Event.class), listener, EventPriority.MONITOR,
                (l, event) -> {
                    if (type.isInstance(event)) {
                        handle(Reflect.invokeNoArgs(event, "getPunishment"));
                    }
                }, plugin, false);
    }

    private void handle(Object punishment) {
        if (punishment == null) {
            return;
        }
        Object type = Reflect.invokeNoArgs(punishment, "getType");
        Object basic = Reflect.invokeNoArgs(type, "getBasic");
        if (!(basic instanceof Enum<?> b) || !b.name().equals("BAN")) {
            return;
        }
        boolean ipBan = Boolean.TRUE.equals(Reflect.invokeNoArgs(type, "isIpOrientated"));
        String name = string(Reflect.invokeNoArgs(punishment, "getName"));
        // For player bans this is the (undashed) UUID, for IP bans it is the address.
        String target = string(Reflect.invokeNoArgs(punishment, "getUuid"));
        UUID uuid = HookSupport.parseUuid(target);
        String operator = string(Reflect.invokeNoArgs(punishment, "getOperator"));
        String reason = string(Reflect.invokeNoArgs(punishment, "getReason"));
        Object end = Reflect.invokeNoArgs(punishment, "getEnd");
        String duration = end instanceof Long e && e > 0 ? Durations.remaining(e) : null;

        support.debug("AdvancedBan ban: " + name + " (" + target + ") by " + operator);
        boolean nameIsIp = HookSupport.parseIp(name) != null;
        support.report(target(), uuid, nameIsIp ? null : name,
                uuid == null ? HookSupport.parseIp(nameIsIp ? name : target) : null,
                "CONSOLE".equalsIgnoreCase(operator) ? "Console" : operator, reason, duration, ipBan, false);
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(listener);
    }
}
