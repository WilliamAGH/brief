package com.williamcallahan.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Role of a message sender in a conversation. */
public enum Role {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    /** Serializes to JSON as the string value (e.g., "user" not "USER"). */
    @JsonValue
    public String getValue() {
        return value;
    }

    /** Deserializes from JSON string to enum constant. */
    @JsonCreator
    public static Role fromString(String value) {
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
