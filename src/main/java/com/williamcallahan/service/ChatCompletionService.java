package com.williamcallahan.service;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/** Chat completions API wrapper. */
public final class ChatCompletionService {

    private final OpenAiService openAi;

    public ChatCompletionService(OpenAiService openAi) {
        this.openAi = openAi;
    }

    /**
     * Sends a chat completion request.
     *
     * @param params completion parameters (model, messages, tools, etc.)
     * @return completion response; never null per OpenAI SDK contract
     * @throws com.openai.core.http.HttpRequestException on network or API errors
     */
    public ChatCompletion complete(ChatCompletionCreateParams params) {
        return openAi.client().chat().completions().create(params);
    }
}
