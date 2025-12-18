package com.williamcallahan.lattetui.slash;

/**
 * Opens the model selection palette.
 *
 * This command doesn't execute directly — it signals the UI to open the model picker.
 */
public final class ModelSlashCommand implements SlashCommand {

    @Override
    public String name() {
        return "/model";
    }

    @Override
    public String description() {
        return "Switch AI model";
    }

    @Override
    public boolean matchesInvocation(String input) {
        if (input == null) return false;
        String trimmed = input.trim().toLowerCase();
        return trimmed.equals("/model") || trimmed.startsWith("/model ");
    }

    @Override
    public String run(String input) {
        // Handled by ChatConversationScreen — opens the model palette
        return null;
    }
}
