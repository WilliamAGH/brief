package com.williamcallahan.ui;

/**
 * Manages the scroll state and viewport window calculation for chat history.
 * Supports bottom-anchored scrolling where an offset of 0 represents the most recent messages.
 */
final class HistoryViewport {
    private int scrollOffsetLines = 0;

    /** @return current number of lines scrolled up from the bottom. */
    int scrollOffsetLines() {
        return scrollOffsetLines;
    }

    /** Scrolls the viewport up by the specified number of lines. */
    void scrollUp(int lines) {
        if (lines <= 0) return;
        scrollOffsetLines = Math.min(scrollOffsetLines + lines, 50_000);
    }

    /** Scrolls the viewport down by the specified number of lines. */
    void scrollDown(int lines) {
        if (lines <= 0) return;
        scrollOffsetLines = Math.max(0, scrollOffsetLines - lines);
    }

    /** Resets the scroll offset to 0, following the most recent messages. */
    void follow() {
        scrollOffsetLines = 0;
    }

    /** Scrolls to the very beginning of the history. */
    void top() {
        scrollOffsetLines = 50_000;
    }

    /**
     * Calculates the current visible window of lines based on total lines and viewport height.
     */
    Window window(int totalLines, int maxLines) {
        if (totalLines < 0) totalLines = 0;
        if (maxLines < 0) maxLines = 0;

        int maxScrollOffset = Math.max(0, totalLines - maxLines);
        scrollOffsetLines = Math.max(0, Math.min(scrollOffsetLines, maxScrollOffset));

        int endExclusive = Math.max(0, totalLines - scrollOffsetLines);
        int startInclusive = Math.max(0, endExclusive - maxLines);
        return new Window(startInclusive, endExclusive);
    }

    /** Represents a range of lines visible in the viewport. */
    record Window(int startInclusive, int endExclusive) {}
}
