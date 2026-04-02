package com.williamcallahan.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainMouseModeTest {

    @Test
    void defaultsToSelectWhenUnsetOrUnknown() {
        assertEquals("select", Main.normalizeMouseMode(null));
        assertEquals("select", Main.normalizeMouseMode(""));
        assertEquals("select", Main.normalizeMouseMode("unknown"));
    }

    @Test
    void normalizesMouseAliases() {
        assertEquals("0", Main.normalizeMouseMode("off"));
        assertEquals("0", Main.normalizeMouseMode("native"));
        assertEquals("1", Main.normalizeMouseMode("all"));
        assertEquals("1", Main.normalizeMouseMode("1"));
        assertEquals("wheel", Main.normalizeMouseMode("btn"));
        assertEquals("wheel", Main.normalizeMouseMode("wheel"));
        assertEquals("select", Main.normalizeMouseMode("select"));
    }
}
