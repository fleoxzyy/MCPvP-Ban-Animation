package com.fleoxxzy.mcpvpBanAnimation.hook;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;
import com.fleoxxzy.mcpvpBanAnimation.core.Durations;
import com.fleoxxzy.mcpvpBanAnimation.core.Reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

/**
 * LibertyBans: {@code PostPunishEvent} on its Omnibus event bus, fired after a punishment is
 * stored. Reached by reflection, so no LibertyBans classes are needed at compile time.
 */
final class LibertyBansHook implements BanHook {

    private static final String OMNIBUS_PROVIDER = "space.arim.omnibus.OmnibusProvider";
    private static final String EVENT = "space.arim.libertybans.api.event.PostPunishEvent";
    private static final String CONSUMER = "space.arim.omnibus.events.EventConsumer";
    private static final String PRIORITIES = "space.arim.omnibus.events.ListenerPriorities";
    private static final UUID WILDCARD = new UUID(0, 0);

    private final HookSupport support;
    private Object eventBus;
    private Object registration;

    LibertyBansHook(HookSupport support) {
        this.support = support;
    }

    @Override
    public HookTarget target() {
        return HookTarget.LIBERTYBANS;
    }

    @Override
    public String pluginName() {
        return "LibertyBans";
    }

    @Override
    public void enable() throws Exception {
        Class<?> provider = require(OMNIBUS_PROVIDER);
        Class<?> eventType = require(EVENT);
        Class<?> consumerType = require(CONSUMER);
        byte priority = require(PRIORITIES).getField("HIGHEST").getByte(null);

        Object omnibus = provider.getMethod("getOmnibus").invoke(null);
        eventBus = omnibus.getClass().getMethod("getEventBus").invoke(omnibus);
        Object consumer = Proxy.newProxyInstance(consumerType.getClassLoader(), new Class<?>[]{consumerType},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "accept" -> {
                            if (args != null && args.length == 1) {
                                handle(args[0]);
                            }
                            return null;
                        }
                        case "hashCode" -> {
                            return System.identityHashCode(proxy);
                        }
                        case "equals" -> {
                            return args != null && proxy == args[0];
                        }
                        case "toString" -> {
                            return "MCPvPBanAnimation LibertyBans listener";
                        }
                        default -> {
                            return null;
                        }
                    }
                });
        Method register = publicMethod(eventBus, "registerListener", Class.class, byte.class, consumerType);
        registration = register.invoke(eventBus, eventType, priority, consumer);
    }

    private void handle(Object event) {
        Object punishment = Reflect.invokeNoArgs(event, "getPunishment");
        Object type = Reflect.invokeNoArgs(punishment, "getType");
        if (!(type instanceof Enum<?> t) || !t.name().equals("BAN")) {
            return;
        }
        boolean silent = Boolean.TRUE.equals(Reflect.invokeNoArgs(event, "isSilent"));

        Object victim = Reflect.invokeNoArgs(punishment, "getVictim");
        Object victimType = Reflect.invokeNoArgs(victim, "getType");
        String kind = victimType instanceof Enum<?> v ? v.name() : "";
        UUID uuid = null;
        InetAddress ip = null;
        if (kind.equals("PLAYER") || kind.equals("COMPOSITE")) {
            Object id = Reflect.invokeNoArgs(victim, "getUUID");
            uuid = id instanceof UUID u && !u.equals(WILDCARD) ? u : null;
        }
        if (kind.equals("ADDRESS") || kind.equals("COMPOSITE")) {
            Object address = Reflect.invokeNoArgs(Reflect.invokeNoArgs(victim, "getAddress"), "toInetAddress");
            ip = address instanceof InetAddress a ? a : null;
        }
        boolean ipBan = !kind.equals("PLAYER");

        Object operator = Reflect.invokeNoArgs(punishment, "getOperator");
        Object operatorId = Reflect.invokeNoArgs(operator, "getUUID");
        String staff = operatorId instanceof UUID u ? HookSupport.nameOf(u) : "Console";

        Object reason = Reflect.invokeNoArgs(punishment, "getReason");
        Object end = Reflect.invokeNoArgs(punishment, "getEndDate");
        String duration = end instanceof Instant e && !e.equals(Instant.MAX) ? Durations.remaining(e.toEpochMilli()) : null;

        support.debug("LibertyBans ban: " + uuid + " / " + ip + " by " + staff);
        support.report(target(), uuid, null, uuid == null ? ip : null, staff,
                reason == null ? null : String.valueOf(reason), duration, ipBan, silent);
    }

    @Override
    public void disable() {
        if (eventBus == null || registration == null) {
            return;
        }
        try {
            for (Method m : eventBus.getClass().getMethods()) {
                if (m.getName().equals("unregisterListener") && m.getParameterCount() == 1) {
                    publicMethod(eventBus, "unregisterListener", m.getParameterTypes()[0]).invoke(eventBus, registration);
                    break;
                }
            }
        } catch (Throwable ignored) {
            // LibertyBans already shut down.
        }
        eventBus = null;
        registration = null;
    }

    private static Class<?> require(String name) throws ClassNotFoundException {
        Class<?> c = Reflect.findClass(name);
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

    /** The method declared on a public interface of {@code target}, so it can be invoked across class loaders. */
    private static Method publicMethod(Object target, String name, Class<?>... params) throws NoSuchMethodException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Class<?> iface : type.getInterfaces()) {
                try {
                    return iface.getMethod(name, params);
                } catch (NoSuchMethodException ignored) {
                    // Next interface.
                }
            }
        }
        return target.getClass().getMethod(name, params);
    }
}
