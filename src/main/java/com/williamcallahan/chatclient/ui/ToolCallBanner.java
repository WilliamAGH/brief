package com.williamcallahan.chatclient.ui;

import com.williamcallahan.chatclient.service.tools.WeatherForecastTool;
import com.williamcallahan.tui4j.compat.lipgloss.border.StandardBorder;
import com.williamcallahan.tui4j.compat.lipgloss.Style;

final class ToolCallBanner {

    private ToolCallBanner() {}

    static String render(String toolName, int wrapWidth) {
        String displayName = displayName(toolName);
        String title = TuiTheme.hint().render("tool call");
        String name = TuiTheme.sectionHeader().render(displayName);
        String text = title + ": " + name;

        Style box = Style.newStyle()
            .border(StandardBorder.NormalBorder, true, true, true, true)
            .borderForeground(TuiTheme.BORDER)
            .padding(0, 1, 0, 1);

        int chromeWidth = box.getHorizontalFrameSize();
        int maxContentWidth = Math.max(1, wrapWidth - chromeWidth);
        String clippedText = TuiTheme.truncate(text, maxContentWidth);
        return box.render(clippedText);
    }

    static String displayName(String toolName) {
        if (toolName == null || toolName.isBlank()) return "tool";
        if (WeatherForecastTool.NAME.equals(toolName)) return "weather";
        String normalized = toolName;
        if (normalized.startsWith("get_")) normalized = normalized.substring(4);
        return normalized.replace('_', ' ').trim();
    }
}
