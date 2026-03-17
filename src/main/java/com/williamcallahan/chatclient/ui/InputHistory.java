package com.williamcallahan.chatclient.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * In-session input history for the composer, navigable with Up/Down arrows.
 * Stores sent messages and allows browsing through them, preserving the
 * in-progress draft when entering history mode.
 */
final class InputHistory {

    private final List<String> entries = new ArrayList<>();
    private int cursor = -1;
    private String draft = "";

    /** Pushes a sent message onto the history stack and resets browsing state. */
    void push(String text) {
        if (text == null || text.isBlank()) return;
        // Avoid consecutive duplicates
        if (!entries.isEmpty() && entries.getLast().equals(text)) {
            reset();
            return;
        }
        entries.add(text);
        reset();
    }

    /**
     * Moves to the previous (older) history entry.
     *
     * @param currentText the current composer content, saved as draft on first call
     * @return the previous entry, or null if already at the oldest
     */
    String previous(String currentText) {
        if (entries.isEmpty()) return null;
        if (cursor == -1) {
            draft = currentText;
            cursor = entries.size() - 1;
        } else if (cursor > 0) {
            cursor--;
        } else {
            return null;
        }
        return entries.get(cursor);
    }

    /**
     * Moves to the next (newer) history entry, or restores the draft.
     *
     * @return the next entry, the saved draft, or null if not browsing
     */
    String next() {
        if (cursor == -1) return null;
        if (cursor < entries.size() - 1) {
            cursor++;
            return entries.get(cursor);
        }
        // Past the newest entry — restore draft
        String saved = draft;
        reset();
        return saved;
    }

    /** Returns true if currently browsing history (not at the draft position). */
    boolean isBrowsing() {
        return cursor != -1;
    }

    /** Resets browsing state without clearing stored entries. */
    void reset() {
        cursor = -1;
        draft = "";
    }
}
