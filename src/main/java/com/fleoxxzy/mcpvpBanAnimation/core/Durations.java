package com.fleoxxzy.mcpvpBanAnimation.core;

/** Formats the time left on a ban for the {@code {duration}} placeholder. */
public final class Durations {

    private Durations() {
    }

    /** "7d 3h", "1h 30m", "45m" for a ban expiring at {@code expiresAtMillis}. */
    public static String remaining(long expiresAtMillis) {
        long seconds = Math.max(0, (expiresAtMillis - System.currentTimeMillis()) / 1000);
        // Round up to the next minute, so a fresh "/tempban 1h" shows 1h instead of 59m.
        long minutes = (seconds + 59) / 60;
        long days = minutes / 1440;
        long hours = minutes % 1440 / 60;
        long mins = minutes % 60;
        StringBuilder out = new StringBuilder();
        if (days > 0) {
            out.append(days).append('d');
        }
        if (hours > 0) {
            out.append(out.isEmpty() ? "" : " ").append(hours).append('h');
        }
        if (mins > 0 && days == 0) {
            out.append(out.isEmpty() ? "" : " ").append(mins).append('m');
        }
        return out.isEmpty() ? "1m" : out.toString();
    }
}
