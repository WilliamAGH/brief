package com.williamcallahan.chatclient.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class PasteRoutingModelTest {

    @Test
    void routesPasteToRunesWhenStrategySaysToRoute() {
        RecordingModel delegate = new RecordingModel();
        PasteRoutingModel model = new PasteRoutingModel(delegate, ignored -> true);

        model.update(new PasteMessage("coffee\nshop"));

        KeyPressMessage keyPress = assertInstanceOf(
            KeyPressMessage.class,
            delegate.lastMessage
        );
        assertEquals("coffee shop", new String(keyPress.runes()));
    }

    @Test
    void leavesPasteUntouchedWhenStrategyDeclinesRoute() {
        RecordingModel delegate = new RecordingModel();
        PasteRoutingModel model = new PasteRoutingModel(delegate, ignored -> false);

        model.update(new PasteMessage("coffee"));

        assertInstanceOf(PasteMessage.class, delegate.lastMessage);
    }

    @Test
    void applyComposerStylesClearsFocusedCursorLineBackground() {
        Textarea composer = new Textarea();
        Object before = styleBackground(composer.focusedStyle().cursorLine());
        assertFalse(before instanceof NoColor);

        PasteRoutingModel.applyComposerStyles(composer);

        Object after = styleBackground(composer.focusedStyle().cursorLine());
        assertTrue(after instanceof NoColor);
    }

    @Test
    void normalizeForSingleLineInputTrimsBoundaryWhitespace() {
        assertEquals(
            "coffee shop",
            PasteRoutingModel.normalizeForSingleLineInput("\n coffee\tshop \r\n")
        );
    }

    private static final class RecordingModel implements Model {

        private Message lastMessage;

        @Override
        public Command init() {
            return null;
        }

        @Override
        public UpdateResult<? extends Model> update(Message msg) {
            lastMessage = msg;
            return UpdateResult.from(this);
        }

        @Override
        public String view() {
            return "";
        }
    }

    private static Object styleBackground(
        com.williamcallahan.tui4j.compat.lipgloss.Style style
    ) {
        try {
            Field background = style.getClass().getDeclaredField("background");
            background.setAccessible(true);
            return background.get(style);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "Unable to inspect style background",
                e
            );
        }
    }
}
