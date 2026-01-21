package com.williamcallahan.chatclient.ui;

import com.williamcallahan.chatclient.ui.slash.SlashCommand;
import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SlashCommandPaletteTest {

    private static final class TestCommand implements SlashCommand {
        private final String name;

        private TestCommand(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "";
        }

        @Override
        public boolean matchesInvocation(String input) {
            return input != null && input.startsWith(name);
        }

        @Override
        public String run(String input) {
            return "";
        }
    }

    @Test
    void clickSubmitsCommandNameOnly() {
        SlashCommandPalette palette = new SlashCommandPalette();
        Textarea composer = new Textarea();
        List<SlashCommand> commands = List.of(new TestCommand("/weather"), new TestCommand("/clear"));

        palette.openFromMouse(composer, commands);
        composer.setValue("/weather San Francisco");

        SlashCommandPalette.PaletteUpdate update = palette.click(0, composer, commands);

        assertNotNull(update);
        assertEquals("/weather", update.submitText());
    }
}
