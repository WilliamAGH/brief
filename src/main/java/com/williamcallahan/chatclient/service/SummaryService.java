package com.williamcallahan.chatclient.service;

import com.williamcallahan.chatclient.Config;
import com.williamcallahan.chatclient.domain.ChatMessage;
import com.williamcallahan.chatclient.domain.Conversation;
import com.williamcallahan.chatclient.domain.Role;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unified summarization service for paste content and context window management.
 * Provides DRY summarization logic used for:
 * <ul>
 *   <li>Long paste summarization (when input exceeds ~8k tokens)</li>
 *   <li>Context window trimming (when conversation approaches limit)</li>
 * </ul>
 */
public final class SummaryService {

    private static final String ENV_DISABLED = "BRIEF_SUMMARY_DISABLED";
    private static final String ENV_TARGET = "BRIEF_SUMMARY_TARGET_TOKENS";

    // Thresholds for placeholder usage
    private static final int PLACEHOLDER_MIN_LINES = 3;
    private static final int PLACEHOLDER_MIN_CHARS = 150;

    // Summarization tuning
    private static final double SUMMARY_WORD_RATIO = 0.85;
    private static final int MIN_SUMMARY_TOKENS = 500;
    private static final int MESSAGES_TO_PRESERVE = 4;
    private static final int CHARS_PER_TOKEN = 4;
    private static final String TRUNCATION_MARKER = "\n[... truncated]";

    private final ChatCompletionService completionService;
    private final Config config;

    public SummaryService(ChatCompletionService completionService, Config config) {
        this.completionService = completionService;
        this.config = config;
    }

    /**
     * Result of processing a paste.
     *
     * @param displayText   Text shown in the composer (placeholder or original)
     * @param actualText    Text sent to LLM (summarized if needed, or original)
     * @param wasSummarized Whether the content was summarized
     * @param wasTruncated  Whether summarization failed and content was truncated
     * @param lineCount     Number of lines in original content
     */
    public record PasteSummary(String displayText, String actualText, boolean wasSummarized, boolean wasTruncated, int lineCount) {}

    /**
     * Processes pasted content, potentially summarizing if it exceeds the token threshold.
     */
    public PasteSummary processPaste(String pastedText, int pasteIndex) {
        if (pastedText == null || pastedText.isEmpty()) {
            return new PasteSummary("", "", false, false, 0);
        }

        int lineCount = TokenCounter.countLines(pastedText);
        int tokens = TokenCounter.estimateTokens(pastedText);
        int targetTokens = getTargetTokens();

        // Always use placeholder display for any pasted content that contains a line break.
        // This keeps the composer single-line unless the user explicitly inserts a newline
        // (Shift+Enter/Ctrl+Enter/Ctrl+J).
        boolean usePlaceholder = containsLineBreak(pastedText)
            || lineCount >= PLACEHOLDER_MIN_LINES
            || pastedText.length() > PLACEHOLDER_MIN_CHARS;
        String displayText = usePlaceholder
            ? "[Pasted text " + pasteIndex + "]"
            : pastedText;

        // Check if summarization is needed
        if (!isSummaryEnabled() || tokens <= targetTokens) {
            return new PasteSummary(displayText, pastedText, false, false, lineCount);
        }

        // Summarize the content
        SummarizeResult result = summarizeWithFallback(pastedText, targetTokens, "pasted content");
        String summaryDisplay = result.wasTruncated()
            ? "[Pasted text " + pasteIndex + " (truncated)]"
            : "[Pasted text " + pasteIndex + " (summarized)]";
        return new PasteSummary(summaryDisplay, result.text(), true, result.wasTruncated(), lineCount);
    }

    private static boolean containsLineBreak(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }

    /**
     * Result of context trimming.
     *
     * @param messages     The trimmed message list
     * @param wasTrimmed   Whether any messages were summarized
     * @param wasTruncated Whether summarization failed and content was truncated
     */
    public record TrimResult(List<ChatMessage> messages, boolean wasTrimmed, boolean wasTruncated) {}

    /**
     * Internal result of summarization attempt.
     */
    private record SummarizeResult(String text, boolean wasTruncated) {}

    /**
     * Trims a conversation to fit within the context window, preserving recent messages.
     *
     * @param conversation  The conversation to trim
     * @param model         The model (determines context window size)
     * @param reserveTokens Tokens to reserve for new input and response
     */
    public TrimResult trimIfNeeded(Conversation conversation, String model, int reserveTokens) {
        int remaining = ContextTracker.remainingTokens(conversation, model);
        if (remaining >= reserveTokens) {
            return new TrimResult(conversation.getMessages(), false, false);
        }

        List<ChatMessage> messages = new ArrayList<>(conversation.getMessages());
        if (messages.size() <= 2) {
            return new TrimResult(messages, false, false);
        }

        int summarizeStart = findSummarizeStart(messages);
        int summarizeEnd = findSummarizeEnd(messages, summarizeStart);

        if (summarizeEnd <= summarizeStart) {
            return new TrimResult(messages, false, false);
        }

        String textToSummarize = extractMessagesForSummary(messages, summarizeStart, summarizeEnd);
        int sourceTokens = TokenCounter.estimateTokens(textToSummarize);
        int tokensToFree = reserveTokens - remaining + MIN_SUMMARY_TOKENS;
        int desiredTokens = Math.max(MIN_SUMMARY_TOKENS, sourceTokens - tokensToFree);
        int targetTokens = Math.min(sourceTokens, desiredTokens);

        SummarizeResult result = summarizeWithFallback(textToSummarize, targetTokens, "conversation history");

        ChatMessage summaryMessage = createSummaryMessage(
            conversation, summarizeStart, model, result.text()
        );

        List<ChatMessage> trimmed = buildTrimmedList(messages, summarizeStart, summarizeEnd, summaryMessage);
        return new TrimResult(trimmed, true, result.wasTruncated());
    }

    private String extractMessagesForSummary(List<ChatMessage> messages, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            ChatMessage m = messages.get(i);
            if (m.role() == Role.USER) {
                sb.append("User: ");
            } else if (m.role() == Role.ASSISTANT) {
                sb.append("Assistant: ");
            }
            sb.append(m.content()).append("\n\n");
        }
        return sb.toString();
    }

    private ChatMessage createSummaryMessage(Conversation conversation, int index, String model, String summary) {
        return new ChatMessage(
            "summary_" + UUID.randomUUID().toString().substring(0, 8),
            conversation.getId(),
            index,
            Role.SYSTEM,
            ChatMessage.Source.SYSTEM,
            "[Earlier conversation summarized]\n" + summary,
            OffsetDateTime.now(ZoneOffset.UTC),
            model,
            conversation.getProvider().name().toLowerCase(),
            null, null, null, null, null
        );
    }

    private List<ChatMessage> buildTrimmedList(
            List<ChatMessage> messages, int summarizeStart, int summarizeEnd, ChatMessage summaryMessage) {
        List<ChatMessage> trimmed = new ArrayList<>();
        for (int i = 0; i < summarizeStart; i++) {
            trimmed.add(messages.get(i));
        }
        trimmed.add(summaryMessage);
        for (int i = summarizeEnd; i < messages.size(); i++) {
            trimmed.add(messages.get(i));
        }
        return trimmed;
    }

    /**
     * Summarizes text to fit within a target token count.
     * Falls back to truncation if LLM summarization fails.
     *
     * @return summarized text (caller unaware of fallback path)
     */
    public String summarize(String text, int targetTokens, String context) {
        return summarizeWithFallback(text, targetTokens, context).text();
    }

    /**
     * Summarizes text with explicit indication of whether fallback was used.
     */
    private SummarizeResult summarizeWithFallback(String text, int targetTokens, String context) {
        int targetWords = TokenCounter.tokensToWords((int) (targetTokens * SUMMARY_WORD_RATIO));

        String prompt = """
            Summarize the following %s concisely in approximately %d words.
            Preserve key information, code snippets, file paths, and important technical details.
            Do not add commentary or preamble - provide only the summary.

            ---
            %s
            """.formatted(context, targetWords, text);

        try {
            String model = config.resolveModel();
            String summary = completionService.complete(prompt, model);
            return new SummarizeResult(summary, false);
        } catch (RuntimeException e) {
            // LLM unavailable - fall back to simple truncation
            // Expected when offline or API key invalid
            return new SummarizeResult(truncateToTokens(text, targetTokens), true);
        }
    }

    private String truncateToTokens(String text, int targetTokens) {
        int targetChars = targetTokens * CHARS_PER_TOKEN;
        if (text.length() <= targetChars) {
            return text;
        }
        return text.substring(0, targetChars) + TRUNCATION_MARKER;
    }

    public boolean isSummaryEnabled() {
        String env = System.getenv(ENV_DISABLED);
        if ("1".equals(env) || "true".equalsIgnoreCase(env)) return false;
        return config.isSummaryEnabled();
    }

    public int getTargetTokens() {
        String env = System.getenv(ENV_TARGET);
        if (env != null && !env.isBlank()) {
            int parsed = parseIntOrDefault(env.trim(), -1);
            if (parsed > 0) {
                return parsed;
            }
            // Invalid env value - fall through to config
        }
        return config.getSummaryTargetTokens();
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int findSummarizeStart(List<ChatMessage> messages) {
        // Skip any leading system messages
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).role() != Role.SYSTEM) {
                return i;
            }
        }
        return 0;
    }

    private int findSummarizeEnd(List<ChatMessage> messages, int start) {
        // Keep at least the last 2 exchanges (user, assistant, user, assistant)
        return Math.max(start, messages.size() - MESSAGES_TO_PRESERVE);
    }
}
