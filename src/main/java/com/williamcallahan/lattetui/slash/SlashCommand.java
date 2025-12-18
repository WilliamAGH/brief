package com.williamcallahan.lattetui.slash;

import com.williamcallahan.lattetui.PaletteItem;

/** Contract for a slash command that can run locally. */
public interface SlashCommand extends PaletteItem {
    /** Command name including leading slash (e.g. "/weather"). */
    String name();

    /** Short help shown in the palette. */
    String description();

    /** True when this command should handle the given input line. */
    boolean matchesInvocation(String input);

    /** Executes the command and returns text to display. */
    String run(String input) throws Exception;

    /** How the command output is added to the conversation. */
    enum ContextType {
        /** Display only - not added to conversation, LLM won't see it. */
        NONE,
        /** Added as SYSTEM message - LLM sees it as context for follow-up questions. */
        SYSTEM,
        /** Added as ASSISTANT message - LLM sees it as if it said it. */
        ASSISTANT
    }

    /**
     * Determines how the command output is added to the conversation.
     * Default is NONE (display only, not sent to LLM).
     */
    default ContextType contextType() {
        return ContextType.NONE;
    }

    default boolean quits() {
        return false;
    }
}
