package com.williamcallahan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.*;
import com.williamcallahan.domain.ChatMessage;
import com.williamcallahan.domain.Conversation;
import com.williamcallahan.domain.Role;
import com.williamcallahan.domain.ToolCall;
import com.williamcallahan.service.tools.Tool;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Executes tool calls requested by the LLM, feeding results back until completion. */
public final class ToolExecutor {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};
    private static final int MAX_ITERATIONS = 3;

    private final ChatCompletionService chat;
    private final List<Tool> tools;

    public ToolExecutor(ChatCompletionService chat, List<Tool> tools) {
        this.chat = chat;
        this.tools = tools;
    }

    public String respond(Conversation conversation, String modelOverride) {
        ChatCompletionCreateParams.Builder builder = buildParams(conversation, modelOverride);
        for (Tool tool : tools) {
            builder.addFunctionTool(tool.definition());
        }

        String model = builder.build().model().toString();

        for (int step = 0; step < MAX_ITERATIONS; step++) {
            ChatCompletion completion = chat.complete(builder.build());
            ChatCompletionMessage msg = completion.choices().stream()
                .findFirst()
                .map(c -> c.message())
                .orElse(null);
            if (msg == null) return "";

            List<ChatCompletionMessageToolCall> toolCalls = msg.toolCalls().orElse(List.of());
            if (toolCalls.isEmpty()) {
                return msg.content().orElse("");
            }

            builder.addMessage(msg.toParam());
            List<ChatCompletionMessageToolCall> functionCalls = toolCalls.stream()
                .filter(ChatCompletionMessageToolCall::isFunction)
                .toList();

            saveAssistantMessage(conversation, msg, functionCalls, model);

            for (ChatCompletionMessageToolCall tc : functionCalls) {
                executeAndSave(conversation, builder, tc, model);
            }
        }
        return "ERROR: tool loop did not resolve to a final assistant message.";
    }

    private void saveAssistantMessage(Conversation conversation, ChatCompletionMessage msg,
            List<ChatCompletionMessageToolCall> functionCalls, String model) {
        List<ToolCall> domainCalls = functionCalls.stream().map(this::toDomainToolCall).toList();
        conversation.addMessage(new ChatMessage(
            "asst_" + shortId(), conversation.getId(), conversation.getMessages().size(),
            Role.ASSISTANT, ChatMessage.Source.LLM_OUTPUT, msg.content().orElse(""),
            OffsetDateTime.now(ZoneOffset.UTC), model, conversation.getProvider().name().toLowerCase(),
            null, domainCalls, null, null, null));
    }

    private void executeAndSave(Conversation conversation, ChatCompletionCreateParams.Builder builder,
            ChatCompletionMessageToolCall toolCall, String model) {
        var fn = toolCall.asFunction();
        String toolCallId = fn.id();
        String toolName = fn.function().name();
        Map<String, Object> args = parseArgs(fn.function().arguments());

        Tool tool = tools.stream().filter(t -> t.name().equals(toolName)).findFirst().orElse(null);

        Object result;
        try {
            if (tool == null) throw new IllegalArgumentException("Unknown tool: " + toolName);
            result = tool.execute(args);
        } catch (Exception e) {
            result = Map.of("error", e.getMessage() == null ? "Error" : e.getMessage());
        }

        String resultText;
        try { resultText = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result); }
        catch (Exception e) { resultText = String.valueOf(result); }

        conversation.addMessage(new ChatMessage(
            "tool_" + shortId(), conversation.getId(), conversation.getMessages().size(),
            Role.TOOL, ChatMessage.Source.TOOL_OUTPUT, resultText,
            OffsetDateTime.now(ZoneOffset.UTC), model, conversation.getProvider().name().toLowerCase(),
            null, null, toolCallId, null, null));

        builder.addMessage(ChatCompletionToolMessageParam.builder()
            .toolCallId(toolCallId).contentAsJson(result).build());
    }

    private ToolCall toDomainToolCall(ChatCompletionMessageToolCall tc) {
        var fn = tc.asFunction();
        return new ToolCall(fn.id(), fn.id(), fn.function().name(),
            parseArgs(fn.function().arguments()), ToolCall.Status.PENDING, null, null);
    }

    private ChatCompletionCreateParams.Builder buildParams(Conversation conversation, String modelOverride) {
        ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder();
        b.model(modelOverride != null && !modelOverride.isBlank() ? modelOverride : conversation.getDefaultModel());
        b.temperature(0.3);

        List<ChatMessage> messages = conversation.getMessages();
        ChatMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);

        for (ChatMessage m : messages) {
            if (shouldSkipMessage(m, last)) continue;
            appendMessage(b, m);
        }
        return b;
    }

    private boolean shouldSkipMessage(ChatMessage m, ChatMessage last) {
        boolean hasContent = m.content() != null && !m.content().isBlank();
        boolean hasToolCalls = m.toolCalls() != null && !m.toolCalls().isEmpty();
        boolean isToolResponse = m.role() == Role.TOOL && m.toolCallId() != null;

        if (!hasContent && !hasToolCalls && !isToolResponse) return true;
        return m.source() == ChatMessage.Source.INTERNAL && m != last;
    }

    private void appendMessage(ChatCompletionCreateParams.Builder b, ChatMessage m) {
        String content = contentOrEmpty(m);
        switch (m.role()) {
            case SYSTEM -> { if (!content.isBlank()) b.addSystemMessage(content); }
            case USER -> { if (m.source() == ChatMessage.Source.USER_INPUT && !content.isBlank()) b.addUserMessage(content); }
            case ASSISTANT -> appendAssistantMessage(b, m, content);
            case TOOL -> appendToolMessage(b, m, content);
        }
    }

    private void appendAssistantMessage(ChatCompletionCreateParams.Builder b, ChatMessage m, String content) {
        if (m.source() != ChatMessage.Source.LLM_OUTPUT) return;

        List<ToolCall> toolCalls = m.toolCalls();
        boolean hasToolCalls = toolCalls != null && !toolCalls.isEmpty();

        if (hasToolCalls) {
            var ab = ChatCompletionAssistantMessageParam.builder();
            if (!content.isBlank()) ab.content(content);
            for (ToolCall tc : toolCalls) {
                ab.addToolCall(buildToolCall(tc));
            }
            b.addMessage(ab.build());
        } else if (!content.isBlank()) {
            b.addAssistantMessage(content);
        }
    }

    private ChatCompletionMessageFunctionToolCall buildToolCall(ToolCall tc) {
        return ChatCompletionMessageFunctionToolCall.builder()
            .id(tc.id())
            .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                .name(tc.name())
                .arguments(toJson(tc.arguments()))
                .build())
            .build();
    }

    private void appendToolMessage(ChatCompletionCreateParams.Builder b, ChatMessage m, String content) {
        String toolCallId = m.toolCallId();
        if (toolCallId == null || content.isBlank()) return;
        b.addMessage(ChatCompletionToolMessageParam.builder()
            .toolCallId(toolCallId)
            .content(content)
            .build());
    }

    private static String contentOrEmpty(ChatMessage m) {
        String c = m.content();
        return c != null ? c : "";
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, Object> parseArgs(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return JSON.readValue(json, MAP_REF); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid JSON: " + json, e); }
    }

    private static String toJson(Map<String, Object> args) {
        if (args == null || args.isEmpty()) return "{}";
        try {
            return JSON.writeValueAsString(args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize tool arguments", e);
        }
    }
}
