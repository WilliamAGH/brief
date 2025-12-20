package com.williamcallahan.service;

import com.williamcallahan.domain.ChatMessage;
import com.williamcallahan.domain.Role;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutorTest {

    @Test
    void shouldIncludeUserMessage_AllowsInternalUserMessages() {
        ChatMessage message = new ChatMessage(
            "m_1",
            "c_1",
            0,
            Role.USER,
            ChatMessage.Source.INTERNAL,
            "weather for san francisco",
            OffsetDateTime.now(ZoneOffset.UTC),
            "model",
            "provider",
            null,
            null,
            null,
            null,
            null
        );

        assertTrue(ToolExecutor.shouldIncludeUserMessage(message, message.content()));
    }

    @Test
    void shouldIncludeUserMessage_RejectsLocalUserMessages() {
        ChatMessage message = new ChatMessage(
            "m_2",
            "c_1",
            1,
            Role.USER,
            ChatMessage.Source.LOCAL,
            "/weather",
            OffsetDateTime.now(ZoneOffset.UTC),
            "model",
            "provider",
            null,
            null,
            null,
            null,
            null
        );

        assertFalse(ToolExecutor.shouldIncludeUserMessage(message, message.content()));
    }
}
