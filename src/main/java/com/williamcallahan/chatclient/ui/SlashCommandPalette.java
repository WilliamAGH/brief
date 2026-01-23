package com.williamcallahan.chatclient.ui;

import com.williamcallahan.chatclient.ui.slash.SlashCommand;
import com.williamcallahan.chatclient.ui.slash.SlashCommands;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;

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

    PaletteUpdate openFromMouse(Textarea composer, List<SlashCommand> commands) {
        if (open) return new PaletteUpdate(true, null, null);
        composer.setValue("/");
        updateFromComposer(composer);
        clampSelection(SlashCommands.filterForComposer(commands, composer.value()).size());
        return new PaletteUpdate(true, null, null);
    }

    PaletteUpdate update(KeyPressMessage key, Message rawMsg, Textarea composer, boolean waiting, List<SlashCommand> commands) {
        if (waiting) return new PaletteUpdate(false, null, null);

        if (!open) {
            if (!isSlashTrigger(key)) return new PaletteUpdate(false, null, null);
            UpdateResult<? extends com.williamcallahan.tui4j.compat.bubbletea.Model> inputUpdate = composer.update(rawMsg);
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

        UpdateResult<? extends com.williamcallahan.tui4j.compat.bubbletea.Model> inputUpdate = composer.update(rawMsg);
        updateFromComposer(composer);
        matches = SlashCommands.filterForComposer(commands, composer.value());
        clampSelection(matches.size());
        return new PaletteUpdate(true, inputUpdate.command(), null);
    }

    PaletteUpdate click(int index, Textarea composer, List<SlashCommand> commands) {
        if (!open) return new PaletteUpdate(false, null, null);

        List<SlashCommand> matches = SlashCommands.filterForComposer(commands, composer.value());
        clampSelection(matches.size());
        if (matches.isEmpty()) {
            close();
            return new PaletteUpdate(true, null, null);
        }
        selectedIndex = Math.max(0, Math.min(index, matches.size() - 1));
        String submit = matches.get(selectedIndex).name();
        close();
        return (submit == null || submit.isBlank())
            ? new PaletteUpdate(true, null, null)
            : new PaletteUpdate(true, null, submit);
    }

    private void updateFromComposer(Textarea composer) {
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

    private static void fillCommand(Textarea composer, String cmdName, boolean trailingSpace) {
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
    }

    PaletteOverlay.Overlay applyOverlay(List<String> baseLines, int innerWidth, int innerHeight, int dividerRow,
                                        List<SlashCommand> commands, String composerValue) {
        if (!open) return null;
        List<SlashCommand> matches = SlashCommands.filterForComposer(commands, composerValue);
        PaletteOverlay.Overlay overlay = PaletteOverlay.render(matches, selectedIndex, "Slash Commands", null, innerWidth, innerHeight, dividerRow);
        PaletteOverlay.apply(overlay, baseLines);
        return overlay;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
