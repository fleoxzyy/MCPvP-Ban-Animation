package com.fleoxxzy.mcpvpBanAnimation.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A small, dependency-free renderer for the MiniMessage subset that Spigot can display with
 * legacy section codes. Supported:
 * <ul>
 *   <li>named colors, {@code <#RRGGBB>}, {@code <color:...>} / {@code <c:...>}</li>
 *   <li>decorations and their aliases: bold/b, italic/i/em, underlined/u, strikethrough/st, obfuscated/obf,
 *       negation via {@code <!bold>} or {@code <bold:false>}</li>
 *   <li>{@code <reset>}, {@code <newline>}/{@code <br>}, {@code <gradient:...>}, {@code <rainbow[:phase]>}</li>
 *   <li>closing tags, and escaping with {@code \<}</li>
 * </ul>
 * Interactive tags (click, hover, font, ...) are dropped. Unknown tags are kept as literal text, like MiniMessage does.
 * Legacy {@code &} codes are converted first, so mixed input works.
 */
public final class MiniMessageLite {

    private static final char SECTION = '\u00A7';

    private static final int OBFUSCATED = 1;
    private static final int BOLD = 2;
    private static final int STRIKETHROUGH = 4;
    private static final int UNDERLINED = 8;
    private static final int ITALIC = 16;
    private static final int[] DECORATION_BITS = {OBFUSCATED, BOLD, STRIKETHROUGH, UNDERLINED, ITALIC};
    private static final char[] DECORATION_CODES = {'k', 'l', 'm', 'n', 'o'};

    private static final Map<String, LColor> NAMED = new HashMap<>();
    private static final Map<String, Integer> DECORATIONS = new HashMap<>();
    private static final Set<String> STRIPPED_PAIRED = Set.of(
            "click", "hover", "insert", "insertion", "font", "transition", "shadow", "pride");
    private static final Set<String> STRIPPED_SINGLE = Set.of(
            "key", "lang", "tr", "translate", "lang_or", "tr_or", "translate_or", "selector", "sel",
            "score", "nbt", "data", "keybind", "sprite", "head", "newline_placeholder");

    static {
        named("black", '0', 0x000000);
        named("dark_blue", '1', 0x0000AA);
        named("dark_green", '2', 0x00AA00);
        named("dark_aqua", '3', 0x00AAAA);
        named("dark_red", '4', 0xAA0000);
        named("dark_purple", '5', 0xAA00AA);
        named("gold", '6', 0xFFAA00);
        named("gray", '7', 0xAAAAAA);
        named("dark_gray", '8', 0x555555);
        named("blue", '9', 0x5555FF);
        named("green", 'a', 0x55FF55);
        named("aqua", 'b', 0x55FFFF);
        named("red", 'c', 0xFF5555);
        named("light_purple", 'd', 0xFF55FF);
        named("yellow", 'e', 0xFFFF55);
        named("white", 'f', 0xFFFFFF);
        NAMED.put("grey", NAMED.get("gray"));
        NAMED.put("dark_grey", NAMED.get("dark_gray"));

        DECORATIONS.put("bold", BOLD);
        DECORATIONS.put("b", BOLD);
        DECORATIONS.put("italic", ITALIC);
        DECORATIONS.put("i", ITALIC);
        DECORATIONS.put("em", ITALIC);
        DECORATIONS.put("underlined", UNDERLINED);
        DECORATIONS.put("u", UNDERLINED);
        DECORATIONS.put("strikethrough", STRIKETHROUGH);
        DECORATIONS.put("st", STRIKETHROUGH);
        DECORATIONS.put("obfuscated", OBFUSCATED);
        DECORATIONS.put("obf", OBFUSCATED);
    }

    private MiniMessageLite() {
    }

    /** MiniMessage (or legacy) → legacy section-coded text. */
    public static String toLegacy(String input) {
        return render(input, true);
    }

    /** MiniMessage (or legacy) → plain text. */
    public static String toPlain(String input) {
        return render(input, false);
    }

    private static void named(String name, char code, int rgb) {
        NAMED.put(name, new LColor(code, rgb));
    }

    // ---------------------------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------------------------

    private static String render(String input, boolean legacy) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        List<Token> tokens = tokenize(LegacyBridge.toMiniMessage(input));
        StringBuilder out = new StringBuilder(input.length() + 16);
        Deque<Frame> stack = new ArrayDeque<>();
        Emitter emitter = new Emitter(out, legacy);

        for (int t = 0; t < tokens.size(); t++) {
            Token token = tokens.get(t);
            if (token instanceof Text text) {
                Style style = stack.isEmpty() ? Style.EMPTY : stack.peek().style;
                text.value.codePoints().forEach(cp -> emitter.emit(cp, stack.isEmpty() ? null : stack.peek(), style));
                continue;
            }
            if (token instanceof Close close) {
                popTo(stack, close.name);
                continue;
            }
            Open open = (Open) token;
            String name = open.name;
            Style current = stack.isEmpty() ? Style.EMPTY : stack.peek().style;
            ColorRun inheritedRun = stack.isEmpty() ? null : stack.peek().run;

            if (name.equals("reset")) {
                stack.clear();
                continue;
            }
            if (name.equals("newline") || name.equals("br")) {
                out.append('\n');
                continue;
            }
            if (STRIPPED_SINGLE.contains(name)) {
                continue;
            }
            if (STRIPPED_PAIRED.contains(name)) {
                stack.push(new Frame(name, current, inheritedRun));
                continue;
            }

            LColor color = resolveColorTag(name, open.args);
            if (color != null) {
                stack.push(new Frame(name, current.withColor(color), null));
                continue;
            }

            boolean negated = name.startsWith("!");
            String decoName = negated ? name.substring(1) : name;
            Integer bit = DECORATIONS.get(decoName);
            if (bit != null) {
                boolean on = !negated && !(open.args.size() == 1 && open.args.get(0).equalsIgnoreCase("false"));
                stack.push(new Frame(decoName, current.withDecoration(bit, on), inheritedRun));
                continue;
            }

            if (name.equals("gradient") || name.equals("rainbow")) {
                int length = countVisible(tokens, t + 1, name);
                ColorRun run = name.equals("gradient") ? gradient(open.args, length) : rainbow(open.args, length);
                stack.push(new Frame(name, current, run));
                continue;
            }

            // Unknown tag: render literally, like MiniMessage.
            Style style = current;
            Frame top = stack.peek();
            open.raw.codePoints().forEach(cp -> emitter.emit(cp, top, style));
        }
        return out.toString();
    }

    private static void popTo(Deque<Frame> stack, String rawName) {
        String name = rawName.startsWith("!") ? rawName.substring(1) : rawName;
        String canonical = canonical(name);
        for (Frame frame : stack) {
            if (canonical(frame.name).equals(canonical)) {
                while (!stack.isEmpty()) {
                    if (stack.pop() == frame) {
                        return;
                    }
                }
                return;
            }
        }
    }

    private static String canonical(String name) {
        if (name.startsWith("#") || NAMED.containsKey(name) || name.equals("c") || name.equals("colour")) {
            return "color";
        }
        Integer bit = DECORATIONS.get(name);
        if (bit != null) {
            return "deco" + bit;
        }
        return name;
    }

    private static LColor resolveColorTag(String name, List<String> args) {
        if (name.equals("color") || name.equals("colour") || name.equals("c")) {
            return args.isEmpty() ? null : parseColor(args.get(0));
        }
        return parseColor(name);
    }

    private static LColor parseColor(String value) {
        if (value == null) {
            return null;
        }
        String v = value.toLowerCase(Locale.ROOT);
        LColor named = NAMED.get(v);
        if (named != null) {
            return named;
        }
        if (v.length() == 7 && v.charAt(0) == '#') {
            try {
                return new LColor('\0', Integer.parseInt(v.substring(1), 16));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** Counts visible code points from {@code start} until the tag closing the gradient opened before it. */
    private static int countVisible(List<Token> tokens, int start, String name) {
        int depth = 0;
        int count = 0;
        for (int i = start; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token instanceof Text text) {
                count += text.value.codePointCount(0, text.value.length());
            } else if (token instanceof Open open) {
                if (open.name.equals(name)) {
                    depth++;
                } else if (open.name.equals("reset")) {
                    break;
                }
            } else if (token instanceof Close close && close.name.equals(name)) {
                if (depth == 0) {
                    break;
                }
                depth--;
            }
        }
        return Math.max(count, 1);
    }

    private static ColorRun gradient(List<String> args, int length) {
        List<Integer> stops = new ArrayList<>();
        for (String arg : args) {
            LColor c = parseColor(arg);
            if (c != null) {
                stops.add(c.rgb);
            }
        }
        if (stops.isEmpty()) {
            stops.add(0xFFFFFF);
            stops.add(0x000000);
        } else if (stops.size() == 1) {
            stops.add(stops.get(0));
        }
        int[] colors = new int[length];
        for (int i = 0; i < length; i++) {
            double pos = length == 1 ? 0 : (double) i / (length - 1);
            double scaled = pos * (stops.size() - 1);
            int seg = Math.min((int) Math.floor(scaled), stops.size() - 2);
            colors[i] = lerp(stops.get(seg), stops.get(seg + 1), scaled - seg);
        }
        return new ColorRun(colors);
    }

    private static ColorRun rainbow(List<String> args, int length) {
        boolean reversed = false;
        double phase = 0;
        if (!args.isEmpty()) {
            String a = args.get(0);
            if (a.startsWith("!")) {
                reversed = true;
                a = a.substring(1);
            }
            try {
                phase = a.isEmpty() ? 0 : Integer.parseInt(a) / 10.0;
            } catch (NumberFormatException ignored) {
                phase = 0;
            }
        }
        int[] colors = new int[length];
        for (int i = 0; i < length; i++) {
            int idx = reversed ? length - 1 - i : i;
            float hue = (float) (((double) idx / length + phase) % 1.0);
            colors[i] = hsbToRgb(hue, 1f, 1f);
        }
        return new ColorRun(colors);
    }

    /** HSB → RGB without java.awt, because slim server JREs may not include java.desktop. */
    private static int hsbToRgb(float hue, float saturation, float brightness) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        int sector = (int) h;
        float f = h - sector;
        float p = brightness * (1f - saturation);
        float q = brightness * (1f - saturation * f);
        float t = brightness * (1f - saturation * (1f - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ---------------------------------------------------------------------------------------------
    // Tokenizer
    // ---------------------------------------------------------------------------------------------

    private static List<Token> tokenize(String s) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length() && (s.charAt(i + 1) == '<' || s.charAt(i + 1) == '\\')) {
                text.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '<') {
                int end = findTagEnd(s, i + 1);
                if (end > i + 1) {
                    String inner = s.substring(i + 1, end);
                    if (isPlausibleTag(inner)) {
                        if (!text.isEmpty()) {
                            tokens.add(new Text(text.toString()));
                            text.setLength(0);
                        }
                        tokens.add(parseTag(inner, s.substring(i, end + 1)));
                        i = end + 1;
                        continue;
                    }
                }
            }
            text.append(c);
            i++;
        }
        if (!text.isEmpty()) {
            tokens.add(new Text(text.toString()));
        }
        return tokens;
    }

    /** Finds the closing '>' and skips over quoted arguments ('...' or "..."). */
    private static int findTagEnd(String s, int from) {
        char quote = 0;
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '<') {
                return -1;
            } else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    private static boolean isPlausibleTag(String inner) {
        String body = inner.startsWith("/") ? inner.substring(1) : inner;
        if (body.isEmpty()) {
            return false;
        }
        char first = body.charAt(0);
        return first == '#' || first == '!' || Character.isLetter(first);
    }

    private static Token parseTag(String inner, String raw) {
        boolean closing = inner.startsWith("/");
        String body = closing ? inner.substring(1) : inner;
        if (body.endsWith("/")) {
            body = body.substring(0, body.length() - 1);
        }
        List<String> parts = splitArgs(body);
        String name = parts.get(0).toLowerCase(Locale.ROOT);
        if (closing) {
            return new Close(name);
        }
        return new Open(name, parts.subList(1, parts.size()), raw);
    }

    private static List<String> splitArgs(String body) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == ':') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    // ---------------------------------------------------------------------------------------------
    // Model
    // ---------------------------------------------------------------------------------------------

    private sealed interface Token permits Text, Open, Close {
    }

    private record Text(String value) implements Token {
    }

    private record Open(String name, List<String> args, String raw) implements Token {
    }

    private record Close(String name) implements Token {
    }

    /** A named legacy color ({@code code != 0}) or a hex color. */
    private record LColor(char code, int rgb) {
    }

    private record Style(LColor color, int decorations) {
        static final Style EMPTY = new Style(null, 0);

        Style withColor(LColor c) {
            return new Style(c, decorations);
        }

        Style withDecoration(int bit, boolean on) {
            return new Style(color, on ? decorations | bit : decorations & ~bit);
        }
    }

    /** Per-character colors for a gradient/rainbow. {@code cursor} advances as characters are emitted. */
    private static final class ColorRun {
        final int[] colors;
        int cursor;

        ColorRun(int[] colors) {
            this.colors = colors;
        }

        int next() {
            int c = colors[Math.min(cursor, colors.length - 1)];
            cursor++;
            return c;
        }
    }

    private record Frame(String name, Style style, ColorRun run) {
    }

    /** Writes characters and inserts the smallest set of section codes needed when the style changes. */
    private static final class Emitter {
        private final StringBuilder out;
        private final boolean legacy;
        private LColor lastColor;
        private int lastDecorations;

        Emitter(StringBuilder out, boolean legacy) {
            this.out = out;
            this.legacy = legacy;
        }

        void emit(int codePoint, Frame top, Style style) {
            if (legacy) {
                LColor color = style.color;
                ColorRun run = findRun(top);
                if (run != null) {
                    color = new LColor('\0', run.next());
                }
                applyStyle(color, style.decorations);
            }
            out.appendCodePoint(codePoint);
        }

        private ColorRun findRun(Frame top) {
            // Decoration frames inherit their parent's run. A color tag inside a gradient starts a
            // frame with no run, so its own color wins.
            return top == null ? null : top.run;
        }

        private void applyStyle(LColor color, int decorations) {
            boolean colorChanged = !Objects.equals(color, lastColor);
            boolean decorationRemoved = (lastDecorations & ~decorations) != 0;
            if (colorChanged || decorationRemoved) {
                if (color == null) {
                    if (lastColor != null || lastDecorations != 0) {
                        out.append(SECTION).append('r');
                    }
                } else {
                    appendColor(color);
                }
                appendDecorations(decorations);
            } else if (decorations != lastDecorations) {
                appendDecorations(decorations & ~lastDecorations);
            }
            lastColor = color;
            lastDecorations = decorations;
        }

        private void appendColor(LColor color) {
            if (color.code != '\0') {
                out.append(SECTION).append(color.code);
                return;
            }
            String hex = String.format(Locale.ROOT, "%06x", color.rgb & 0xFFFFFF);
            out.append(SECTION).append('x');
            for (int i = 0; i < 6; i++) {
                out.append(SECTION).append(hex.charAt(i));
            }
        }

        private void appendDecorations(int decorations) {
            for (int i = 0; i < DECORATION_BITS.length; i++) {
                if ((decorations & DECORATION_BITS[i]) != 0) {
                    out.append(SECTION).append(DECORATION_CODES[i]);
                }
            }
        }
    }
}
