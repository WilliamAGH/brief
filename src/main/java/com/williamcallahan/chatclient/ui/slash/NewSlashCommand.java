package com.williamcallahan.chatclient.ui.slash;

public final class NewSlashCommand {
    private NewSlashCommand() {}

    public static final class Command implements SlashCommand {
        @Override
        public String name() {
            return "/new";
        }

        @Override
        public String description() {
            return "Start a new chat session (new conversation id)";
        }

        @Override
        public boolean matchesInvocation(String input) {
            return input != null && input.equals(name());
        }

        /** No-op; handled by ChatConversationScreen via model transition. */
        @Override
        public String run(String input) {
            return "";
        }
    }
}

