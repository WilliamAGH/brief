package com.williamcallahan.chatclient.ui.slash;

/**
 * Opens the full-screen configuration editor.
 * This command doesn't execute directly - it signals the UI to transition to ConfigEditorScreen.
 */
public final class ConfigSlashCommand implements SlashCommand {

    @Override
    public String name() {
        return "/config";
    }

    @Override
    public String description() {
        return "Edit settings";
    }

    @Override
    public boolean matchesInvocation(String input) {
        if (input == null) return false;
        String trimmed = input.trim().toLowerCase();
        return trimmed.equals("/config") || trimmed.startsWith("/config ");
    }

    @Override
    public String run(String input) {
        // Handled by ChatConversationScreen - transitions to ConfigEditorScreen
        return "";
    }
}
