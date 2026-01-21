package com.williamcallahan.chatclient.service;

import com.williamcallahan.chatclient.domain.ChatMessage;
import com.williamcallahan.chatclient.domain.Conversation;

/**
 * Estimates token counts for text and conversations.
 * Uses character-based approximation (~4 chars per token for English).
 * Can be replaced with tiktoken-java for exact counts.
 */
public final class TokenCounter {

    private static final double CHARS_PER_TOKEN = 4.0;
    private static final double WORDS_PER_TOKEN = 0.75; // ~1.33 tokens per word

    private TokenCounter() {}

    /**
     * Estimates token count for text.
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * Estimates token count for a conversation (all visible messages).
     */
    public static int estimateTokens(Conversation conversation) {
        if (conversation == null) return 0;
        return conversation.getMessages().stream()
            .filter(m -> m != null && m.source() != ChatMessage.Source.INTERNAL)
            .mapToInt(m -> estimateTokens(m.content()))
            .sum();
    }

    /**
     * Converts word count to estimated token count.
     */
    public static int wordsToTokens(int words) {
        return (int) Math.ceil(words / WORDS_PER_TOKEN);
    }

    /**
     * Converts token count to estimated word count.
     */
    public static int tokensToWords(int tokens) {
        return (int) (tokens * WORDS_PER_TOKEN);
    }

    /**
     * Counts words in text.
     */
    public static int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    /**
     * Counts lines in text.
     */
    public static int countLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) text.chars().filter(c -> c == '\n').count() + 1;
    }
}
