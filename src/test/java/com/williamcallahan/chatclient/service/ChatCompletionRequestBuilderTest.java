package com.williamcallahan.chatclient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.williamcallahan.chatclient.ReasoningEffort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatCompletionRequestBuilderTest {

    @Test
    void forwardsEveryConfiguredEffortAsTheExactStandardField() {
        for (ReasoningEffort effort : ReasoningEffort.values()) {
            ChatCompletionCreateParams params = requestFor(Optional.of(effort), "gpt-5");

            assertEquals(
                effort.wireValue(),
                params.reasoningEffort().orElseThrow().asString()
            );
        }
    }

    @Test
    void leavesReasoningFieldAbsentWhenEffortIsOmitted() {
        ChatCompletionCreateParams params = requestFor(Optional.empty(), "gpt-5");

        assertTrue(params.reasoningEffort().isEmpty());
    }

    @Test
    void preservesNonGptModelAliasWithoutReasoningPolicy() {
        String modelAlias = "anthropic/claude-sonnet-4";
        ChatCompletionCreateParams params = requestFor(Optional.of(ReasoningEffort.HIGH), modelAlias);

        assertEquals(modelAlias, params.model().asString());
        assertEquals("high", params.reasoningEffort().orElseThrow().asString());
    }

    private ChatCompletionCreateParams requestFor(
        Optional<ReasoningEffort> effort,
        String model
    ) {
        return new ChatCompletionRequestBuilder(() -> effort)
            .create()
            .model(model)
            .addUserMessage("test")
            .build();
    }
}
