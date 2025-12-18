package com.williamcallahan.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A single message in a conversation. */
public record ChatMessage(
    @JsonProperty("id") String id,
    @JsonProperty("conversation_id") String conversationId,
    @JsonProperty("index") int index,

    @JsonProperty("role") Role role,
    @JsonProperty("source") Source source,
    @JsonProperty("content") String content,

    @JsonProperty("created_at") OffsetDateTime createdAt,

    @JsonProperty("model") String model,
    @JsonProperty("provider") String provider,

    @JsonProperty("provider_message_id") String providerMessageId,

    @JsonProperty("tool_calls") List<ToolCall> toolCalls,
    @JsonProperty("tool_call_id") String toolCallId,

    @JsonProperty("usage") Map<String, Object> usage,
    @JsonProperty("error") Map<String, Object> error
) {
    public enum Source {
        @JsonProperty("user-input") USER_INPUT,
        @JsonProperty("llm-output") LLM_OUTPUT,
        @JsonProperty("system") SYSTEM,
        @JsonProperty("tool-output") TOOL_OUTPUT,
        @JsonProperty("internal") INTERNAL
    }
}
