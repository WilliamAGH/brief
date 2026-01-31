package com.williamcallahan.chatclient.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoryViewportTest {

    @Test
    void window_doesNotResetScrollOffsetWhenContentFits() {
        HistoryViewport viewport = new HistoryViewport();

        viewport.scrollUp(100);
        assertEquals(100, viewport.scrollOffsetLines());

        viewport.window(5, 10);
        assertEquals(100, viewport.scrollOffsetLines());
    }

    @Test
    void window_usesClampedScrollOffsetForVisibleRange() {
        HistoryViewport viewport = new HistoryViewport();

        viewport.scrollUp(200);
        HistoryViewport.Window window = viewport.window(150, 20);

        assertEquals(0, window.startInclusive());
        assertEquals(20, window.endExclusive());
    }
}
