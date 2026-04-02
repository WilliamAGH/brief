package com.williamcallahan.chatclient.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatConversationScreenInputHistoryPolicyTest {

    @Test
    void storesHistoryForPlainMessagesAndLlmSlashCommands() {
        assertTrue(ChatConversationScreen.storesInputHistory(" hello world "));
        assertTrue(ChatConversationScreen.storesInputHistory("/weather San Francisco"));
        assertTrue(ChatConversationScreen.storesInputHistory("/locate coffee"));
    }

    @Test
    void skipsHistoryForUiOnlyAndLocalSlashCommands() {
        assertFalse(ChatConversationScreen.storesInputHistory(""));
        assertFalse(ChatConversationScreen.storesInputHistory("   "));
        assertFalse(ChatConversationScreen.storesInputHistory("/model"));
        assertFalse(ChatConversationScreen.storesInputHistory("/config"));
        assertFalse(ChatConversationScreen.storesInputHistory("/locate"));
        assertFalse(ChatConversationScreen.storesInputHistory("/clear"));
        assertFalse(ChatConversationScreen.storesInputHistory("/about"));
        assertFalse(ChatConversationScreen.storesInputHistory("/does-not-exist"));
    }
}
