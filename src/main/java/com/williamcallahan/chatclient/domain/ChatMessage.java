package com.williamcallahan.chatclient.domain;

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
    public static Builder builder(
        String id,
        String conversationId,
        int index,
        Role role,
        Source source,
        String content,
        OffsetDateTime createdAt,
        String model,
        String provider
    ) {
        return new Builder(
            id,
            conversationId,
            index,
            role,
            source,
            content,
            createdAt,
            model,
            provider
        );
    }

    public enum Source {
        @JsonProperty("user-input") USER_INPUT,
        @JsonProperty("llm-output") LLM_OUTPUT,
        @JsonProperty("system") SYSTEM,
        @JsonProperty("tool-output") TOOL_OUTPUT,
        @JsonProperty("internal") INTERNAL,
        @JsonProperty("local") LOCAL
    }

    public static final class Builder {

        private final String id;
        private final String conversationId;
        private final int index;
        private final Role role;
        private final Source source;
        private final String content;
        private final OffsetDateTime createdAt;
        private final String model;
        private final String provider;
        private String providerMessageId;
        private List<ToolCall> toolCalls;
        private String toolCallId;
        private Map<String, Object> usage;
        private Map<String, Object> error;

        private Builder(
            String id,
            String conversationId,
            int index,
            Role role,
            Source source,
            String content,
            OffsetDateTime createdAt,
            String model,
            String provider
        ) {
            this.id = id;
            this.conversationId = conversationId;
            this.index = index;
            this.role = role;
            this.source = source;
            this.content = content;
            this.createdAt = createdAt;
            this.model = model;
            this.provider = provider;
        }

        public Builder providerMessageId(String providerMessageId) {
            this.providerMessageId = providerMessageId;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public Builder usage(Map<String, Object> usage) {
            this.usage = usage;
            return this;
        }

        public Builder error(Map<String, Object> error) {
            this.error = error;
            return this;
        }

        public ChatMessage build() {
            return new ChatMessage(
                id,
                conversationId,
                index,
                role,
                source,
                content,
                createdAt,
                model,
                provider,
                providerMessageId,
                toolCalls,
                toolCallId,
                usage,
                error
            );
        }
    }
}
