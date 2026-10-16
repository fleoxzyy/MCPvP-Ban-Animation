package com.fleoxxzy.mcpvpBanAnimation.text;

import java.util.Map;

/**
 * {@code {key}} substitution. Values are escaped so a reason like {@code "<red>lol"} cannot inject tags.
 */
public final class Placeholders {

    private Placeholders() {
    }

    public static String apply(String template, Map<String, String> values) {
        if (template == null || template.isEmpty() || values == null || values.isEmpty()) {
            return template == null ? "" : template;
        }
        StringBuilder out = new StringBuilder(template.length() + 32);
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i + 1);
                if (end > i + 1) {
                    String key = template.substring(i + 1, end);
                    String value = values.get(key);
                    if (value != null) {
                        out.append(escapeTags(value));
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** Builds a map from alternating key/value arguments. */
    public static Map<String, String> of(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders.of needs key/value pairs");
        }
        Map<String, String> map = new java.util.HashMap<>(keyValues.length);
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1] == null ? "" : keyValues[i + 1]);
        }
        return map;
    }

    /** Escapes MiniMessage tag openers, so the value always renders literally. */
    public static String escapeTags(String value) {
        if (value.indexOf('<') < 0 && value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<' || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }
}
