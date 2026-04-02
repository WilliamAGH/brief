package com.williamcallahan.chatclient.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.williamcallahan.chatclient.Config;
import com.williamcallahan.chatclient.domain.Conversation;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import com.williamcallahan.tui4j.compat.lipgloss.color.NoColor;
import com.williamcallahan.tui4j.term.TerminalInfo;
import org.junit.jupiter.api.Test;

class ChatConversationScreenWheelTest {

    @Test
    void wheelInputUpdatesVisibleScrollState() {
        TerminalInfo.provide(() -> new TerminalInfo(false, new NoColor()));
        ChatConversationScreen screen = new ChatConversationScreen(
            "You",
            Conversation.builder().id("conversation-1").build(),
            new Config(),
            80,
            24,
            false
        );

        assertFalse(screen.view().contains("↑3L"));

        screen.update(wheel(MouseButton.MouseButtonWheelUp));
        assertTrue(screen.view().contains("↑3L"));

        screen.update(wheel(MouseButton.MouseButtonWheelDown));
        assertFalse(screen.view().contains("↑3L"));
    }

    private static MouseMessage wheel(MouseButton button) {
        return new MouseMessage(
            0,
            0,
            false,
            false,
            false,
            MouseAction.MouseActionPress,
            button
        );
    }
}
