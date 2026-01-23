package com.williamcallahan.chatclient.ui.slash;

public final class ClearSlashCommand {

    private ClearSlashCommand() {}

    public static final class Command implements SlashCommand {

        @Override
        public String name() {
            return "/clear";
        }

        @Override
        public String description() {
            return "Clear chat and start a new session";
        }

        @Override
        public boolean matchesInvocation(String input) {
            return input != null && input.equalsIgnoreCase(name());
        }

        /** No-op; handled by ChatConversationScreen via model transition. */
        @Override
        public String run(String input) {
            return "";
        }
    }
}
