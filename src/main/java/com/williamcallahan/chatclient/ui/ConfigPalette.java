package com.williamcallahan.chatclient.ui;

import com.williamcallahan.chatclient.Config;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.lipgloss.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Config settings palette overlay. */
final class ConfigPalette {

    record ConfigItem(
        String name,
        String envVar,
        Supplier<String> valueSupplier,
        Consumer<String> valueSetter,
        boolean isToggle,
        boolean readOnly,
        boolean isSensitive
    ) implements PaletteItem {

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            String value = valueSupplier.get();
            if (value == null || value.isBlank()) return "(not set)";
            return value;
        }

        static ConfigItem toggle(String name, String envVar, Supplier<String> getter, Consumer<String> setter) {
            return new ConfigItem(name, envVar, getter, setter, true, false, false);
        }

        static ConfigItem editable(String name, String envVar, Supplier<String> getter, Consumer<String> setter) {
            return new ConfigItem(name, envVar, getter, setter, false, false, false);
        }

        static ConfigItem sensitive(String name, String envVar, Supplier<String> getter, Consumer<String> setter) {
            return new ConfigItem(name, envVar, getter, setter, false, false, true);
        }

        static ConfigItem readOnly(String name, String envVar, Supplier<String> getter) {
            return new ConfigItem(name, envVar, getter, v -> {}, false, true, false);
        }
    }

    record PaletteResult(boolean handled) {}

    private boolean open = false;
    private int selectedIndex = 0;
    private List<ConfigItem> items = List.of();
    private Config config;

    // Editing state
    private boolean editing = false;
    private ConfigItem editingItem = null;
    private StringBuilder editBuffer = new StringBuilder();
    private int cursorPos = 0;

    boolean isOpen() {
        return open;
    }

    boolean isEditing() {
        return editing;
    }

    void open(Config config) {
        this.config = config;
        this.items = buildItems(config);
        this.selectedIndex = 0;
        this.open = true;
        this.editing = false;
        this.editingItem = null;
    }

    void close() {
        open = false;
        selectedIndex = 0;
        editing = false;
        editingItem = null;
    }

    void refresh() {
        if (config != null) {
            this.items = buildItems(config);
        }
    }

    private void startEditing(ConfigItem item) {
        this.editing = true;
        this.editingItem = item;
        this.editBuffer = new StringBuilder();
        // For sensitive items, start empty; for others, pre-fill with current value
        if (!item.isSensitive()) {
            String current = item.valueSupplier().get();
            if (current != null && !current.isBlank()) {
                editBuffer.append(current);
            }
        }
        this.cursorPos = editBuffer.length();
    }

    private void cancelEditing() {
        this.editing = false;
        this.editingItem = null;
        this.editBuffer = new StringBuilder();
        this.cursorPos = 0;
    }

    private void saveEditing() {
        if (editingItem != null) {
            editingItem.valueSetter().accept(editBuffer.toString().trim());
            refresh();
        }
        cancelEditing();
    }

    private static List<ConfigItem> buildItems(Config config) {
        return List.of(
            ConfigItem.sensitive(
                "API Key",
                "OPENAI_API_KEY",
                () -> maskSensitive(config.resolveApiKey()),
                config::setApiKey
            ),
            ConfigItem.editable(
                "Base URL",
                "OPENAI_BASE_URL",
                () -> nullToNotSet(config.resolveBaseUrl()),
                config::setBaseUrl
            ),
            ConfigItem.editable(
                "Model",
                "LLM_MODEL",
                () -> nullToNotSet(config.resolveModel()),
                config::setModel
            ),
            ConfigItem.sensitive(
                "Apple Maps Token",
                "APPLE_MAPS_TOKEN",
                () -> maskSensitive(config.resolveAppleMapsToken()),
                config::setAppleMapsToken
            ),
            ConfigItem.toggle(
                "Summarization",
                "",
                () -> config.isSummaryEnabled() ? "enabled" : "disabled",
                v -> config.setSummaryDisabled("disabled".equalsIgnoreCase(v))
            ),
            ConfigItem.editable(
                "Summary Tokens",
                "",
                () -> String.valueOf(config.getSummaryTargetTokens()),
                v -> {
                    try {
                        config.setSummaryTargetTokens(Integer.parseInt(v.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            ),
            ConfigItem.readOnly(
                "Config Priority",
                "",
                () -> config.priority().name().toLowerCase()
            )
        );
    }

    private static String maskSensitive(String value) {
        if (value == null || value.isBlank()) return "(not set)";
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private static String nullToNotSet(String value) {
        return (value == null || value.isBlank()) ? "(not set)" : value;
    }

    PaletteResult update(KeyPressMessage key) {
        if (!open) return new PaletteResult(false);

        // Handle editing mode
        if (editing) {
            return handleEditingKey(key);
        }

        if (key.type() == KeyType.keyESC) {
            close();
            return new PaletteResult(true);
        }

        int total = items.size();

        if (key.type() == KeyType.KeyUp && total > 0) {
            selectedIndex = Math.max(0, selectedIndex - 1);
            return new PaletteResult(true);
        }
        if (key.type() == KeyType.KeyDown && total > 0) {
            selectedIndex = Math.min(total - 1, selectedIndex + 1);
            return new PaletteResult(true);
        }
        if (key.type() == KeyType.KeyPgUp && total > 0) {
            selectedIndex = Math.max(0, selectedIndex - 5);
            return new PaletteResult(true);
        }
        if (key.type() == KeyType.KeyPgDown && total > 0) {
            selectedIndex = Math.min(total - 1, selectedIndex + 5);
            return new PaletteResult(true);
        }

        ConfigItem item = (selectedIndex >= 0 && selectedIndex < total) ? items.get(selectedIndex) : null;

        // Space toggles boolean items
        if (key.type() == KeyType.KeySpace && item != null && item.isToggle() && !item.readOnly()) {
            String current = item.valueSupplier().get();
            String next = "enabled".equalsIgnoreCase(current) ? "disabled" : "enabled";
            item.valueSetter().accept(next);
            refresh();
            return new PaletteResult(true);
        }

        // Enter opens text edit for editable items
        if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type() && item != null && !item.readOnly() && !item.isToggle()) {
            startEditing(item);
            return new PaletteResult(true);
        }

        return new PaletteResult(true);
    }

    private PaletteResult handleEditingKey(KeyPressMessage key) {
        // ESC cancels editing
        if (key.type() == KeyType.keyESC) {
            cancelEditing();
            return new PaletteResult(true);
        }

        // Enter saves
        if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type()) {
            saveEditing();
            return new PaletteResult(true);
        }

        // Backspace
        if (key.type() == KeyType.keyBS || key.type() == KeyType.keyDEL) {
            if (cursorPos > 0) {
                editBuffer.deleteCharAt(cursorPos - 1);
                cursorPos--;
            }
            return new PaletteResult(true);
        }

        // Left arrow
        if (key.type() == KeyType.KeyLeft) {
            if (cursorPos > 0) cursorPos--;
            return new PaletteResult(true);
        }

        // Right arrow
        if (key.type() == KeyType.KeyRight) {
            if (cursorPos < editBuffer.length()) cursorPos++;
            return new PaletteResult(true);
        }

        // Home
        if (key.type() == KeyType.KeyHome) {
            cursorPos = 0;
            return new PaletteResult(true);
        }

        // End
        if (key.type() == KeyType.KeyEnd) {
            cursorPos = editBuffer.length();
            return new PaletteResult(true);
        }

        // Typing characters
        if (key.type() == KeyType.KeyRunes) {
            char[] runes = key.runes();
            if (runes != null && runes.length > 0 && runes[0] >= 32) {
                for (char c : runes) {
                    editBuffer.insert(cursorPos, c);
                    cursorPos++;
                }
            }
            return new PaletteResult(true);
        }

        return new PaletteResult(true);
    }

    /**
     * Handles pasted content when in editing mode.
     * @return true if the paste was handled, false if not editing
     */
    boolean handlePaste(String content) {
        if (!editing || content == null) return false;
        // Strip newlines - config values are single-line
        String cleaned = content.replace("\r\n", "").replace("\n", "").replace("\r", "");
        for (char c : cleaned.toCharArray()) {
            if (c >= 32) {
                editBuffer.insert(cursorPos, c);
                cursorPos++;
            }
        }
        return true;
    }

    PaletteResult click(int index) {
        if (!open) return new PaletteResult(false);
        if (editing) return new PaletteResult(true); // Ignore clicks while editing
        if (items.isEmpty()) {
            close();
            return new PaletteResult(true);
        }
        int clamped = Math.max(0, Math.min(index, items.size() - 1));
        selectedIndex = clamped;
        ConfigItem item = items.get(clamped);

        if (item.isToggle() && !item.readOnly()) {
            String current = item.valueSupplier().get();
            String next = "enabled".equalsIgnoreCase(current) ? "disabled" : "enabled";
            item.valueSetter().accept(next);
            refresh();
            return new PaletteResult(true);
        }

        if (!item.readOnly() && !item.isToggle()) {
            startEditing(item);
            return new PaletteResult(true);
        }

        return new PaletteResult(true);
    }

    PaletteOverlay.Overlay applyOverlay(List<String> baseLines, int innerWidth, int innerHeight, int dividerRow) {
        if (!open) return null;

        if (editing && editingItem != null) {
            PaletteOverlay.Overlay overlay = renderEditOverlay(innerWidth, innerHeight, dividerRow);
            PaletteOverlay.apply(overlay, baseLines);
            return overlay;
        }

        int clampedIndex = Math.min(selectedIndex, Math.max(0, items.size() - 1));
        PaletteOverlay.Overlay overlay = PaletteOverlay.render(
            items, clampedIndex, "Settings", null, "↑/↓  space toggle  enter edit  esc",
            innerWidth, innerHeight, dividerRow
        );
        PaletteOverlay.apply(overlay, baseLines);
        return overlay;
    }

    private PaletteOverlay.Overlay renderEditOverlay(int innerWidth, int innerHeight, int dividerRow) {
        int boxWidth = Math.max(30, Math.min(50, innerWidth - 6));
        int innerBoxWidth = boxWidth - 2;
        int boxHeight = 6;
        int bottom = Math.max(1, Math.min(dividerRow, innerHeight - 1));
        int top = Math.max(1, bottom - boxHeight);
        int leftPad = Math.max(0, (innerWidth - boxWidth) / 2);

        Style borderStyle = Style.newStyle().foreground(TuiTheme.BORDER);
        Style titleStyle = TuiTheme.sectionHeader();
        Style hintStyle = TuiTheme.hint();
        Style textStyle = Style.newStyle().foreground(TuiTheme.PRIMARY);

        List<String> box = new ArrayList<>();

        // Top border
        box.add(borderStyle.render("┌" + "─".repeat(boxWidth - 2) + "┐"));

        // Title row
        String title = "Edit: " + editingItem.name();
        box.add(borderStyle.render("│") +
            TuiTheme.padRight(" " + titleStyle.render(title), innerBoxWidth) +
            borderStyle.render("│"));

        // Divider
        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));

        // Input row with cursor
        String inputText = editBuffer.toString();
        int maxInputWidth = innerBoxWidth - 3; // Leave room for prompt and padding
        String displayText;
        int displayCursor = cursorPos;

        if (inputText.length() > maxInputWidth) {
            // Scroll the view to keep cursor visible
            int start = Math.max(0, cursorPos - maxInputWidth + 5);
            int end = Math.min(inputText.length(), start + maxInputWidth);
            displayText = inputText.substring(start, end);
            displayCursor = cursorPos - start;
        } else {
            displayText = inputText;
        }

        StringBuilder inputRow = new StringBuilder();
        inputRow.append(" ");
        for (int i = 0; i < displayText.length(); i++) {
            if (i == displayCursor) {
                inputRow.append("\u001b[7m").append(displayText.charAt(i)).append("\u001b[0m");
            } else {
                inputRow.append(textStyle.render(String.valueOf(displayText.charAt(i))));
            }
        }
        if (displayCursor >= displayText.length()) {
            inputRow.append("\u001b[7m \u001b[0m");
        }

        box.add(borderStyle.render("│") +
            TuiTheme.padRight(inputRow.toString(), innerBoxWidth) +
            borderStyle.render("│"));

        // Footer divider
        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));

        // Help footer
        String help = hintStyle.render("enter save  esc cancel");
        box.add(borderStyle.render("│") +
            TuiTheme.padRight(" " + help, innerBoxWidth) +
            borderStyle.render("│"));

        // Bottom border
        box.add(borderStyle.render("└" + "─".repeat(boxWidth - 2) + "┘"));

        List<String> overlayLines = new ArrayList<>(box.size());
        for (String line : box) {
            String padded = " ".repeat(leftPad) + line;
            overlayLines.add(TuiTheme.padRight(padded, innerWidth));
        }

        PaletteOverlay.Layout layout = new PaletteOverlay.Layout(
            top, leftPad, boxWidth, innerBoxWidth, 1, 0, 1
        );
        return new PaletteOverlay.Overlay(top, overlayLines, layout);
    }
}
