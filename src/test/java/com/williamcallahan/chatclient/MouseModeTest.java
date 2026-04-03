package com.williamcallahan.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void allModeEnablesAllMotionWithoutCellMotion() {
        MouseMode mode = MouseMode.parse("all");

        assertTrue(mode.enablesAllMotion());
        assertFalse(mode.enablesCellMotion());
        assertTrue(mode.enablesMouseClicks());
        assertTrue(mode.enablesTargetCursor());
        assertFalse(mode.enablesHistorySelection());
        assertFalse(mode.enablesSelectionCursor());
        assertFalse(mode.enablesSelectionAutoScroll());
    }

    @Test
    void defaultsToSelectForNullBlankAndUnrecognized() {
        assertEquals(MouseMode.SELECT, MouseMode.parse(null));
        assertEquals(MouseMode.SELECT, MouseMode.parse(""));
        assertEquals(MouseMode.SELECT, MouseMode.parse("   "));
        assertEquals(MouseMode.SELECT, MouseMode.parse("garbage"));
    }

    @Test
    void parseNormalizesAliasesToCanonicalValues() {
        assertEquals("0", MouseMode.parse("off").value());
        assertEquals("0", MouseMode.parse("native").value());
        assertEquals("0", MouseMode.parse("false").value());
        assertEquals("1", MouseMode.parse("all").value());
        assertEquals("1", MouseMode.parse("true").value());
        assertEquals("1", MouseMode.parse("1").value());
        assertEquals("wheel", MouseMode.parse("btn").value());
        assertEquals("wheel", MouseMode.parse("buttons").value());
        assertEquals("wheel", MouseMode.parse("wheel").value());
        assertEquals("select", MouseMode.parse("select").value());
    }
}
