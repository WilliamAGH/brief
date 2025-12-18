package com.williamcallahan.lattetui;

import org.flatscrew.latte.input.MouseButton;
import org.flatscrew.latte.input.MouseMessage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** Selection state and mapping from mouse coordinates to visible history lines. */
final class MouseSelectionController {
    private boolean selecting = false;
    private boolean selectionMoved = false;
    private int selectionStart = -1;
    private int selectionEnd = -1;

    private long lastActionAtMs = 0L;
    private String lastStatus = null;

    private int historyStartRow = 0;
    private int historyStartCol = 0;
    private List<String> visibleHistoryPlain = List.of();

    boolean isSelecting() {
        return selecting;
    }

    int selectionStart() {
        return selectionStart;
    }

    int selectionEnd() {
        return selectionEnd;
    }

    String transientStatus(long nowMs) {
        if (lastStatus == null) return null;
        return (nowMs - lastActionAtMs) < 1200 ? lastStatus : null;
    }

    void updateHistoryMapping(int startRow, int startCol, List<String> visiblePlain) {
        this.historyStartRow = startRow;
        this.historyStartCol = startCol;
        this.visibleHistoryPlain = (visiblePlain == null) ? List.of() : visiblePlain;
    }

    /**
     * Handles non-wheel mouse messages for selection/open/copy.
     * Returns true when the UI should re-render.
     */
    boolean handle(MouseMessage mouse) {
        if (mouse == null || mouse.isWheel()) return false;

        int idx = historyLineIndex(mouse.row());
        if (idx >= 0) {
            if (mouse.getAction() == org.flatscrew.latte.input.MouseAction.MouseActionPress
                && mouse.getButton() == MouseButton.MouseButtonLeft) {
                selecting = true;
                selectionMoved = false;
                selectionStart = idx;
                selectionEnd = idx;
                return true;
            }
            if (mouse.getAction() == org.flatscrew.latte.input.MouseAction.MouseActionMotion && selecting) {
                selectionMoved = true;
                selectionEnd = idx;
                return true;
            }
            if (mouse.getAction() == org.flatscrew.latte.input.MouseAction.MouseActionRelease && selecting) {
                selecting = false;
                selectionEnd = idx;

                if (!selectionMoved) {
                    boolean opened = openLinkUnderMouse(mouse.row(), mouse.column());
                    if (!opened) copySelectedHistoryLines();
                } else {
                    copySelectedHistoryLines();
                }
                return true;
            }
        } else if (mouse.getAction() == org.flatscrew.latte.input.MouseAction.MouseActionRelease) {
            selecting = false;
            return true;
        }

        return false;
    }

    private int historyLineIndex(int mouseRow) {
        int idx = mouseRow - historyStartRow;
        if (idx < 0 || idx >= visibleHistoryPlain.size()) return -1;
        return idx;
    }

    private boolean openLinkUnderMouse(int mouseRow, int mouseCol) {
        int lineIndex = historyLineIndex(mouseRow);
        if (lineIndex < 0) return false;

        int col = mouseCol - historyStartCol;
        if (col < 0) col = 0;

        String line = visibleHistoryPlain.get(lineIndex);
        if (line == null || line.isBlank()) return false;
        if (col >= line.length()) col = line.length() - 1;

        String token = tokenAt(line, col);
        if (token == null || token.isBlank()) return false;

        String url = normalizeUrlToken(token);
        if (url == null) return false;

        boolean ok = openUrl(url);
        if (ok) {
            lastActionAtMs = System.currentTimeMillis();
            lastStatus = "OPENED";
            selectionStart = -1;
            selectionEnd = -1;
        }
        return ok;
    }

    private void copySelectedHistoryLines() {
        if (visibleHistoryPlain.isEmpty() || selectionStart < 0 || selectionEnd < 0) return;
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        start = Math.max(0, Math.min(start, visibleHistoryPlain.size() - 1));
        end = Math.max(0, Math.min(end, visibleHistoryPlain.size() - 1));

        List<String> lines = visibleHistoryPlain.subList(start, end + 1);
        String text = String.join("\n", lines).trim();
        if (text.isBlank()) return;

        boolean ok = copyToClipboard(text);
        lastActionAtMs = System.currentTimeMillis();
        lastStatus = ok ? "COPIED" : "COPY-ERR";
        selectionStart = -1;
        selectionEnd = -1;
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

    private static String normalizeUrlToken(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.isEmpty()) return null;

        // Strip common surrounding punctuation from markdown/parentheses.
        t = t.replaceAll("^[\"'\\[\\(<{`]+|[\"'\\]\\)>}`.,]+$", "");

        if (t.isEmpty()) return null;

        String lower = t.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) return t;

        // Bare domains like github.com/foo
        if (t.contains(".") && !t.contains("://") && !t.contains("@") && !t.contains("..")) {
            return "https://" + t;
        }
        return null;
    }

    private static boolean openUrl(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(java.net.URI.create(url));
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                return true;
            }
            if (os.contains("nix") || os.contains("nux") || os.contains("linux")) {
                new ProcessBuilder("xdg-open", url).start();
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static boolean copyToClipboard(String text) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                Process p = new ProcessBuilder("pbcopy").start();
                p.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
                p.getOutputStream().close();
                p.waitFor();
                return p.exitValue() == 0;
            }
        } catch (Throwable ignored) {
            // fall through to OSC52
        }
        try {
            String b64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            System.out.print("\u001b]52;c;" + b64 + "\u0007");
            System.out.flush();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
