package com.williamcallahan.chatclient.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.williamcallahan.chatclient.Config;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import org.junit.jupiter.api.Test;

class ConfigPromptScreenPasteTest {

    @Test
    void pasteInsertsSanitizedSingleLineText() {
        PromptScreenUnderTest prompt = new PromptScreenUnderTest();

        prompt.update(new PasteMessage("sk-test\r\nline-two\tline-three"));

        assertEquals("sk-test line-two line-three", prompt.currentValue());
    }

    @Test
    void pasteTrimsBoundaryWhitespaceFromControlCharacters() {
        PromptScreenUnderTest prompt = new PromptScreenUnderTest();

        prompt.update(new PasteMessage("\n\t sk-test \r\n"));

        assertEquals("sk-test", prompt.currentValue());
    }

    private static final class PromptScreenUnderTest extends ConfigPromptScreen {

        private PromptScreenUnderTest() {
            super(new Config(), "placeholder", 256);
        }

        @Override
        protected String promptTitle() {
            return "title";
        }

        @Override
        protected String promptBody() {
            return "body";
        }

        @Override
        protected UpdateResult<? extends Model> onSubmit(String value) {
            return UpdateResult.from(this);
        }

        private String currentValue() {
            return textInput.value();
        }
    }
}
