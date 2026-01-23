package com.williamcallahan.chatclient.ui;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.message.EnterKeyModifierMessage;

/**
 * Determines whether a message should insert a newline in the composer.
 */
final class ComposerNewlineDecider {

    /**
     * Creates a newline decision helper.
     */
    private ComposerNewlineDecider() {
    }

    /**
     * Returns whether the message should insert a newline in the composer.
     *
     * @param msg input message
     * @return true when the message represents a newline trigger
     */
    static boolean shouldInsertNewline(Message msg) {
        if (msg instanceof EnterKeyModifierMessage) {
            return true;
        }
        if (msg instanceof KeyPressMessage key) {
            return KeyAliases.getKeyType(KeyAlias.KeyCtrlJ) == key.type();
        }
        return false;
    }
}
