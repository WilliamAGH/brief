package com.williamcallahan.chatclient.domain;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents a tool invocation requested by a model and executed by the app. */
public record ToolCall(
    @JsonProperty("id") String id,
    @JsonProperty("provider_id") String providerId,
    @JsonProperty("name") String name,
    @JsonProperty("arguments") Map<String, Object> arguments,
    @JsonProperty("status") Status status,
    @JsonProperty("result") Map<String, Object> result,
    @JsonProperty("error") Map<String, Object> error
) {
    public enum Status {
        @JsonProperty("pending") PENDING,
        @JsonProperty("completed") COMPLETED,
        @JsonProperty("error") ERROR
    }
}
