package com.williamcallahan.chatclient.ui;

import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests view line manipulation from Textarea component.
 */
class ComposerViewLinesTest {

    /**
     * Ensures single line input with prompt is handled correctly.
     */
    @Test
    void testSingleLine() {
        Textarea composer = mock(Textarea.class);
        
        when(composer.view()).thenReturn("› Hello");
        when(composer.lineCount()).thenReturn(1);

        List<String> lines = ComposerViewLines.from(composer);
        
        assertEquals(1, lines.size());
        assertEquals("› Hello", lines.get(0));
    }

    /**
     * Ensures trailing empty lines are trimmed but at least one line is kept.
     */
    @Test
    void testTrailingEmptyLinesTrimmed() {
        Textarea composer = mock(Textarea.class);
        
        when(composer.view()).thenReturn("› Content\n\n\n");
        when(composer.lineCount()).thenReturn(1);

        List<String> lines = ComposerViewLines.from(composer);
        
        // Should trim trailing empty lines
        assertTrue(lines.size() >= 1);
        assertEquals("› Content", lines.get(0));
    }

    /**
     * Ensures empty input results in at least one line.
     */
    @Test
    void testEmptyInput() {
        Textarea composer = mock(Textarea.class);
        
        when(composer.view()).thenReturn("");
        when(composer.lineCount()).thenReturn(0);

        List<String> lines = ComposerViewLines.from(composer);
        
        // Should return empty list or single empty line
        assertNotNull(lines);
        assertTrue(lines.isEmpty() || lines.size() == 1);
    }

    /**
     * Ensures multi-line input is processed correctly.
     */
    @Test
    void testMultiLineInput() {
        Textarea composer = mock(Textarea.class);
        Textarea.Style mockTextAreaStyle = mock(Textarea.Style.class);
        Style mockPromptStyle = mock(Style.class);
        
        when(composer.view()).thenReturn("› Line 1\n› Line 2\n› Line 3");
        when(composer.lineCount()).thenReturn(3);
        when(composer.style()).thenReturn(mockTextAreaStyle);
        when(mockTextAreaStyle.computedPrompt()).thenReturn(mockPromptStyle);
        when(mockPromptStyle.render(anyString())).thenReturn("› ");

        List<String> lines = ComposerViewLines.from(composer);
        
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("Line 1"));
        assertTrue(lines.get(1).contains("Line 2"));
        assertTrue(lines.get(2).contains("Line 3"));
    }

    /**
     * Ensures lines with just whitespace are handled properly.
     */
    @Test
    void testWhitespaceOnlyLines() {
        Textarea composer = mock(Textarea.class);
        
        when(composer.view()).thenReturn("› Content\n   \n   ");
        when(composer.lineCount()).thenReturn(1);

        List<String> lines = ComposerViewLines.from(composer);
        
        // Should handle whitespace-only trailing lines
        assertTrue(lines.size() >= 1);
        assertEquals("› Content", lines.get(0));
    }
}
