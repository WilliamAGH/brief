package com.williamcallahan.chatclient.ui.maps;

import com.williamcallahan.chatclient.service.AppleMapsService.PlaceResult;
import com.williamcallahan.chatclient.ui.PaletteItem;
import com.williamcallahan.chatclient.ui.TuiTheme;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a PlaceResult for display in the place selection overlay.
 */
public record PlaceItem(PlaceResult place) implements PaletteItem {

    private static final int MAX_DESC_LENGTH = 50;
    private static final int MIN_ADDRESS_PARTS_FOR_REDUCTION = 3;

    @Override
    public String name() {
        return getCategoryIcon(place.category()) + " " + place.name();
    }

    @Override
    public String description() {
        String addr = place.address();
        if (addr == null || addr.isBlank()) {
            String cat = place.category();
            return (cat != null && !cat.isBlank()) ? cat : "";
        }
        return formatAddressForDisplay(addr, MAX_DESC_LENGTH);
    }

    /**
     * Formats an address for display, intelligently truncating if needed.
     * Address format expected: "street, city, state postal, country" (any part may be missing).
     * Strategy: if too long, drop street first, then truncate from end.
     */
    private static String formatAddressForDisplay(String address, int maxLen) {
        if (address == null || address.isBlank()) return "";

        // Clean up and normalize
        String clean = address.trim();

        // If it fits, return as-is
        if (clean.length() <= maxLen) {
            return clean;
        }

        // Split into parts
        String[] parts = clean.split(",\\s*");
        if (parts.length <= 1) {
            // Single part, just truncate
            return truncate(clean, maxLen);
        }

        // Try without the first part (street address) if we have 3+ parts
        if (parts.length >= MIN_ADDRESS_PARTS_FOR_REDUCTION) {
            List<String> reduced = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isBlank()) {
                    reduced.add(parts[i].trim());
                }
            }
            String shorter = String.join(", ", reduced);
            if (shorter.length() <= maxLen) {
                return shorter;
            }
            // Still too long, truncate
            return truncate(shorter, maxLen);
        }

        // 2 parts, try to fit
        List<String> nonEmpty = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                nonEmpty.add(part.trim());
            }
        }
        String joined = String.join(", ", nonEmpty);
        return truncate(joined, maxLen);
    }

    private static String truncate(String s, int maxLen) {
        return TuiTheme.truncate(s, maxLen);
    }

    static String getCategoryIcon(String category) {
        if (category == null || category.isBlank()) return "📍";
        String lower = category.toLowerCase();
        if (lower.contains("coffee") || lower.contains("cafe")) return "☕";
        if (lower.contains("restaurant") || lower.contains("food")) return "🍽";
        if (lower.contains("bar") || lower.contains("pub")) return "🍺";
        if (lower.contains("hotel") || lower.contains("lodging")) return "🏨";
        if (lower.contains("gas") || lower.contains("fuel")) return "⛽";
        if (lower.contains("hospital") || lower.contains("medical")) return "🏥";
        if (lower.contains("pharmacy") || lower.contains("drug")) return "💊";
        if (lower.contains("bank") || lower.contains("atm")) return "🏦";
        if (lower.contains("grocery") || lower.contains("supermarket")) return "🛒";
        if (lower.contains("gym") || lower.contains("fitness")) return "💪";
        if (lower.contains("park")) return "🌳";
        if (lower.contains("museum") || lower.contains("gallery")) return "🏛";
        if (lower.contains("theater") || lower.contains("cinema")) return "🎭";
        if (lower.contains("airport")) return "✈";
        if (lower.contains("train") || lower.contains("station")) return "🚉";
        if (lower.contains("school") || lower.contains("university")) return "🎓";
        if (lower.contains("library")) return "📚";
        if (lower.contains("shop") || lower.contains("store") || lower.contains("mall")) return "🛍";
        return "📍";
    }
}
