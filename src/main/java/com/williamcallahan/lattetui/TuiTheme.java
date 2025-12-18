package com.williamcallahan.lattetui;

import org.flatscrew.latte.cream.Position;
import org.flatscrew.latte.cream.Style;
import org.flatscrew.latte.cream.color.Color;
import org.flatscrew.latte.cream.color.TerminalColor;
import org.flatscrew.latte.cream.join.VerticalJoinDecorator;

/** Green CRT terminal theme. */
public final class TuiTheme {

    private TuiTheme() {}

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
        return Style.newStyle()
            .foreground(PRIMARY)
            .bold(true);
    }

    public static Style sectionHeader() {
        return Style.newStyle()
            .foreground(SECONDARY)
            .bold(true);
    }

    public static Style hint() {
        return Style.newStyle()
            .foreground(MUTED)
            .faint(true);
    }

    public static Style success() {
        return Style.newStyle()
            .foreground(SUCCESS);
    }

    public static Style warning() {
        return Style.newStyle()
            .foreground(WARNING);
    }

    public static Style error() {
        return Style.newStyle()
            .foreground(ERROR)
            .bold(true);
    }

    public static Style userLabel() {
        return Style.newStyle()
            .foreground(ACCENT)
            .bold(true);
    }

    public static Style assistantLabel() {
        return Style.newStyle()
            .foreground(PRIMARY)
            .bold(true);
    }

    public static Style inputPrompt() {
        return Style.newStyle()
            .foreground(ACCENT)
            .bold(true);
    }

    public static Style spinner() {
        return Style.newStyle()
            .foreground(SECONDARY);
    }

    public static String headerWithInfo(String title, String info, int width) {
        Style titleStyle = Style.newStyle()
            .foreground(PRIMARY)
            .bold(true);
        Style infoStyle = Style.newStyle()
            .foreground(MUTED);
        Style lineStyle = Style.newStyle()
            .foreground(MUTED)
            .faint(true);

        String titleRendered = titleStyle.render(title);
        String infoRendered = infoStyle.render(info);

        int usedWidth = visualWidth(title) + visualWidth(info) + 2;
        int lineWidth = Math.max(1, width - usedWidth);
        String line = lineStyle.render(" " + "─".repeat(lineWidth) + " ");

        return titleRendered + line + infoRendered;
    }

    public static String divider(int width, TerminalColor color) {
        Style style = Style.newStyle().foreground(color);
        return style.render("─".repeat(width));
    }

    public static String divider(int width) {
        return divider(width, MUTED);
    }

    public static String joinVertical(Position pos, String... strs) {
        return VerticalJoinDecorator.joinVertical(pos, strs);
    }

    public static String center(String text, int width) {
        int textLen = stripAnsi(text).length();
        if (textLen >= width) return text;
        int padding = (width - textLen) / 2;
        return " ".repeat(padding) + text;
    }

    public static String padRight(String text, int width) {
        if (text == null) text = "";
        int textLen = stripAnsi(text).length();
        if (textLen >= width) return text;
        return text + " ".repeat(width - textLen);
    }

    public static String truncate(String text, int width) {
        if (text == null) return "";
        String stripped = stripAnsi(text);
        if (stripped.length() <= width) return text;
        if (width <= 3) return stripped.substring(0, width);
        return stripped.substring(0, width - 3) + "...";
    }

    public static String stripAnsi(String text) {
        if (text == null) return "";
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    public static int visualWidth(String text) {
        return stripAnsi(text).length();
    }

    public static String stripTrailingNewlines(String s) {
        if (s == null || s.isEmpty()) return "";
        int end = s.length();
        while (end > 0 && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')) {
            end--;
        }
        return s.substring(0, end);
    }

    public static String shortcutHint(String key, String action) {
        Style keyStyle = Style.newStyle()
            .foreground(ACCENT)
            .bold(true);
        Style actionStyle = Style.newStyle()
            .foreground(MUTED);

        return keyStyle.render(key) + " " + actionStyle.render(action);
    }

    public static String shortcutRow(String... shortcuts) {
        Style bulletStyle = Style.newStyle().foreground(MUTED);
        String bullet = bulletStyle.render(" • ");
        return String.join(bullet, shortcuts);
    }
}
