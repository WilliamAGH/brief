package com.williamcallahan.chatclient.ui.maps;

import com.williamcallahan.chatclient.service.AppleMapsService.PlaceResult;
import com.williamcallahan.chatclient.ui.PaletteOverlay;
import com.williamcallahan.chatclient.ui.TuiTheme;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive overlay for browsing and selecting places from search results.
 * Supports two modes:
 * - Input mode: Shows a search input field for entering a query
 * - Results mode: Shows search results with keyboard navigation
 */
public final class PlacesOverlay {

    private boolean open = false;
    private boolean inputMode = false;
    private StringBuilder inputBuffer = new StringBuilder();
    private List<PlaceResult> places = List.of();
    private String query = "";
    private int selectedIndex = 0;
    private PlaceResult detailPlace = null;

    public boolean isOpen() {
        return open;
    }

    public boolean isInputMode() {
        return inputMode;
    }

    /** Opens the overlay in input mode, prompting user to enter a search query. */
    public void openForInput() {
        this.query = "";
        this.places = List.of();
        this.selectedIndex = 0;
        this.detailPlace = null;
        this.inputMode = true;
        this.inputBuffer = new StringBuilder();
        this.open = true;
    }

    /** Opens the overlay with search results. */
    public void open(String query, List<PlaceResult> results) {
        this.query = query != null ? query : "";
        this.places = results != null ? results : List.of();
        this.selectedIndex = 0;
        this.detailPlace = null;
        this.inputMode = false;
        this.inputBuffer = new StringBuilder();
        this.open = true;
    }

    public void close() {
        this.open = false;
        this.detailPlace = null;
        this.inputMode = false;
        this.inputBuffer = new StringBuilder();
    }

    public String getInputValue() {
        return inputBuffer.toString();
    }

    public PlaceResult selectedPlace() {
        if (
            places.isEmpty() ||
            selectedIndex < 0 ||
            selectedIndex >= places.size()
        ) {
            return null;
        }
        return places.get(selectedIndex);
    }

    public record UpdateResult(
        boolean wasHandled,
        PlaceResult selectedForContext,
        boolean wasClosed,
        String searchQuery
    ) {
        public static UpdateResult notHandled() {
            return new UpdateResult(false, null, false, null);
        }

        public static UpdateResult handled() {
            return new UpdateResult(true, null, false, null);
        }

        public static UpdateResult selected(PlaceResult place) {
            return new UpdateResult(true, place, true, null);
        }

        public static UpdateResult closed() {
            return new UpdateResult(true, null, true, null);
        }

        /** User submitted a search query from input mode. */
        public static UpdateResult search(String query) {
            return new UpdateResult(true, null, false, query);
        }
    }

    public UpdateResult update(KeyPressMessage key) {
        if (!open) return UpdateResult.notHandled();

        // Input mode: handle text input
        if (inputMode) {
            return handleInputMode(key);
        }

        // Detail view navigation
        if (detailPlace != null) {
            if (
                key.type() == KeyType.keyESC ||
                KeyAliases.getKeyType(KeyAlias.KeyBackspace) == key.type()
            ) {
                detailPlace = null;
                return UpdateResult.handled();
            }
            return UpdateResult.handled();
        }

        // List navigation
        switch (key.type()) {
            case keyESC -> {
                close();
                return UpdateResult.closed();
            }
            case KeyUp -> {
                if (selectedIndex > 0) selectedIndex--;
                return UpdateResult.handled();
            }
            case KeyDown -> {
                if (selectedIndex < places.size() - 1) selectedIndex++;
                return UpdateResult.handled();
            }
            case KeyPgUp -> {
                selectedIndex = Math.max(0, selectedIndex - 5);
                return UpdateResult.handled();
            }
            case KeyPgDown -> {
                if (!places.isEmpty()) {
                    selectedIndex = Math.min(places.size() - 1, selectedIndex + 5);
                }
                return UpdateResult.handled();
            }
            case KeyHome -> {
                selectedIndex = 0;
                return UpdateResult.handled();
            }
            case KeyEnd -> {
                if (!places.isEmpty()) {
                    selectedIndex = places.size() - 1;
                }
                return UpdateResult.handled();
            }
            default -> {
            }
        }

        // Enter to select or view details
        if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type()) {
            PlaceResult selected = selectedPlace();
            if (selected != null) {
                return UpdateResult.selected(selected);
            }
            return UpdateResult.handled();
        }

        // Tab to show details
        if (KeyAliases.getKeyType(KeyAlias.KeyTab) == key.type()) {
            PlaceResult selected = selectedPlace();
            if (selected != null) {
                detailPlace = selected;
            }
            return UpdateResult.handled();
        }

        return UpdateResult.handled();
    }

    private UpdateResult handleInputMode(KeyPressMessage key) {
        // Escape closes
        if (key.type() == KeyType.keyESC) {
            close();
            return UpdateResult.closed();
        }

        // Enter submits search
        if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type()) {
            String searchText = inputBuffer.toString().trim();
            if (!searchText.isEmpty()) {
                return UpdateResult.search(searchText);
            }
            return UpdateResult.handled();
        }

        // Backspace removes last char
        if (KeyAliases.getKeyType(KeyAlias.KeyBackspace) == key.type()) {
            if (!inputBuffer.isEmpty()) {
                inputBuffer.deleteCharAt(inputBuffer.length() - 1);
            }
            return UpdateResult.handled();
        }

        // Regular character input
        if (key.type() == KeyType.KeyRunes) {
            char[] runes = key.runes();
            if (runes != null) {
                for (char c : runes) {
                    if (c >= 32) { // Printable characters
                        inputBuffer.append(c);
                    }
                }
            }
            return UpdateResult.handled();
        }

        // Space
        if (key.type() == KeyType.KeySpace) {
            inputBuffer.append(' ');
            return UpdateResult.handled();
        }

        return UpdateResult.handled();
    }

    public PaletteOverlay.Overlay applyOverlay(
        List<String> lines,
        int innerWidth,
        int innerHeight,
        int dividerRow
    ) {
        if (!open) return null;

        if (inputMode) {
            return renderInputOverlay(lines, innerWidth, innerHeight, dividerRow);
        }

        if (detailPlace != null) {
            return renderDetailOverlay(
                lines,
                innerWidth,
                innerHeight,
                dividerRow
            );
        }

        List<PlaceItem> items = places.stream().map(PlaceItem::new).toList();

        PaletteOverlay.Overlay overlay = PaletteOverlay.render(
            items,
            selectedIndex,
            "🔍 Searching for: " + query,
            null,
            innerWidth,
            innerHeight,
            dividerRow
        );

        if (overlay != null) {
            PaletteOverlay.apply(overlay, lines);
        }
        return overlay;
    }

    private PaletteOverlay.Overlay renderInputOverlay(
        List<String> lines,
        int innerWidth,
        int innerHeight,
        int dividerRow
    ) {
        Style borderStyle = Style.newStyle().foreground(TuiTheme.BORDER);
        Style titleStyle = Style.newStyle()
            .foreground(TuiTheme.PRIMARY)
            .bold(true);
        Style labelStyle = Style.newStyle().foreground(TuiTheme.SECONDARY);
        Style inputStyle = Style.newStyle().foreground(TuiTheme.LIGHT);
        Style hintStyle = TuiTheme.hint();

        int boxWidth = Math.max(50, Math.min(70, innerWidth - 6));
        int innerBoxWidth = boxWidth - 2;
        int boxHeight = 8;
        int bottom = Math.max(1, Math.min(dividerRow, innerHeight - 1));
        int top = Math.max(1, bottom - boxHeight);
        int leftPad = Math.max(0, (innerWidth - boxWidth) / 2);

        List<String> box = new ArrayList<>();

        // Top border with title
        box.add(borderStyle.render("┌" + "─".repeat(boxWidth - 2) + "┐"));

        // Title
        String title = "📍 Search Places";
        box.add(
            borderStyle.render("│") +
                TuiTheme.padRight(" " + titleStyle.render(title), innerBoxWidth) +
                borderStyle.render("│")
        );

        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));

        // Prompt
        String prompt = labelStyle.render("Enter location or place to search:");
        box.add(
            borderStyle.render("│") +
                TuiTheme.padRight(" " + prompt, innerBoxWidth) +
                borderStyle.render("│")
        );

        // Input field with cursor
        String inputText = inputBuffer.toString();
        String cursor = "█";
        String displayInput = "> " + inputText + cursor;
        int maxInputLen = innerBoxWidth - 2;
        if (TuiTheme.visualWidth(displayInput) > maxInputLen) {
            // Truncate from the start to show the end of the input
            while (TuiTheme.visualWidth(displayInput) > maxInputLen && displayInput.length() > 3) {
                displayInput = "> " + displayInput.substring(3);
            }
        }
        String inputLine = " " + inputStyle.render(displayInput);
        box.add(
            borderStyle.render("│") +
                TuiTheme.padRight(inputLine, innerBoxWidth) +
                borderStyle.render("│")
        );

        // Empty line
        box.add(
            borderStyle.render("│") +
                " ".repeat(innerBoxWidth) +
                borderStyle.render("│")
        );

        // Footer
        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));
        String footer = hintStyle.render("enter search  esc cancel");
        box.add(
            borderStyle.render("│") +
                TuiTheme.padRight(" " + footer, innerBoxWidth) +
                borderStyle.render("│")
        );
        box.add(borderStyle.render("└" + "─".repeat(boxWidth - 2) + "┘"));

        List<String> overlayLines = new ArrayList<>();
        for (String line : box) {
            String padded = " ".repeat(leftPad) + line;
            overlayLines.add(TuiTheme.padRight(padded, innerWidth));
        }

        PaletteOverlay.Layout layout = new PaletteOverlay.Layout(
            top,
            leftPad,
            boxWidth,
            innerBoxWidth,
            1,
            0,
            0
        );
        PaletteOverlay.Overlay overlay = new PaletteOverlay.Overlay(
            top,
            overlayLines,
            layout
        );
        PaletteOverlay.apply(overlay, lines);
        return overlay;
    }

    private PaletteOverlay.Overlay renderDetailOverlay(
        List<String> lines,
        int innerWidth,
        int innerHeight,
        int dividerRow
    ) {
        if (detailPlace == null) return null;

        Style borderStyle = Style.newStyle().foreground(TuiTheme.BORDER);
        Style titleStyle = TuiTheme.sectionHeader();
        Style nameStyle = Style.newStyle()
            .foreground(TuiTheme.PRIMARY)
            .bold(true);
        Style labelStyle = Style.newStyle().foreground(TuiTheme.SECONDARY);
        Style valueStyle = Style.newStyle().foreground(TuiTheme.LIGHT);
        Style hintStyle = TuiTheme.hint();

        int boxWidth = Math.max(40, Math.min(60, innerWidth - 6));
        int innerBoxWidth = boxWidth - 2;
        int boxHeight = 12;
        int bottom = Math.max(1, Math.min(dividerRow, innerHeight - 1));
        int top = Math.max(1, bottom - boxHeight);
        int leftPad = Math.max(0, (innerWidth - boxWidth) / 2);

        List<String> box = new ArrayList<>();

        // Top border
        box.add(borderStyle.render("┌" + "─".repeat(boxWidth - 2) + "┐"));

        // Title
        String icon = getCategoryIcon(detailPlace.category());
        String title = icon + " " + detailPlace.name();
        if (TuiTheme.visualWidth(title) > innerBoxWidth - 2) {
            title = TuiTheme.truncate(title, innerBoxWidth - 2);
        }
        box.add(
            borderStyle.render("│") +
                TuiTheme.padRight(
                    " " + nameStyle.render(title),
                    innerBoxWidth
                ) +
                borderStyle.render("│")
        );

        // Category
        if (!detailPlace.category().isBlank()) {
            box.add(
                borderStyle.render("│") +
                    TuiTheme.padRight(
                        " " + titleStyle.render(detailPlace.category()),
                        innerBoxWidth
                    ) +
                    borderStyle.render("│")
            );
        }

        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));

        // Address
        if (!detailPlace.address().isBlank()) {
            String addrLabel = labelStyle.render("Address: ");
            String addrValue = valueStyle.render(
                truncateField(detailPlace.address(), innerBoxWidth - 12)
            );
            box.add(
                borderStyle.render("│") +
                    TuiTheme.padRight(
                        " " + addrLabel + addrValue,
                        innerBoxWidth
                    ) +
                    borderStyle.render("│")
            );
        }

        // Coordinates
        if (detailPlace.hasCoordinates()) {
            String coordLabel = labelStyle.render("Coords:  ");
            String coordValue = valueStyle.render(
                String.format(
                    "%.5f, %.5f",
                    detailPlace.latitude(),
                    detailPlace.longitude()
                )
            );
            box.add(
                borderStyle.render("│") +
                    TuiTheme.padRight(
                        " " + coordLabel + coordValue,
                        innerBoxWidth
                    ) +
                    borderStyle.render("│")
            );
        }

        // Phone
        if (detailPlace.phone() != null && !detailPlace.phone().isBlank()) {
            String phoneLabel = labelStyle.render("Phone:   ");
            String phoneValue = valueStyle.render(detailPlace.phone());
            box.add(
                borderStyle.render("│") +
                    TuiTheme.padRight(
                        " " + phoneLabel + phoneValue,
                        innerBoxWidth
                    ) +
                    borderStyle.render("│")
            );
        }

        // URL
        if (detailPlace.url() != null && !detailPlace.url().isBlank()) {
            String urlLabel = labelStyle.render("Website: ");
            String urlValue = valueStyle.render(
                truncateField(detailPlace.url(), innerBoxWidth - 12)
            );
            box.add(
                borderStyle.render("│") +
                    TuiTheme.padRight(
                        " " + urlLabel + urlValue,
                        innerBoxWidth
                    ) +
                    borderStyle.render("│")
            );
        }

        // Padding to fill box
        while (box.size() < boxHeight - 2) {
            box.add(
                borderStyle.render("│") +
                    " ".repeat(innerBoxWidth) +
                    borderStyle.render("│")
            );
        }

        // Footer
        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));
        String footer = hintStyle.render("enter select  esc back");
        box.add(
            borderStyle.render("│") +
                TuiTheme.padRight(" " + footer, innerBoxWidth) +
                borderStyle.render("│")
        );
        box.add(borderStyle.render("└" + "─".repeat(boxWidth - 2) + "┘"));

        List<String> overlayLines = new ArrayList<>();
        for (String line : box) {
            String padded = " ".repeat(leftPad) + line;
            overlayLines.add(TuiTheme.padRight(padded, innerWidth));
        }

        PaletteOverlay.Layout layout = new PaletteOverlay.Layout(
            top,
            leftPad,
            boxWidth,
            innerBoxWidth,
            boxHeight - 6,
            0,
            1
        );
        PaletteOverlay.Overlay overlay = new PaletteOverlay.Overlay(
            top,
            overlayLines,
            layout
        );
        PaletteOverlay.apply(overlay, lines);
        return overlay;
    }

    private String truncateField(String value, int maxWidth) {
        if (value == null) return "";
        if (value.length() <= maxWidth) return value;
        return value.substring(0, maxWidth - 3) + "...";
    }

    private String getCategoryIcon(String category) {
        if (category == null || category.isBlank()) return "📍";
        String lower = category.toLowerCase();
        if (lower.contains("coffee") || lower.contains("cafe")) return "☕";
        if (lower.contains("restaurant") || lower.contains("food")) return "🍽";
        if (lower.contains("bar") || lower.contains("pub")) return "🍺";
        if (lower.contains("hotel") || lower.contains("lodging")) return "🏨";
        if (lower.contains("gas") || lower.contains("fuel")) return "⛽";
        if (
            lower.contains("hospital") || lower.contains("medical")
        ) return "🏥";
        if (lower.contains("pharmacy") || lower.contains("drug")) return "💊";
        if (lower.contains("bank") || lower.contains("atm")) return "🏦";
        if (
            lower.contains("grocery") || lower.contains("supermarket")
        ) return "🛒";
        if (lower.contains("gym") || lower.contains("fitness")) return "💪";
        if (lower.contains("park")) return "🌳";
        if (lower.contains("museum") || lower.contains("gallery")) return "🏛";
        if (lower.contains("theater") || lower.contains("cinema")) return "🎭";
        if (lower.contains("airport")) return "✈";
        if (lower.contains("train") || lower.contains("station")) return "🚉";
        if (
            lower.contains("school") || lower.contains("university")
        ) return "🎓";
        if (lower.contains("library")) return "📚";
        if (
            lower.contains("shop") ||
            lower.contains("store") ||
            lower.contains("mall")
        ) return "🛍";
        return "📍";
    }
}
