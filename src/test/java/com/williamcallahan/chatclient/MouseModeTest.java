package com.williamcallahan.chatclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MouseModeTest {

    @Test
    void selectModeEnablesHistorySelectionAndAutoScroll() {
        MouseMode mode = MouseMode.parse("select");

        assertTrue(mode.enablesCellMotion());
        assertTrue(mode.enablesMouseClicks());
        assertTrue(mode.enablesTargetCursor());
        assertTrue(mode.enablesHistorySelection());
        assertTrue(mode.enablesSelectionCursor());
        assertTrue(mode.enablesSelectionAutoScroll());
    }

    @Test
    void wheelModeCapturesWheelWithoutHistorySelection() {
        MouseMode mode = MouseMode.parse("wheel");

        assertTrue(mode.enablesCellMotion());
        assertTrue(mode.enablesMouseClicks());
        assertTrue(mode.enablesTargetCursor());
        assertFalse(mode.enablesHistorySelection());
        assertFalse(mode.enablesSelectionCursor());
        assertFalse(mode.enablesSelectionAutoScroll());
    }

    @Test
    void nativeModeLeavesMouseWithTerminal() {
        MouseMode mode = MouseMode.parse("native");

        assertFalse(mode.enablesAllMotion());
        assertFalse(mode.enablesCellMotion());
        assertFalse(mode.enablesMouseClicks());
        assertFalse(mode.enablesTargetCursor());
        assertFalse(mode.enablesHistorySelection());
        assertFalse(mode.enablesSelectionCursor());
    }
}
