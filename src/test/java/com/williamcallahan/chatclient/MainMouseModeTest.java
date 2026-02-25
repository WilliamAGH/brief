package com.williamcallahan.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainMouseModeTest {

    @Test
    void defaultsToNativeWhenUnsetOrUnknown() {
        assertEquals("0", Main.normalizeMouseMode(null));
        assertEquals("0", Main.normalizeMouseMode(""));
        assertEquals("0", Main.normalizeMouseMode("unknown"));
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
