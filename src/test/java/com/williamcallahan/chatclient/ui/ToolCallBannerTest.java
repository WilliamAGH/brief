package com.williamcallahan.chatclient.ui;

import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallBannerTest {

    @BeforeAll
    static void initTerminal() {
        TerminalInfo.provide(() -> new TerminalInfo(false, null));
    }

    @Test
    void render_UsesFriendlyName() {
        String banner = ToolCallBanner.render("get_weather_forecast", 60);
        String plain = TuiTheme.stripAnsi(banner);
        assertTrue(plain.contains("tool call: weather"));
    }

    @Test
    void render_ShrinksWideWrapToContent() {
        String banner = ToolCallBanner.render("get_weather_forecast", 60);
        String plain = TuiTheme.stripAnsi(banner);
        String[] lines = plain.split("\n", -1);
        int maxWidth = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
        int textWidth = "tool call: weather".length();
        assertTrue(
            maxWidth <= textWidth + 4,
            () -> "maxWidth=" + maxWidth + " textWidth=" + textWidth + "\n" + plain
        );
    }
}
