package com.williamcallahan.chatclient.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.williamcallahan.tui4j.compat.bubbletea.BatchMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import com.williamcallahan.tui4j.message.CopyToClipboardMessage;
import com.williamcallahan.tui4j.message.OpenUrlMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MouseSelectionControllerTest {

    @Test
    void dragSelectionQueuesClipboardCopyThroughTui4j() {
        String previous = System.getProperty("tui4j.clipboard.disabled");
        System.setProperty("tui4j.clipboard.disabled", "true");
        try {
            MouseSelectionController controller = new MouseSelectionController();
            controller.updateHistoryMapping(
                0,
                0,
                0,
                List.of("first line"),
                List.of("first line")
            );

            controller.handle(mouse(0, 0, MouseAction.MouseActionPress, MouseButton.MouseButtonLeft));
            controller.handle(mouse(5, 0, MouseAction.MouseActionMotion, MouseButton.MouseButtonLeft));

            Command command = controller.handle(
                mouse(5, 0, MouseAction.MouseActionRelease, MouseButton.MouseButtonNone)
            );

            CopyToClipboardMessage copy = findMessage(
                command,
                CopyToClipboardMessage.class
            );
            assertEquals("first", copy.text());
            assertEquals("COPIED", controller.transientStatus(System.currentTimeMillis()));
        } finally {
            restoreClipboardProperty(previous);
        }
    }

    @Test
    void clickingUrlQueuesOpenUrlThroughTui4j() {
        MouseSelectionController controller = new MouseSelectionController();
        controller.updateHistoryMapping(
            0,
            0,
            0,
            List.of("visit example.com now"),
            List.of("visit example.com now")
        );

        controller.handle(mouse(6, 0, MouseAction.MouseActionPress, MouseButton.MouseButtonLeft));
        Command command = controller.handle(
            mouse(6, 0, MouseAction.MouseActionRelease, MouseButton.MouseButtonNone)
        );

        OpenUrlMessage openUrl = findMessage(command, OpenUrlMessage.class);
        assertEquals("https://example.com", openUrl.url());
    }

    private static MouseMessage mouse(
        int column,
        int row,
        MouseAction action,
        MouseButton button
    ) {
        return new MouseMessage(
            column,
            row,
            false,
            false,
            false,
            action,
            button
        );
    }

    private static <T extends Message> T findMessage(
        Command command,
        Class<T> messageType
    ) {
        BatchMessage batch = assertInstanceOf(BatchMessage.class, command.execute());
        return Arrays.stream(batch.commands())
            .map(Command::execute)
            .filter(Objects::nonNull)
            .filter(messageType::isInstance)
            .map(messageType::cast)
            .findFirst()
            .orElseThrow(() ->
                new AssertionError("Missing message of type " + messageType.getSimpleName())
            );
    }

    private static void restoreClipboardProperty(String previous) {
        if (previous == null) {
            System.clearProperty("tui4j.clipboard.disabled");
            return;
        }
        System.setProperty("tui4j.clipboard.disabled", previous);
    }
}
