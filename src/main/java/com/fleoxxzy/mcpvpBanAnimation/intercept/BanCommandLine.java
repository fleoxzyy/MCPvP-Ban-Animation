package com.fleoxxzy.mcpvpBanAnimation.intercept;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Splits a ban command line ({@code /tempban Steve 7d -s Hacking}) into its parts. Pure string
 * work, no server access.
 */
public final class BanCommandLine {

    /** {@code 30m}, {@code 1h30m}, {@code 7d}, {@code 2w}, {@code 1mo}, {@code 1y}... */
    private static final Pattern DURATION = Pattern.compile(
            "(?i)^(\\d+(s|sec|secs|m|min|mins|h|hr|hrs|d|day|days|w|wk|week|weeks|mo|month|months|y|yr|year|years))+$");
    private static final Set<String> PERMANENT = Set.of("perm", "permanent", "forever", "-1");
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    public final String label;
    public final String target;
    public final String duration;
    public final String reason;
    public final List<String> flags;
    public final boolean ipBan;

    private BanCommandLine(String label, String target, String duration, String reason, List<String> flags,
                           boolean ipBan) {
        this.label = label;
        this.target = target;
        this.duration = duration;
        this.reason = reason;
        this.flags = flags;
        this.ipBan = ipBan;
    }

    /** Parses a command line, with or without the leading slash. Returns null if there is no target. */
    public static BanCommandLine parse(String commandLine) {
        if (commandLine == null) {
            return null;
        }
        String line = commandLine.trim();
        while (line.startsWith("/")) {
            line = line.substring(1);
        }
        String[] parts = line.split("\\s+");
        if (parts.length < 2 || parts[0].isEmpty()) {
            return null;
        }
        String label = parts[0].toLowerCase(Locale.ROOT);
        List<String> flags = new ArrayList<>();
        List<String> rest = new ArrayList<>();
        String target = null;
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) {
                continue;
            }
            // Flags like -s may appear anywhere (LiteBans allows them before the target too).
            if (p.length() > 1 && p.charAt(0) == '-' && !p.equals("-1") && Character.isLetter(p.charAt(p.length() - 1))) {
                flags.add(p.toLowerCase(Locale.ROOT));
                continue;
            }
            if (target == null) {
                target = p;
            } else {
                rest.add(p);
            }
        }
        if (target == null) {
            return null;
        }
        String duration = null;
        if (!rest.isEmpty()) {
            String first = rest.get(0);
            if (DURATION.matcher(first).matches()) {
                duration = first;
                rest.remove(0);
            } else if (PERMANENT.contains(first.toLowerCase(Locale.ROOT))) {
                rest.remove(0);
            }
        }
        String reason = rest.isEmpty() ? null : String.join(" ", rest);
        boolean ipBan = label.contains("ip") || isIp(target);
        return new BanCommandLine(label, target, duration, reason, List.copyOf(flags), ipBan);
    }

    /** True if the target looks like an IPv4 address instead of a player name. */
    public static boolean isIp(String target) {
        return target != null && IPV4.matcher(target).matches();
    }

    public boolean hasAnyFlag(Set<String> wanted) {
        for (String f : flags) {
            if (wanted.contains(f)) {
                return true;
            }
        }
        return false;
    }
}
