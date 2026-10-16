package com.fleoxxzy.mcpvpBanAnimation.text;

/**
 * Turns legacy formatting ({@code &c}, {@code §l}, {@code &#RRGGBB}, {@code §x§r§r§g§g§b§b}) into the
 * equivalent MiniMessage tags, so one MiniMessage renderer can handle mixed input.
 * Only valid codes are converted; a lone {@code &} (as in "Tom & Jerry") stays as it is.
 */
public final class LegacyBridge {

    private static final String CODES = "0123456789abcdefklmnor";
    private static final String[] COLOR_TAGS = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
    };

    private LegacyBridge() {
    }

    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty() || (input.indexOf('&') < 0 && input.indexOf('§') < 0)) {
            return input == null ? "" : input;
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        // In legacy text a color code clears bold/italic/etc. MiniMessage keeps them, so emit a
        // <reset> before a color when a legacy decoration is still active.
        boolean decorationActive = false;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < input.length()) {
                // &#RRGGBB
                if (input.charAt(i + 1) == '#' && i + 8 <= input.length() && isHex(input, i + 2, 6)) {
                    if (decorationActive) {
                        out.append("<reset>");
                        decorationActive = false;
                    }
                    out.append("<#").append(input, i + 2, i + 8).append('>');
                    i += 8;
                    continue;
                }
                // §x§R§R§G§G§B§B (Bungee format)
                char next = Character.toLowerCase(input.charAt(i + 1));
                if (next == 'x' && i + 14 <= input.length()) {
                    String hex = readRepeatedHex(input, i + 2, c);
                    if (hex != null) {
                        if (decorationActive) {
                            out.append("<reset>");
                            decorationActive = false;
                        }
                        out.append("<#").append(hex).append('>');
                        i += 14;
                        continue;
                    }
                }
                int code = CODES.indexOf(next);
                if (code >= 0) {
                    if (code < 16) {
                        if (decorationActive) {
                            out.append("<reset>");
                            decorationActive = false;
                        }
                        out.append('<').append(COLOR_TAGS[code]).append('>');
                    } else {
                        switch (next) {
                            case 'k' -> out.append("<obfuscated>");
                            case 'l' -> out.append("<bold>");
                            case 'm' -> out.append("<strikethrough>");
                            case 'n' -> out.append("<underlined>");
                            case 'o' -> out.append("<italic>");
                            default -> out.append("<reset>");
                        }
                        decorationActive = next != 'r';
                    }
                    i += 2;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String readRepeatedHex(String s, int start, char marker) {
        StringBuilder hex = new StringBuilder(6);
        for (int k = 0; k < 6; k++) {
            int pos = start + k * 2;
            if (s.charAt(pos) != marker) {
                return null;
            }
            char h = s.charAt(pos + 1);
            if (Character.digit(h, 16) < 0) {
                return null;
            }
            hex.append(h);
        }
        return hex.toString();
    }

    private static boolean isHex(String s, int start, int len) {
        for (int k = start; k < start + len; k++) {
            if (Character.digit(s.charAt(k), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
