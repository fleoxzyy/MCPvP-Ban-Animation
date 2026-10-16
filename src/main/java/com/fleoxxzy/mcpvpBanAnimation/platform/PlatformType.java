package com.fleoxxzy.mcpvpBanAnimation.platform;

public enum PlatformType {
    SPIGOT("Spigot"),
    PAPER("Paper"),
    FOLIA("Folia");

    private final String displayName;

    PlatformType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Paper and Folia share the Adventure + region scheduler API surface. */
    public boolean isPaperBased() {
        return this != SPIGOT;
    }
}
