package com.williamcallahan.chatclient.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputHistoryTest {

    @Test
    void previousAndNextPreserveDraftAcrossBrowsingSession() {
        InputHistory history = new InputHistory();
        history.push("first");
        history.push("second");

        assertEquals("second", history.previous("draft"));
        assertEquals("first", history.previous("ignored"));
        assertNull(history.previous("ignored"));
        assertEquals("second", history.next());
        assertEquals("draft", history.next());
        assertFalse(history.isBrowsing());
    }

    @Test
    void pushSkipsBlankAndConsecutiveDuplicateEntries() {
        InputHistory history = new InputHistory();
        history.push("first");
        history.push("first");
        history.push(" ");
        history.push(null);

        assertEquals("first", history.previous(""));
        assertNull(history.previous(""));
    }

    @Test
    void resetExitsBrowsingWithoutDroppingEntries() {
        InputHistory history = new InputHistory();
        history.push("first");

        assertEquals("first", history.previous("draft"));
        assertTrue(history.isBrowsing());

        history.reset();

        assertFalse(history.isBrowsing());
        assertEquals("first", history.previous("new draft"));
    }
}
