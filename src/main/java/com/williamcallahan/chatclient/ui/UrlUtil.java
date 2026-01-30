package com.williamcallahan.chatclient.ui;

import java.util.regex.Pattern;

final class UrlUtil {
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://|www\\.)[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*[a-zA-Z0-9+&@#/%=~_|-]$"
    );
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(:\\d+)?([/?#].*)?$"
    );

    private UrlUtil() {}

    static boolean isPotentialUrl(String token) {
        if (token == null) return false;
        String t = token.trim();
        // Simple heuristic: must contain a dot and start with common URL prefixes
        // or look like a domain name.
        t = stripPunctuation(t);
        if (t.isEmpty()) return false;

        return URL_PATTERN.matcher(t).matches()
            || DOMAIN_PATTERN.matcher(t).matches()
            || (t.contains(".") && (t.toLowerCase().startsWith("http") || t.toLowerCase().startsWith("www")));
    }

    static String normalizeUrl(String token) {
        if (token == null) return null;
        String t = stripPunctuation(token.trim());
        if (t.isEmpty()) return null;

        if (t.toLowerCase().startsWith("http://") || t.toLowerCase().startsWith("https://")) return t;
        if (t.toLowerCase().startsWith("www.")) return "https://" + t;

        if (DOMAIN_PATTERN.matcher(t).matches()) return "https://" + t;
        if (URL_PATTERN.matcher(t).matches()) {
            return t.contains("://") ? t : "https://" + t;
        }
        return null;
    }

    private static String stripPunctuation(String t) {
        return t.replaceAll("^[\"'\\[\\(<{`]+|[\"'\\]\\)>}`.,]+$", "");
    }
}
