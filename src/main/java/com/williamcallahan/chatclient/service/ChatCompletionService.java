package com.williamcallahan.chatclient.service;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import java.util.List;

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

    /**
     * Simple text completion for summarization and other single-turn tasks.
     *
     * @param prompt The user prompt
     * @param model  The model to use
     * @return The assistant's response text
     */
    public String complete(String prompt, String model) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(model)
            .messages(List.of(
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build()
                )
            ))
            .build();

        ChatCompletion completion = complete(params);
        if (completion.choices().isEmpty()) {
            return "";
        }
        var content = completion.choices().get(0).message().content();
        return content.orElse("");
    }
}
