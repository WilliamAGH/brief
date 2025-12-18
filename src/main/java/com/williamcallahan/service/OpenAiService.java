package com.williamcallahan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.models.Model;
import com.williamcallahan.ConfigException;
import com.williamcallahan.domain.ChatMessage;
import com.williamcallahan.domain.Conversation;
import com.williamcallahan.domain.Role;
import com.williamcallahan.service.tools.Tool;
import com.williamcallahan.service.tools.WeatherForecastTool;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OpenAI chat completion client with a tool loop.
 *
 * Executes tool calls returned by the model and feeds results back until a final response.
 */
public final class OpenAiService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};

    private final OpenAIClient client;
    private final String baseUrl;
    private final List<Tool> tools;

    /**
     * Sets up the client using standard environment variables.
     *
     * The SDK relies on the built-in support for {@code OPENAI_API_KEY} and {@code OPENAI_BASE_URL},
     * so you don't have to manually configure keys here.
     */
    public OpenAiService() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String customBaseUrl = System.getenv("OPENAI_BASE_URL");

        if (apiKey == null || apiKey.isBlank()) {
            String target = (customBaseUrl == null || customBaseUrl.isBlank())
                ? "the OpenAI API (https://api.openai.com/v1)"
                : customBaseUrl;
            throw new ConfigException("""

                ┌─────────────────────────────────────────────────────────────────┐
                │  Missing API Key                                                │
                └─────────────────────────────────────────────────────────────────┘

                The OPENAI_API_KEY environment variable is required but was not set.

                Target endpoint: %s

                Quick fix:
                  1. Copy the example config:  cp .env-example .env
                  2. Edit .env and add your API key
                  3. Run again:  make run

                Get an API key:
                  → https://platform.openai.com/docs/quickstart

                For alternative providers (OpenRouter, LMStudio, Ollama):
                  → docs/environment-variables-api-keys.md

                """.formatted(target));
        }

        this.client = OpenAIOkHttpClient.builder()
            .fromEnv()
            .build();
        this.baseUrl = customBaseUrl;
        this.tools = List.of(new WeatherForecastTool());
    }

    public String baseUrl() {
        return baseUrl;
    }

    public OpenAIClient client() {
        return client;
    }

    /**
     * Fetches the list of available models from the API.
     *
     * @throws RuntimeException if the API call fails
     */
    public List<String> modelChoices() {
        return client.models()
            .list()
            .autoPager()
            .stream()
            .map(Model::id)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .toList();
    }

    public ChatCompletion createChatCompletion(Conversation conversation, String modelOverride) {
        return client.chat().completions().create(buildParams(conversation, modelOverride).build());
    }

    /**
     * Sends the conversation to the LLM and executes the tool loop.
     *
     * Recursively calls the API if the model decides to use a tool, up to 3 iterations.
     *
     * @param conversation The full conversation history.
     * @param modelOverride Optional model ID to use instead of the conversation default.
     * @return The final assistant text response, or an error message if something went wrong.
     */
    public String respond(Conversation conversation, String modelOverride) {
        ChatCompletionCreateParams.Builder b = buildParams(conversation, modelOverride);
        for (Tool t : tools) {
            b.addFunctionTool(t.definition());
        }

        String model = b.build().model().toString();

        for (int step = 0; step < 3; step++) {
            ChatCompletion completion = client.chat().completions().create(b.build());
            Optional<ChatCompletionMessage> msgOpt = completion.choices().stream().findFirst().map(c -> c.message());
            if (msgOpt.isEmpty()) return "";

            ChatCompletionMessage msg = msgOpt.get();
            List<ChatCompletionMessageToolCall> toolCalls = msg.toolCalls().orElse(List.of());
            if (toolCalls.isEmpty()) {
                return msg.content().orElse("");
            }

            b.addMessage(msg.toParam());

            for (ChatCompletionMessageToolCall tc : toolCalls) {
                if (tc == null || !tc.isFunction()) continue;

                var ftc = tc.asFunction();
                String toolCallId = ftc.id();
                String toolName = ftc.function().name();
                String argsJson = ftc.function().arguments();

                Map<String, Object> args = parseArgs(argsJson);
                Tool tool = tools.stream().filter(t -> t.name().equals(toolName)).findFirst().orElse(null);

                Object resultObj;
                try {
                    if (tool == null) throw new IllegalArgumentException("Unknown tool: " + toolName);
                    resultObj = tool.execute(args);
                } catch (Exception e) {
                    resultObj = Map.of("error", e.getMessage() == null ? "Error" : e.getMessage());
                }

                String toolText;
                try {
                    toolText = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(resultObj);
                } catch (Exception e) {
                    toolText = String.valueOf(resultObj);
                }

                conversation.addMessage(new ChatMessage(
                    "tool_%s".formatted(UUID.randomUUID().toString().substring(0, 8)),
                    conversation.getId(),
                    conversation.getMessages().size(),
                    Role.TOOL,
                    ChatMessage.Source.TOOL_OUTPUT,
                    toolText,
                    OffsetDateTime.now(ZoneOffset.UTC),
                    model,
                    conversation.getProvider().name().toLowerCase(),
                    null,
                    null,
                    toolCallId,
                    null,
                    null
                ));

                b.addMessage(ChatCompletionToolMessageParam.builder()
                    .toolCallId(toolCallId)
                    .contentAsJson(resultObj)
                    .build());
            }
        }

        return "ERROR: tool loop did not resolve to a final assistant message.";
    }

    private ChatCompletionCreateParams.Builder buildParams(Conversation conversation, String modelOverride) {
        ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder();

        String model = (modelOverride != null && !modelOverride.isBlank())
            ? modelOverride
            : conversation.getDefaultModel();
        b.model(model);
        b.temperature(0.3);

        List<ChatMessage> messages = conversation.getMessages();
        ChatMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        for (ChatMessage m : messages) {
            if (m == null || m.content() == null || m.content().isBlank()) continue;
            // INTERNAL messages are ephemeral routing hints (e.g. /weather). Include only if it's the most recent message.
            if (m.source() == ChatMessage.Source.INTERNAL && m != last) continue;

            String content = m.content();

            if (m.role() == Role.SYSTEM) {
                b.addSystemMessage(content);
            } else if (m.role() == Role.USER && m.source() == ChatMessage.Source.USER_INPUT) {
                b.addUserMessage(content);
            } else if (m.role() == Role.ASSISTANT && m.source() == ChatMessage.Source.LLM_OUTPUT) {
                b.addAssistantMessage(content);
            } else if (m.role() == Role.TOOL) {
                String toolCallId = m.toolCallId();
                if (toolCallId != null && !toolCallId.isBlank()) {
                    b.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCallId)
                        .content(content)
                        .build());
                }
            }
        }
        return b;
    }

    private static Map<String, Object> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) return Map.of();
        try {
            return JSON.readValue(argsJson, MAP_REF);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse tool arguments JSON: " + argsJson, e);
        }
    }

    /** Extracts the first text response from a completion result. */
    public String firstAssistantText(ChatCompletion completion) {
        if (completion == null) return "";
        return completion.choices().stream()
            .map(choice -> choice.message().content().orElse(""))
            .filter(s -> !s.isBlank())
            .findFirst()
            .orElse("");
    }
}
