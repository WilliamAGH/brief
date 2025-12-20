package com.williamcallahan.ui;

import com.williamcallahan.tui4j.compat.bubbletea.lipgloss.Style;

import java.util.ArrayList;
import java.util.List;

/** Renders a selection overlay for any list of PaletteItems. */
public final class PaletteOverlay {

    private PaletteOverlay() {}

    /**
     * Renders items into an overlay box positioned above the divider row.
     *
     * @param items         Items to display (already filtered by caller)
     * @param selectedIndex Currently selected index
     * @param title         Box title
     * @param searchQuery   Optional search filter to display (null to hide)
     * @param innerWidth    Available width inside the main border
     * @param innerHeight   Available height inside the main border
     * @param dividerRow    Row where the divider sits (overlay anchors above this)
     * @return Rendered lines with top row position, or null if too small
     */
    public static Overlay render(
            List<? extends PaletteItem> items,
            int selectedIndex,
            String title,
            String searchQuery,
            int innerWidth,
            int innerHeight,
            int dividerRow) {

        if (innerWidth < 20 || innerHeight < 10) return null;

        int total = items.size();
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

        String titleRendered = titleStyle.render(title);
        String titleLine = borderStyle.render("│") + TuiTheme.padRight(" " + titleRendered, innerBoxWidth) + borderStyle.render("│");
        box.add(titleLine);
        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));

        if (items.isEmpty()) {
            String none = hintStyle.render("No matches");
            box.add(borderStyle.render("│") + TuiTheme.padRight(" " + none, innerBoxWidth) + borderStyle.render("│"));
            for (int i = 1; i < maxItems; i++) {
                box.add(borderStyle.render("│") + " ".repeat(innerBoxWidth) + borderStyle.render("│"));
            }
        } else {
            for (int i = 0; i < maxItems; i++) {
                int idx = scrollTop + i;
                PaletteItem item = (idx >= 0 && idx < total) ? items.get(idx) : null;

                String rowText;
                if (item == null) {
                    rowText = "";
                } else if (idx == selectedIndex) {
                    rowText = "\u001b[7m" + TuiTheme.padRight(" " + item.name() + "  " + item.description(), innerBoxWidth) + "\u001b[0m";
                } else {
                    String left = titleStyle.render(item.name());
                    String right = hintStyle.render(item.description());
                    rowText = TuiTheme.padRight(" " + left + "  " + right, innerBoxWidth);
                }
                box.add(borderStyle.render("│") + rowText + borderStyle.render("│"));
            }
        }

        box.add(borderStyle.render("├" + "─".repeat(boxWidth - 2) + "┤"));

        // Footer: search field (if active) + help hints
        String searchPart = "";
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchPart = titleStyle.render(searchQuery) + "  ";
        }
        String help = hintStyle.render("↑/↓  enter  esc  type to filter");
        String footer = searchPart + help;
        box.add(borderStyle.render("│") + TuiTheme.padRight(" " + footer, innerBoxWidth) + borderStyle.render("│"));
        box.add(borderStyle.render("└" + "─".repeat(boxWidth - 2) + "┘"));

        List<String> overlayLines = new ArrayList<>(box.size());
        for (String line : box) {
            String padded = " ".repeat(leftPad) + line;
            overlayLines.add(TuiTheme.padRight(padded, innerWidth));
        }
        Layout layout = new Layout(top, leftPad, boxWidth, innerBoxWidth, maxItems, scrollTop, total);
        return new Overlay(top, overlayLines, layout);
    }

    /** Applies overlay lines onto the base screen buffer. */
    public static void apply(Overlay overlay, List<String> baseLines) {
        if (overlay == null || overlay.lines().isEmpty()) return;
        for (int i = 0; i < overlay.lines().size(); i++) {
            int row = overlay.topRow() + i;
            if (row < 0 || row >= baseLines.size()) continue;
            baseLines.set(row, overlay.lines().get(i));
        }
    }

    public record Overlay(int topRow, List<String> lines, Layout layout) {}

    public record Layout(
            int topRow,
            int leftCol,
            int boxWidth,
            int innerBoxWidth,
            int maxItems,
            int scrollTop,
            int totalItems
    ) {
        int boxHeight() {
            return maxItems + 6;
        }

        int itemRowStart() {
            return topRow + 3;
        }

        int visibleItemCount() {
            if (totalItems <= 0 || scrollTop >= totalItems) {
                return 0;
            }
            return Math.min(maxItems, totalItems - scrollTop);
        }

        boolean contains(int column, int row) {
            return column >= leftCol
                    && row >= topRow
                    && column < leftCol + boxWidth
                    && row < topRow + boxHeight();
        }

        int itemIndexAt(int column, int row) {
            int itemTop = itemRowStart();
            int itemBottom = itemTop + maxItems;
            int innerLeft = leftCol + 1;
            int innerRight = innerLeft + innerBoxWidth;
            if (row < itemTop || row >= itemBottom) {
                return -1;
            }
            if (column < innerLeft || column >= innerRight) {
                return -1;
            }
            int visibleIndex = row - itemTop;
            int index = scrollTop + visibleIndex;
            return index < totalItems ? index : -1;
        }
    }
}
