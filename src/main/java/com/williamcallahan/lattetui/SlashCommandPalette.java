package com.williamcallahan.lattetui;

import com.williamcallahan.lattetui.slash.SlashCommand;
import com.williamcallahan.lattetui.slash.SlashCommands;
import org.flatscrew.latte.Command;
import org.flatscrew.latte.Message;
import org.flatscrew.latte.UpdateResult;
import org.flatscrew.latte.cream.Style;
import org.flatscrew.latte.input.key.KeyAliases;
import org.flatscrew.latte.input.key.KeyAliases.KeyAlias;
import org.flatscrew.latte.input.key.KeyType;
import org.flatscrew.latte.message.KeyPressMessage;
import org.flatscrew.latte.spice.textinput.TextInput;

import java.util.ArrayList;
import java.util.List;

/** Slash command palette overlay. */
final class SlashCommandPalette {
    record PaletteUpdate(boolean handled, Command command, String submitText) {}

    private boolean open = false;
    private int selectedIndex = 0;

    boolean isOpen() {
        return open;
    }

    void close() {
        open = false;
        selectedIndex = 0;
    }

    PaletteUpdate update(KeyPressMessage key, Message rawMsg, TextInput composer, boolean waiting, List<SlashCommand> commands) {
        if (waiting) return new PaletteUpdate(false, null, null);

        if (!open) {
            if (!isSlashTrigger(key)) return new PaletteUpdate(false, null, null);
            UpdateResult<? extends org.flatscrew.latte.Model> inputUpdate = composer.update(rawMsg);
            updateFromComposer(composer);
            clampSelection(SlashCommands.filterForComposer(commands, composer.value()).size());
            return new PaletteUpdate(true, inputUpdate.command(), null);
        }

        if (key.type() == KeyType.keyESC) {
            close();
            return new PaletteUpdate(true, null, null);
        }

        List<SlashCommand> matches = SlashCommands.filterForComposer(commands, composer.value());
        clampSelection(matches.size());

        if (key.type() == KeyType.KeyUp) {
            if (!matches.isEmpty()) selectedIndex = Math.max(0, selectedIndex - 1);
            return new PaletteUpdate(true, null, null);
        }
        if (key.type() == KeyType.KeyDown) {
            if (!matches.isEmpty()) selectedIndex = Math.min(matches.size() - 1, selectedIndex + 1);
            return new PaletteUpdate(true, null, null);
        }
        if (key.type() == KeyType.KeyPgUp) {
            if (!matches.isEmpty()) selectedIndex = Math.max(0, selectedIndex - 5);
            return new PaletteUpdate(true, null, null);
        }
        if (key.type() == KeyType.KeyPgDown) {
            if (!matches.isEmpty()) selectedIndex = Math.min(matches.size() - 1, selectedIndex + 5);
            return new PaletteUpdate(true, null, null);
        }

        if (KeyAliases.getKeyType(KeyAlias.KeyTab) == key.type()) {
            if (!matches.isEmpty()) fillCommand(composer, matches.get(selectedIndex).name(), true);
            close();
            return new PaletteUpdate(true, null, null);
        }

        if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type()) {
            if (!matches.isEmpty()) {
                String trimmed = safeTrim(composer.value());
                if (trimmed.equals("/") || (trimmed.startsWith("/") && !trimmed.contains(" "))) {
                    fillCommand(composer, matches.get(selectedIndex).name(), false);
                }
            }
            close();
            String submit = safeTrim(composer.value());
            return submit.isEmpty() ? new PaletteUpdate(true, null, null) : new PaletteUpdate(true, null, submit);
        }

        UpdateResult<? extends org.flatscrew.latte.Model> inputUpdate = composer.update(rawMsg);
        updateFromComposer(composer);
        matches = SlashCommands.filterForComposer(commands, composer.value());
        clampSelection(matches.size());
        return new PaletteUpdate(true, inputUpdate.command(), null);
    }

    private void updateFromComposer(TextInput composer) {
        String v = composer.value();
        if (v == null) v = "";
        String trimmed = v.stripLeading();
        if (!trimmed.startsWith("/")) {
            close();
            return;
        }
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace >= 0) {
            // Once the user starts typing args, I want it to get out of the way.
            close();
            return;
        }
        open = true;
    }

    private void clampSelection(int matchCount) {
        if (matchCount <= 0) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, matchCount - 1));
    }

    private static boolean isSlashTrigger(KeyPressMessage key) {
        if (key.type() != KeyType.KeyRunes) return false;
        char[] r = key.runes();
        return r != null && r.length == 1 && r[0] == '/';
    }

    private static void fillCommand(TextInput composer, String cmdName, boolean trailingSpace) {
        if (cmdName == null || cmdName.isBlank()) return;
        String v = composer.value();
        if (v == null) v = "";
        String trimmed = v.stripLeading();
        String args = "";
        if (trimmed.startsWith("/")) {
            int firstSpace = trimmed.indexOf(' ');
            if (firstSpace >= 0) args = trimmed.substring(firstSpace).trim();
        }
        String next;
        if (args.isEmpty()) {
            next = trailingSpace ? (cmdName + " ") : cmdName;
        } else {
            next = cmdName + " " + args;
        }
        composer.setValue(next);
        composer.cursorEnd();
    }

    private record Overlay(int topRow, List<String> lines) {}

    // The overlay needs the live composer value to filter. Keep it simple: pass it in.
    void applyOverlay(List<String> baseLines, int innerWidth, int innerHeight, int dividerRow, List<SlashCommand> commands, String composerValue) {
        if (!open) return;
        Overlay overlay = renderOverlay(innerWidth, innerHeight, dividerRow, commands, composerValue);
        if (overlay == null || overlay.lines.isEmpty()) return;
        for (int i = 0; i < overlay.lines.size(); i++) {
            int row = overlay.topRow + i;
            if (row < 0 || row >= baseLines.size()) continue;
            baseLines.set(row, overlay.lines.get(i));
        }
    }

    private Overlay renderOverlay(int innerWidth, int innerHeight, int dividerRow, List<SlashCommand> commands, String composerValue) {
        if (innerWidth < 20 || innerHeight < 10) return null;

        List<SlashCommand> matches = SlashCommands.filterForComposer(commands, composerValue);
        int total = matches.size();
        int maxItems = Math.max(1, Math.min(Math.max(1, total), innerHeight - 9));

        int boxWidth = Math.max(24, Math.min(64, innerWidth - 6));
        int innerBoxWidth = boxWidth - 2;
        int boxHeight = maxItems + 6;
        int bottom = Math.max(1, Math.min(dividerRow, innerHeight - 1));
        int top = Math.max(1, bottom - boxHeight);
        top = Math.min(top, Math.max(1, innerHeight - boxHeight));
        int leftPad = Math.max(0, (innerWidth - boxWidth) / 2);

        int scrollTop = 0;
        if (total > maxItems) {
            scrollTop = Math.max(0, Math.min(total - maxItems, selectedIndex - (maxItems / 2)));
        }

        Style borderStyle = Style.newStyle().foreground(TuiTheme.BORDER);
        Style titleStyle = TuiTheme.sectionHeader();
        Style hintStyle = TuiTheme.hint();

        List<String> box = new ArrayList<>();
        box.add(borderStyle.render("┌" + "─".repeat(boxWidth - 2) + "┐"));

        String title = titleStyle.render("Slash Commands");
        String titleLine = borderStyle.render("│") + TuiTheme.padRight(" " + title, innerBoxWidth) + borderStyle.render("│");
        box.add(titleLine);
        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));

        if (matches.isEmpty()) {
            String none = hintStyle.render("No matches");
            box.add(borderStyle.render("│") + TuiTheme.padRight(" " + none, innerBoxWidth) + borderStyle.render("│"));
            for (int i = 1; i < maxItems; i++) {
                box.add(borderStyle.render("│") + " ".repeat(innerBoxWidth) + borderStyle.render("│"));
            }
        } else {
            for (int i = 0; i < maxItems; i++) {
                int idx = scrollTop + i;
                SlashCommand cmd = (idx >= 0 && idx < total) ? matches.get(idx) : null;

                String rowText;
                if (cmd == null) {
                    rowText = "";
                } else if (idx == selectedIndex) {
                    rowText = "\u001b[7m" + TuiTheme.padRight(" " + cmd.name() + "  " + cmd.description(), innerBoxWidth) + "\u001b[0m";
                } else {
                    String left = titleStyle.render(cmd.name());
                    String right = hintStyle.render(cmd.description());
                    rowText = TuiTheme.padRight(" " + left + "  " + right, innerBoxWidth);
                }
                box.add(borderStyle.render("│") + rowText + borderStyle.render("│"));
            }
        }

        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));
        String help = hintStyle.render("↑/↓ select   tab fill   enter run   esc close");
        box.add(borderStyle.render("│") + TuiTheme.padRight(" " + help, innerBoxWidth) + borderStyle.render("│"));
        box.add(borderStyle.render("└" + "─".repeat(boxWidth - 2) + "┘"));

        List<String> overlayLines = new ArrayList<>(box.size());
        for (String line : box) {
            String padded = " ".repeat(leftPad) + line;
            overlayLines.add(TuiTheme.padRight(padded, innerWidth));
        }
        return new Overlay(top, overlayLines);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
