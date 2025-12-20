package com.williamcallahan.ui;

import com.williamcallahan.tui4j.Command;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Manages mouse-based text selection and interaction within the chat history.
 * Tracks selection coordinates and maps them to visible history lines for highlighting and copying.
 */
final class MouseSelectionController {
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://|www\\.)[a-zA-Z0-0+&@#/%?=~_|!:,.;]*[a-zA-Z0-0+&@#/%=~_|]$"
    );

    private boolean selecting = false;
    private boolean selectionMoved = false;
    private int selectionStartLineIndex = -1;
    private int selectionStartCol = -1;
    private int selectionEndLineIndex = -1;
    private int selectionEndCol = -1;

    private long lastActionAtMs = 0L;
    private String lastStatus = null;

    private int historyStartRow = 0;
    private int historyStartCol = 0;
    private int historyWindowStartIndex = 0;
    private List<String> visibleHistoryPlain = List.of();
    private List<String> allHistoryPlain = List.of();

    /** @return true if a selection drag is currently in progress. */
    boolean isSelecting() {
        return selecting;
    }

    /** @return true if a selection exists (even if not currently dragging). */
    boolean hasSelection() {
        return selectionStartLineIndex != -1 && selectionEndLineIndex != -1;
    }

    int selectionStartLineIndex() { return selectionStartLineIndex; }
    int selectionStartCol() { return selectionStartCol; }
    int selectionEndLineIndex() { return selectionEndLineIndex; }
    int selectionEndCol() { return selectionEndCol; }

    /**
     * Returns a transient status message (e.g., "COPIED") to display in the UI.
     */
    String transientStatus(long nowMs) {
        if (lastStatus == null) return null;
        return (nowMs - lastActionAtMs) < 1200 ? lastStatus : null;
    }

    /**
     * Updates the mapping between screen coordinates and history lines.
     */
    void updateHistoryMapping(int startRow, int startCol, int windowStartIndex,
                              List<String> visiblePlain, List<String> allPlain) {
        this.historyStartRow = startRow;
        this.historyStartCol = startCol;
        this.historyWindowStartIndex = Math.max(0, windowStartIndex);
        this.visibleHistoryPlain = (visiblePlain == null) ? List.of() : visiblePlain;
        this.allHistoryPlain = (allPlain == null) ? List.of() : allPlain;
        clampSelectionToHistory();
    }

    /**
     * Clears the current selection state.
     */
    void clearSelection() {
        selecting = false;
        selectionMoved = false;
        selectionStartLineIndex = -1;
        selectionStartCol = -1;
        selectionEndLineIndex = -1;
        selectionEndCol = -1;
    }

    /**
     * Handles mouse messages for selection, link opening, and copying.
     */
    Command handle(MouseMessage mouse) {
        if (mouse == null || mouse.isWheel()) return null;

        int visibleRowIndex = visibleLineIndex(mouse.row());
        int col = mouse.column() - historyStartCol;
        if (col < 0) col = 0;

        List<Command> commands = new ArrayList<>();

        // Manage mouse cursor shape based on state and hover position
        if (mouse.getAction() == MouseAction.MouseActionRelease && !selecting) {
            commands.add(Command.resetMouseCursor());
        } else if (selecting) {
            commands.add(Command.setMouseCursorText());
        } else if (visibleRowIndex >= 0) {
            String line = (visibleRowIndex < visibleHistoryPlain.size()) ? visibleHistoryPlain.get(visibleRowIndex) : null;
            if (line != null && col < line.length()) {
                String token = tokenAt(line, col);
                if (isPotentialUrl(token)) {
                    commands.add(Command.setMouseCursorPointer());
                } else {
                    commands.add(Command.setMouseCursorText());
                }
            } else {
                commands.add(Command.setMouseCursorText());
            }
        } else {
            commands.add(Command.resetMouseCursor());
        }

        // Handle selection state transitions
        if (mouse.getAction() == MouseAction.MouseActionPress && mouse.getButton() == MouseButton.MouseButtonLeft) {
            if (visibleRowIndex >= 0) {
                int startLineIndex = clampLineIndex(absoluteLineIndexFromVisibleRow(visibleRowIndex));
                if (startLineIndex == -1) {
                    clearSelection();
                    return Command.batch(commands);
                }
                selecting = true;
                selectionMoved = false;
                selectionStartLineIndex = startLineIndex;
                selectionStartCol = col;
                selectionEndLineIndex = startLineIndex;
                selectionEndCol = col;
            } else {
                clearSelection();
            }
            return Command.batch(commands);
        }

        if (mouse.getAction() == MouseAction.MouseActionMotion && selecting) {
            selectionMoved = true;
            // Clamp motion to history area if it leaves
            int targetLineIndex = clampLineIndexForRow(mouse.row());
            if (targetLineIndex != -1) {
                selectionEndLineIndex = targetLineIndex;
            }
            selectionEndCol = col;
            return Command.batch(commands);
        }

        if (mouse.getAction() == MouseAction.MouseActionRelease && selecting) {
            selecting = false;
            int targetLineIndex = clampLineIndexForRow(mouse.row());
            if (targetLineIndex != -1) {
                selectionEndLineIndex = targetLineIndex;
            }
            selectionEndCol = col;

            if (!selectionMoved) {
                boolean opened = openLinkUnderMouse(mouse.row(), mouse.column());
                if (!opened) {
                    Command copyCmd = copySelectedHistoryLines();
                    if (copyCmd != null) commands.add(copyCmd);
                } else {
                    clearSelection();
                }
            } else {
                Command copyCmd = copySelectedHistoryLines();
                if (copyCmd != null) commands.add(copyCmd);
            }
            return Command.batch(commands);
        }

        return commands.isEmpty() ? null : Command.batch(commands);
    }

    private int visibleLineIndex(int mouseRow) {
        int index = mouseRow - historyStartRow;
        if (index < 0 || index >= visibleHistoryPlain.size()) return -1;
        return index;
    }

    private int absoluteLineIndexFromVisibleRow(int visibleRowIndex) {
        return historyWindowStartIndex + visibleRowIndex;
    }

    private int clampLineIndex(int lineIndex) {
        if (allHistoryPlain.isEmpty()) return -1;
        if (lineIndex < 0) return 0;
        int maxIndex = allHistoryPlain.size() - 1;
        return Math.min(lineIndex, maxIndex);
    }

    private int clampLineIndexForRow(int mouseRow) {
        if (visibleHistoryPlain.isEmpty()) return -1;
        int maxLineIndex = allHistoryPlain.isEmpty() ? -1 : allHistoryPlain.size() - 1;
        if (maxLineIndex < 0) return -1;
        if (mouseRow < historyStartRow) return historyWindowStartIndex;
        int lastVisibleRow = historyStartRow + visibleHistoryPlain.size() - 1;
        if (mouseRow > lastVisibleRow) {
            return Math.min(historyWindowStartIndex + visibleHistoryPlain.size() - 1, maxLineIndex);
        }
        return Math.min(historyWindowStartIndex + (mouseRow - historyStartRow), maxLineIndex);
    }

    private boolean isPotentialUrl(String token) {
        if (token == null) return false;
        String t = token.trim();
        // Simple heuristic: must contain a dot and start with common URL prefixes
        // or look like a domain name.
        t = stripPunctuation(t);
        if (t.isEmpty()) return false;

        return URL_PATTERN.matcher(t).matches() ||
               (t.contains(".") && (t.toLowerCase().startsWith("http") || t.toLowerCase().startsWith("www")));
    }

    private String stripPunctuation(String t) {
        return t.replaceAll("^[\"'\\[\\(<{`]+|[\"'\\]\\)>}`.,]+$", "");
    }

    private boolean openLinkUnderMouse(int mouseRow, int mouseCol) {
        int visibleRowIndex = visibleLineIndex(mouseRow);
        if (visibleRowIndex < 0) return false;

        int col = mouseCol - historyStartCol;
        if (col < 0) col = 0;

        String line = visibleHistoryPlain.get(visibleRowIndex);
        if (line == null || line.isBlank()) return false;
        if (col >= line.length()) col = line.length() - 1;

        String token = tokenAt(line, col);
        String url = normalizeUrl(token);
        if (url == null) return false;

        if (openUrl(url)) {
            lastActionAtMs = System.currentTimeMillis();
            lastStatus = "OPENED";
            return true;
        }
        return false;
    }

    private String normalizeUrl(String token) {
        if (token == null) return null;
        String t = stripPunctuation(token.trim());
        if (t.isEmpty()) return null;

        if (t.toLowerCase().startsWith("http://") || t.toLowerCase().startsWith("https://")) return t;
        if (t.toLowerCase().startsWith("www.")) return "https://" + t;

        // Only return as URL if it matches our pattern
        if (URL_PATTERN.matcher(t).matches()) {
            return t.contains("://") ? t : "https://" + t;
        }
        return null;
    }

    private Command copySelectedHistoryLines() {
        if (!hasSelection() || allHistoryPlain.isEmpty()) return null;

        int r1 = selectionStartLineIndex;
        int c1 = selectionStartCol;
        int r2 = selectionEndLineIndex;
        int c2 = selectionEndCol;

        if (r1 > r2 || (r1 == r2 && c1 > c2)) {
            int tr = r1; r1 = r2; r2 = tr;
            int tc = c1; c1 = c2; c2 = tc;
        }

        StringBuilder text = new StringBuilder();
        for (int r = r1; r <= r2; r++) {
            if (r < 0 || r >= allHistoryPlain.size()) continue;
            String line = allHistoryPlain.get(r);
            if (line == null) continue;

            if (r == r1 && r == r2) {
                int start = Math.max(0, Math.min(c1, line.length()));
                int end = Math.max(0, Math.min(c2, line.length()));
                text.append(line.substring(Math.min(start, end), Math.max(start, end)));
            } else if (r == r1) {
                int start = Math.max(0, Math.min(c1, line.length()));
                text.append(line.substring(Math.min(start, line.length()))).append("\n");
            } else if (r == r2) {
                int end = Math.max(0, Math.min(c2, line.length()));
                text.append(line.substring(0, Math.min(end, line.length())));
            } else {
                text.append(line).append("\n");
            }
        }

        String result = text.toString().trim();
        if (result.isEmpty()) return null;

        lastActionAtMs = System.currentTimeMillis();
        lastStatus = "COPIED";
        return Command.copyToClipboard(result);
    }

    private static String tokenAt(String line, int col) {
        int n = line.length();
        if (n == 0) return null;
        col = Math.max(0, Math.min(col, n - 1));

        int left = col;
        while (left > 0 && !Character.isWhitespace(line.charAt(left - 1))) left--;
        int right = col;
        while (right < n && !Character.isWhitespace(line.charAt(right))) right++;

        return line.substring(left, right).trim();
    }

    private static boolean openUrl(String url) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                return true;
            } else if (os.contains("nix") || os.contains("nux") || os.contains("linux")) {
                new ProcessBuilder("xdg-open", url).start();
                return true;
            } else if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void clampSelectionToHistory() {
        if (allHistoryPlain.isEmpty()) {
            clearSelection();
            return;
        }
        int maxIndex = allHistoryPlain.size() - 1;
        if (selectionStartLineIndex != -1) {
            selectionStartLineIndex = Math.max(0, Math.min(selectionStartLineIndex, maxIndex));
        }
        if (selectionEndLineIndex != -1) {
            selectionEndLineIndex = Math.max(0, Math.min(selectionEndLineIndex, maxIndex));
        }
    }
}
