package com.fleoxxzy.mcpvpBanAnimation.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A comparable Minecraft version. Understands both the classic {@code 1.21.x} scheme and the
 * year-based {@code 26.x} scheme (26.1, 26.1.2, 26.3 ...). Year-based versions always sort after 1.x.
 */
public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

    public static final MinecraftVersion UNKNOWN = new MinecraftVersion(0, 0, 0);
    public static final MinecraftVersion V1_21 = new MinecraftVersion(1, 21, 0);
    public static final MinecraftVersion V1_21_9 = new MinecraftVersion(1, 21, 9);
    public static final MinecraftVersion V26_3 = new MinecraftVersion(26, 3, 0);

    /**
     * Parses strings like {@code 1.21}, {@code 1.21.4-R0.1-SNAPSHOT}, {@code 26.1}, {@code 26.1.2-R0.1-SNAPSHOT}
     * or {@code 26.3.build.159-beta}. Reads leading dot-separated numbers and stops at the first
     * non-numeric part.
     */
    public static Optional<MinecraftVersion> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.trim();
        int dash = text.indexOf('-');
        if (dash >= 0) {
            text = text.substring(0, dash);
        }
        List<Integer> parts = new ArrayList<>(3);
        for (String piece : text.split("\\.")) {
            if (piece.isEmpty() || !piece.chars().allMatch(Character::isDigit) || parts.size() == 3) {
                break;
            }
            try {
                parts.add(Integer.parseInt(piece));
            } catch (NumberFormatException e) {
                break;
            }
        }
        if (parts.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(new MinecraftVersion(parts.get(0), parts.get(1), parts.size() > 2 ? parts.get(2) : 0));
    }

    /** True for the year-based release scheme (26.x and later). */
    public boolean isYearBased() {
        return major >= 26;
    }

    public boolean isAtLeast(MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        return isAtLeast(new MinecraftVersion(major, minor, patch));
    }

    public boolean isKnown() {
        return !equals(UNKNOWN);
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, o.minor);
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override
    public String toString() {
        return patch == 0 ? major + "." + minor : major + "." + minor + "." + patch;
    }
}
