package com.williamcallahan.chatclient.ui;

import com.williamcallahan.tui4j.compat.lipgloss.Join;
import com.williamcallahan.tui4j.compat.lipgloss.Position;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;
import com.williamcallahan.tui4j.compat.lipgloss.color.TerminalColor;

/**
 * Defines the visual theme and CRT-inspired color palette for the application.
 * Provides utility methods for consistent text styling and layout.
 */
public final class TuiTheme {

    private TuiTheme() {}

    private static final char ANSI_ESC = '\u001B';
    private static final String ANSI_RESET = ANSI_ESC + "[0m";

    public static final TerminalColor PRIMARY = Color.color("#00FF41");
    public static final TerminalColor SECONDARY = Color.color("#39FF14");
    public static final TerminalColor ACCENT = PRIMARY;
    public static final TerminalColor SUCCESS = SECONDARY;
    public static final TerminalColor WARNING = Color.color("#FFAA00");
    public static final TerminalColor ERROR = Color.color("#FF3333");
    public static final TerminalColor MUTED = Color.color("#00AA2A");
    public static final TerminalColor LIGHT = PRIMARY;
    public static final TerminalColor BORDER = Color.color("#006615");

    public static Style brandTitle() {
        return Style.newStyle().foreground(PRIMARY).bold(true);
    }

    public static Style sectionHeader() {
        return Style.newStyle().foreground(SECONDARY).bold(true);
    }

    public static Style hint() {
        return Style.newStyle().foreground(MUTED).faint(true);
    }

    public static Style success() {
        return Style.newStyle().foreground(SUCCESS);
    }

    public static Style warning() {
        return Style.newStyle().foreground(WARNING);
    }

    public static Style error() {
        return Style.newStyle().foreground(ERROR).bold(true);
    }

    public static Style userLabel() {
        return Style.newStyle().foreground(ACCENT).bold(true);
    }

    public static Style assistantLabel() {
        return Style.newStyle().foreground(PRIMARY).bold(true);
    }

    public static Style inputPrompt() {
        return Style.newStyle().foreground(ACCENT).bold(true);
    }

    public static Style spinner() {
        return Style.newStyle().foreground(SECONDARY);
    }

    /** Renders a horizontal divider line. */
    public static String divider(int width, TerminalColor color) {
        Style style = Style.newStyle().foreground(color);
        return style.render("─".repeat(width));
    }

    public static String divider(int width) {
        return divider(width, MUTED);
    }

    public static String joinVertical(Position pos, String... strs) {
        return Join.joinVertical(pos, strs);
    }

    /** Centers text within a given width. */
    public static String center(String text, int width) {
        int textLen = stripAnsi(text).length();
        if (textLen >= width) return text;
        int padding = (width - textLen) / 2;
        return " ".repeat(padding) + text;
    }

    /** Pads text with spaces on the right to reach a target width. */
    public static String padRight(String text, int width) {
        if (text == null) text = "";
        int textLen = stripAnsi(text).length();
        if (textLen >= width) return text;
        return text + " ".repeat(width - textLen);
    }

    /** Truncates text with an ellipsis if it exceeds the specified width, preserving ANSI styling. */
    public static String truncate(String text, int width) {
        if (text == null) return "";
        int visualLen = visualWidth(text);
        if (visualLen <= width) return text;
        if (width <= 3) return truncatePreservingAnsi(text, width);
        return truncatePreservingAnsi(text, width - 3) + "...";
    }

    /** Truncates styled text to a target visual width while preserving ANSI escape sequences. */
    private static String truncatePreservingAnsi(String text, int targetWidth) {
        if (text == null || targetWidth <= 0) return "";

        StringBuilder result = new StringBuilder();
        int visualCount = 0;
        int i = 0;
        boolean hasOpenAnsi = false;

        while (i < text.length() && visualCount < targetWidth) {
            int ansiEnd = findAnsiSequenceEnd(text, i);
            if (ansiEnd > i) {
                String seq = text.substring(i, ansiEnd);
                result.append(seq);
                hasOpenAnsi = !seq.equals(ANSI_RESET);
                i = ansiEnd;
            } else {
                result.append(text.charAt(i));
                visualCount++;
                i++;
            }
        }

        if (hasOpenAnsi) {
            result.append(ANSI_RESET);
        }
        return result.toString();
    }

    /** Returns end index of ANSI sequence starting at pos, or pos if none found. */
    private static int findAnsiSequenceEnd(String text, int pos) {
        if (pos + 1 >= text.length()) return pos;
        if (
            text.charAt(pos) != ANSI_ESC || text.charAt(pos + 1) != '['
        ) return pos;

        int end = pos + 2;
        while (end < text.length() && text.charAt(end) != 'm') {
            end++;
        }
        return (end < text.length()) ? end + 1 : pos;
    }

    /** Removes all ANSI escape sequences from the provided text. */
    public static String stripAnsi(String text) {
        if (text == null) return "";
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    /** Returns the visual width of the text, excluding ANSI escape sequences. */
    public static int visualWidth(String text) {
        return stripAnsi(text).length();
    }

    /** Removes trailing newlines from the provided string. */
    public static String stripTrailingNewlines(String s) {
        if (s == null || s.isEmpty()) return "";
        int end = s.length();
        while (
            end > 0 && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')
        ) {
            end--;
        }
        return s.substring(0, end);
    }

    /** Renders a keyboard shortcut hint (e.g., "ENTER submit"). */
    public static String shortcutHint(String key, String action) {
        Style keyStyle = Style.newStyle().foreground(ACCENT).bold(true);
        Style actionStyle = Style.newStyle().foreground(MUTED);
        return keyStyle.render(key) + " " + actionStyle.render(action);
    }

    /** Joins multiple shortcut hints with a bullet separator. */
    public static String shortcutRow(String... shortcuts) {
        Style bulletStyle = Style.newStyle().foreground(MUTED);
        String bullet = bulletStyle.render(" • ");
        return String.join(bullet, shortcuts);
    }
}
