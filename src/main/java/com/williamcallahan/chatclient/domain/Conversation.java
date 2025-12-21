package com.williamcallahan.chatclient.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZoneOffset;

import lombok.Builder;
import lombok.Data;

/** Container for a chat session with metadata and message history. */
@Data
@Builder
public class Conversation {

    public enum Provider { OPENAI, OPENROUTER, LMSTUDIO }
    public enum ApiFamily { RESPONSES, CHAT_COMPLETIONS }

    // The version of the conversation object from the API provider
    @JsonProperty("version")
    @Builder.Default
    private int version = 1;

    // The unique identifier for the conversation, uuidv4
    @JsonProperty("id")
    private String id; 

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("provider")
    @Builder.Default
    private Provider provider = Provider.LMSTUDIO;

    @JsonProperty("api_family")
    @Builder.Default
    private ApiFamily apiFamily = ApiFamily.CHAT_COMPLETIONS;

    @JsonProperty("default_model")
    @Builder.Default
    private String defaultModel = "gpt-oss-120b";

    @JsonProperty("metadata")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @JsonProperty("messages")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    public List<ChatMessage> messages() { return messages; }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
