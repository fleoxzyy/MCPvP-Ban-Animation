package com.fleoxxzy.mcpvpBanAnimation.hook;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig.HookTarget;
import com.fleoxxzy.mcpvpBanAnimation.core.Durations;
import litebans.api.Entry;
import litebans.api.Events;

import java.net.InetAddress;
import java.util.UUID;

/**
 * LiteBans: {@code Events.Listener#entryAdded}, fired (usually async) for every new punishment,
 * also ones made on another server of the network. Only {@code ban} entries are used.
 */
final class LiteBansHook implements BanHook {

    private final HookSupport support;
    private Events.Listener listener;

    LiteBansHook(HookSupport support) {
        this.support = support;
    }

    @Override
    public HookTarget target() {
        return HookTarget.LITEBANS;
    }

    @Override
    public String pluginName() {
        return "LiteBans";
    }

    @Override
    public void enable() {
        listener = new Events.Listener() {
            @Override
            public void entryAdded(Entry entry) {
                handle(entry);
            }
        };
        Events.get().register(listener);
    }

    private void handle(Entry entry) {
        if (entry == null || !"ban".equalsIgnoreCase(entry.getType())) {
            return;
        }
        UUID uuid = HookSupport.parseUuid(entry.getUuid());
        InetAddress ip = HookSupport.parseIp(entry.getIp());
        String duration = entry.isPermanent() || entry.getDateEnd() <= 0 ? null : Durations.remaining(entry.getDateEnd());
        support.debug("LiteBans ban: " + entry.getUuid() + " / " + entry.getIp() + " by " + entry.getExecutorName());
        support.report(target(), uuid, null, uuid == null ? ip : null, staff(entry.getExecutorName()),
                entry.getReason(), duration, entry.isIpban(), entry.isSilent());
    }

    private static String staff(String executor) {
        return executor == null || executor.isBlank() ? null : executor;
    }

    @Override
    public void disable() {
        if (listener != null) {
            try {
                Events.get().unregister(listener);
            } catch (Throwable ignored) {
                // LiteBans already shut down.
            }
            listener = null;
        }
    }
}
