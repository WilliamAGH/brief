package com.williamcallahan.lattetui;

import com.williamcallahan.lattetui.slash.SlashCommand;
import com.williamcallahan.lattetui.slash.SlashCommands;
import org.flatscrew.latte.Command;
import org.flatscrew.latte.Message;
import org.flatscrew.latte.UpdateResult;
import org.flatscrew.latte.input.key.KeyAliases;
import org.flatscrew.latte.input.key.KeyAliases.KeyAlias;
import org.flatscrew.latte.input.key.KeyType;
import org.flatscrew.latte.message.KeyPressMessage;
import org.flatscrew.latte.spice.textinput.TextInput;

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

    void applyOverlay(List<String> baseLines, int innerWidth, int innerHeight, int dividerRow, List<SlashCommand> commands, String composerValue) {
        if (!open) return;
        List<SlashCommand> matches = SlashCommands.filterForComposer(commands, composerValue);
        PaletteOverlay.Overlay overlay = PaletteOverlay.render(matches, selectedIndex, "Slash Commands", null, innerWidth, innerHeight, dividerRow);
        PaletteOverlay.apply(overlay, baseLines);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
