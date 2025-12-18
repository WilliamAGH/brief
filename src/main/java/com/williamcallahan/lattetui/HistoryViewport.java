package com.williamcallahan.lattetui;

/** Scroll state and window calculation for a bottom-anchored history viewport. */
final class HistoryViewport {
    private int scrollOffsetLines = 0;

    int scrollOffsetLines() {
        return scrollOffsetLines;
    }

    void scrollUp(int lines) {
        if (lines <= 0) return;
        scrollOffsetLines = Math.min(scrollOffsetLines + lines, 50_000);
    }

    void scrollDown(int lines) {
        if (lines <= 0) return;
        scrollOffsetLines = Math.max(0, scrollOffsetLines - lines);
    }

    void follow() {
        scrollOffsetLines = 0;
    }

    void top() {
        scrollOffsetLines = 50_000;
    }

    Window window(int totalLines, int maxLines) {
        if (totalLines < 0) totalLines = 0;
        if (maxLines < 0) maxLines = 0;

        int maxScrollOffset = Math.max(0, totalLines - maxLines);
        scrollOffsetLines = Math.max(0, Math.min(scrollOffsetLines, maxScrollOffset));

        int endExclusive = Math.max(0, totalLines - scrollOffsetLines);
        int startInclusive = Math.max(0, endExclusive - maxLines);
        return new Window(startInclusive, endExclusive);
    }

    record Window(int startInclusive, int endExclusive) {}
}
