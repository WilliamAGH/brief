package com.williamcallahan.chatclient.ui;

import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for manipulating view lines from the Textarea component.
 * Handles prompt alignment and ANSI stripping for consistent display.
 */
final class ComposerViewLines {

    private static final String PROMPT = "› ";
    private static final char ANSI_ESC = '\u001B';

    private ComposerViewLines() {}

    static List<String> from(Textarea composer) {
        List<String> lines = new ArrayList<>(
            Arrays.asList(composer.view().split("\n", -1))
        );
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        // Textarea.view() always appends height padding lines; drop them.
        int trim = composer.height();
        int keep = Math.max(1, lines.size() - trim);
        if (keep < lines.size()) {
            lines = new ArrayList<>(lines.subList(0, keep));
        }

        if (lines.size() > 1) {
            Style promptStyle = composer.style().computedPrompt();
            String styledPrompt = promptStyle.render(PROMPT);
            int promptWidth = TuiTheme.visualWidth(PROMPT);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(styledPrompt)) {
                    lines.set(
                        i,
                        " ".repeat(promptWidth) +
                            line.substring(styledPrompt.length())
                    );
                    continue;
                }

                if (TuiTheme.stripAnsi(line).startsWith(PROMPT)) {
                    lines.set(
                        i,
                        " ".repeat(promptWidth) +
                            dropLeadingVisible(line, promptWidth)
                    );
                }
            }
        }

        return lines;
    }

    private static String dropLeadingVisible(String line, int visibleCount) {
        int index = 0;
        int seen = 0;
        while (index < line.length() && seen < visibleCount) {
            if (
                line.charAt(index) == ANSI_ESC &&
                index + 1 < line.length() &&
                line.charAt(index + 1) == '['
            ) {
                index = findAnsiSequenceEnd(line, index);
                continue;
            }
            index++;
            seen++;
        }
        return line.substring(index);
    }

    private static int findAnsiSequenceEnd(String text, int pos) {
        int end = pos + 2;
        while (end < text.length() && text.charAt(end) != 'm') {
            end++;
        }
        return (end < text.length()) ? end + 1 : pos;
    }
}
