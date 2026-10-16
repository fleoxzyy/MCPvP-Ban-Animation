package com.fleoxxzy.mcpvpBanAnimation.config;

import java.util.Locale;

public enum ReplicaType {
    AUTO,
    MANNEQUIN,
    ARMORSTAND;

    public static ReplicaType parse(String raw) {
        if (raw == null) {
            return AUTO;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT).replace("_", "").replace("-", "");
        for (ReplicaType t : values()) {
            if (t.name().equals(v)) {
                return t;
            }
        }
        return AUTO;
    }
}
