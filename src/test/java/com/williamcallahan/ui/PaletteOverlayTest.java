package com.williamcallahan.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaletteOverlayTest {

    private record TestItem(String name) implements PaletteItem {
        @Override
        public String description() {
            return "";
        }
    }

    @Test
    void test_ItemIndexAt_FirstRow() {
        List<TestItem> items = List.of(new TestItem("One"), new TestItem("Two"));
        PaletteOverlay.Overlay overlay = PaletteOverlay.render(items, 0, "Test", null, 60, 20, 10);
        assertNotNull(overlay);
        PaletteOverlay.Layout layout = overlay.layout();
        int col = layout.leftCol() + 1;
        int row = layout.itemRowStart();
        assertEquals(0, layout.itemIndexAt(col, row));
    }
}
