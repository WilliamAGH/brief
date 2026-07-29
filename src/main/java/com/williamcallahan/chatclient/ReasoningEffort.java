package com.williamcallahan.chatclient;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Canonical reasoning-effort values accepted by Brief. */
public enum ReasoningEffort {
    NONE("none"),
    MINIMAL("minimal"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    XHIGH("xhigh"),
    MAX("max");

    private final String wireValue;

    ReasoningEffort(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    /** Returns no value for an omitted setting and validates an explicit setting. */
    public static Optional<ReasoningEffort> fromConfiguredValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(effort -> effort.wireValue.equals(normalized))
            .findFirst()
            .or(() -> {
                throw new ConfigException(
                    "Unsupported reasoning effort '" + value + "'. Supported values: " + supportedValues()
                );
            });
    }

    public static String supportedValues() {
        return Arrays.stream(values())
            .map(ReasoningEffort::wireValue)
            .collect(Collectors.joining(", "));
    }
}
