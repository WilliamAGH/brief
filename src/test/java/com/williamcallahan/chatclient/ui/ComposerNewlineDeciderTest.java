package com.williamcallahan.chatclient.ui;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.message.EnterKeyModifier;
import com.williamcallahan.tui4j.message.EnterKeyModifierMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests newline detection for composer input.
 */
class ComposerNewlineDeciderTest {

    /**
     * Ensures Enter modifier messages map to newline insertion.
     */
    @Test
    void testEnterModifierNewline() {
        Message message = new EnterKeyModifierMessage(EnterKeyModifier.Shift);
        assertTrue(ComposerNewlineDecider.shouldInsertNewline(message));
    }

    /**
     * Ensures Ctrl+J maps to newline insertion and Enter does not.
     */
    @Test
    void testCtrlJNewline() {
        Message ctrlJ = new KeyPressMessage(
            new Key(KeyAliases.getKeyType(KeyAlias.KeyCtrlJ))
        );
        assertTrue(ComposerNewlineDecider.shouldInsertNewline(ctrlJ));

        Message enter = new KeyPressMessage(
            new Key(KeyAliases.getKeyType(KeyAlias.KeyEnter))
        );
        assertFalse(ComposerNewlineDecider.shouldInsertNewline(enter));
    }
}
