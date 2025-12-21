package com.williamcallahan.chatclient.ui;

import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbletea.message.KeyPressMessage;

import java.util.List;

/** Model selection palette overlay with type-to-filter. */
final class ModelPalette {

    record ModelChoice(String id) implements PaletteItem {
        @Override public String name() { return id; }
        @Override public String description() { return ""; }
    }

    record PaletteResult(boolean handled, String selectedModel) {}

    private boolean open = false;
    private int selectedIndex = 0;
    private List<ModelChoice> allChoices = List.of();
    private String filter = "";

    boolean isOpen() {
        return open;
    }

    void open(List<String> modelIds) {
        this.allChoices = modelIds.stream().map(ModelChoice::new).toList();
        this.selectedIndex = 0;
        this.filter = "";
        this.open = true;
    }

    void close() {
        open = false;
        selectedIndex = 0;
        filter = "";
    }

    private List<ModelChoice> filtered() {
        if (filter.isEmpty()) return allChoices;
        String lower = filter.toLowerCase();
        return allChoices.stream()
            .filter(c -> c.id().toLowerCase().contains(lower))
            .toList();
    }

    PaletteResult update(KeyPressMessage key) {
        if (!open) return new PaletteResult(false, null);

        if (key.type() == KeyType.keyESC) {
            close();
            return new PaletteResult(true, null);
        }

        List<ModelChoice> matches = filtered();
        int total = matches.size();

        if (key.type() == KeyType.KeyUp && total > 0) {
            selectedIndex = Math.max(0, selectedIndex - 1);
            return new PaletteResult(true, null);
        }
        if (key.type() == KeyType.KeyDown && total > 0) {
            selectedIndex = Math.min(total - 1, selectedIndex + 1);
            return new PaletteResult(true, null);
        }
        if (key.type() == KeyType.KeyPgUp && total > 0) {
            selectedIndex = Math.max(0, selectedIndex - 5);
            return new PaletteResult(true, null);
        }
        if (key.type() == KeyType.KeyPgDown && total > 0) {
            selectedIndex = Math.min(total - 1, selectedIndex + 5);
            return new PaletteResult(true, null);
        }
        if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type() && total > 0) {
            String selected = matches.get(selectedIndex).id();
            close();
            return new PaletteResult(true, selected);
        }

        // Backspace: remove last character from filter
        if ((key.type() == KeyType.keyBS || key.type() == KeyType.keyDEL) && !filter.isEmpty()) {
            filter = filter.substring(0, filter.length() - 1);
            selectedIndex = 0;
            return new PaletteResult(true, null);
        }

        // Typing: append printable characters to filter
        if (key.type() == KeyType.KeyRunes) {
            char[] runes = key.runes();
            if (runes != null && runes.length > 0 && runes[0] >= 32) {
                filter += new String(runes);
                selectedIndex = 0;
                return new PaletteResult(true, null);
            }
        }

        return new PaletteResult(true, null);
    }

    PaletteResult click(int index) {
        if (!open) return new PaletteResult(false, null);
        List<ModelChoice> matches = filtered();
        if (matches.isEmpty()) {
            close();
            return new PaletteResult(true, null);
        }
        int clamped = Math.max(0, Math.min(index, matches.size() - 1));
        String selected = matches.get(clamped).id();
        close();
        return new PaletteResult(true, selected);
    }

    PaletteOverlay.Overlay applyOverlay(List<String> baseLines, int innerWidth, int innerHeight, int dividerRow) {
        if (!open) return null;
        List<ModelChoice> matches = filtered();
        int clampedIndex = Math.min(selectedIndex, Math.max(0, matches.size() - 1));
        PaletteOverlay.Overlay overlay = PaletteOverlay.render(
            matches, clampedIndex, "Select Model", filter, innerWidth, innerHeight, dividerRow
        );
        PaletteOverlay.apply(overlay, baseLines);
        return overlay;
    }
}
