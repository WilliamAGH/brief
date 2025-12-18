package com.williamcallahan.lattetui.slash;

/** Contract for a slash command that can run locally. */
public interface SlashCommand {
    /** Command name including leading slash (e.g. "/weather"). */
    String name();

    /** Short help shown in the palette. */
    String description();

    /** True when this command should handle the given input line. */
    boolean matchesInvocation(String input);

    /** Executes the command and returns text to display. */
    String run(String input) throws Exception;

    /**
     * If true, the command output is added to the LLM conversation context (as a normal assistant message).
     * If false, it is displayed locally only.
     */
    default boolean addsToConversationContext() {
        return false;
    }
    
    default boolean quits() {
        return false;
    }
}
