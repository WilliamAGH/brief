package com.williamcallahan.chatclient.service;

import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.williamcallahan.chatclient.Config;
import com.williamcallahan.chatclient.ReasoningEffort;
import java.util.Optional;
import java.util.function.Supplier;

/** Creates Chat Completions requests with the configured standard reasoning field. */
final class ChatCompletionRequestBuilder {

    private final Supplier<Optional<ReasoningEffort>> reasoningEffort;

    ChatCompletionRequestBuilder(Config config) {
        this(config::resolveReasoningEffort);
    }

    ChatCompletionRequestBuilder(Supplier<Optional<ReasoningEffort>> reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    ChatCompletionCreateParams.Builder create() {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder();
        reasoningEffort.get().ifPresent(effort ->
            builder.reasoningEffort(com.openai.models.ReasoningEffort.of(effort.wireValue()))
        );
        return builder;
    }
}
