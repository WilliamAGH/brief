package com.williamcallahan.chatclient;

/** Canonical mouse mode policy for terminal interaction. */
public enum MouseMode {
    NATIVE("0"),
    ALL("1"),
    WHEEL("wheel"),
    SELECT("select");

    private final String value;

    MouseMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean enablesAllMotion() {
        return this == ALL;
    }

    public boolean enablesCellMotion() {
        return this == WHEEL || this == SELECT;
    }

    public boolean enablesMouseClicks() {
        return this != NATIVE;
    }

    public boolean enablesTargetCursor() {
        return this != NATIVE;
    }

    public boolean enablesHistorySelection() {
        return this == SELECT;
    }

    public boolean enablesSelectionCursor() {
        return this == SELECT;
    }

    public boolean enablesSelectionAutoScroll() {
        return this == SELECT;
    }

    public static MouseMode parse(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return SELECT;
        }
        String mode = rawMode.trim().toLowerCase();
        return switch (mode) {
            case "0", "off", "native", "false" -> NATIVE;
            case "1", "all", "true" -> ALL;
            case "wheel", "btn", "buttons" -> WHEEL;
            case "select" -> SELECT;
            default -> SELECT;
        };
    }

    public static MouseMode fromEnvironment() {
        return parse(System.getenv("BRIEF_MOUSE"));
    }
}
