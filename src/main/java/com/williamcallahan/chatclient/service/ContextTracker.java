package com.williamcallahan.chatclient.service;

import com.williamcallahan.chatclient.domain.Conversation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Tracks context window usage for various models.
 * Provides remaining token calculations for OpenAI-compatible endpoints.
 */
public final class ContextTracker {

    private static final Map<String, Integer> MODEL_CONTEXT_SIZES = Map.ofEntries(
        // OpenAI models
        Map.entry("gpt-4o", 128_000),
        Map.entry("gpt-4o-mini", 128_000),
        Map.entry("gpt-4-turbo", 128_000),
        Map.entry("gpt-4", 8_192),
        Map.entry("gpt-3.5-turbo", 16_385),
        Map.entry("gpt-3.5", 16_385),
        // Anthropic models
        Map.entry("claude-3-opus", 200_000),
        Map.entry("claude-3-sonnet", 200_000),
        Map.entry("claude-3-haiku", 200_000),
        Map.entry("claude-3.5", 200_000),
        Map.entry("claude-3", 200_000),
        // Other common models
        Map.entry("llama-3", 8_192),
        Map.entry("mixtral", 32_768),
        Map.entry("mistral", 32_768)
    );

    /** Keys sorted by length descending for longest-match-first lookup. */
    private static final List<Map.Entry<String, Integer>> SORTED_ENTRIES =
        MODEL_CONTEXT_SIZES.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getKey().length()).reversed())
            .toList();

    private static final int DEFAULT_CONTEXT_SIZE = 8_192;

    private ContextTracker() {}

    /**
     * Returns the context window size for a model.
     * Uses partial matching (case-insensitive), checking longest keys first
     * to ensure specific model names take precedence over generic ones.
     */
    public static int getContextSize(String model) {
        if (model == null || model.isBlank()) return DEFAULT_CONTEXT_SIZE;
        String lowerModel = model.toLowerCase();
        return SORTED_ENTRIES.stream()
            .filter(e -> lowerModel.contains(e.getKey()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(DEFAULT_CONTEXT_SIZE);
    }

    /**
     * Calculates remaining tokens in the context window.
     */
    public static int remainingTokens(Conversation conversation, String model) {
        int used = TokenCounter.estimateTokens(conversation);
        int total = getContextSize(model);
        return Math.max(0, total - used);
    }

    /**
     * Checks if the conversation is approaching the context limit.
     *
     * @param threshold fraction of context used (e.g., 0.9 = 90% used)
     */
    public static boolean isNearLimit(Conversation conversation, String model, double threshold) {
        int remaining = remainingTokens(conversation, model);
        int total = getContextSize(model);
        double used = 1.0 - ((double) remaining / total);
        return used >= threshold;
    }

    /**
     * Returns context usage as a percentage (0-100).
     */
    public static int usagePercent(Conversation conversation, String model) {
        int used = TokenCounter.estimateTokens(conversation);
        int total = getContextSize(model);
        return (int) Math.min(100, (used * 100.0) / total);
    }
}
