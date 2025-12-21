package com.williamcallahan.chatclient.ui.slash;

import java.util.ArrayList;
import java.util.List;

/** Slash command registry and helpers. */
public final class SlashCommands {
    private SlashCommands() {}

    public static List<SlashCommand> defaults() {
        return List.of(
            new WeatherSlashCommand.Command(),
            new ModelSlashCommand(),
            new NewSlashCommand.Command(),
            new ClearSlashCommand.Command(),
            new AboutSlashCommand.Command(),
            new Quit()
        );
    }

    public static List<SlashCommand> filterForComposer(List<SlashCommand> commands, String composerValue) {
        String prefix = slashTokenPrefix(composerValue);
        if (prefix == null) return List.of();
        if (prefix.isEmpty()) return commands;

        String p = prefix.toLowerCase();
        List<SlashCommand> out = new ArrayList<>();
        for (SlashCommand c : commands) {
            String name = c.name();
            if (name != null && name.length() > 1 && name.substring(1).toLowerCase().startsWith(p)) {
                out.add(c);
            }
        }
        return out;
    }

    public static SlashCommand matchInvocation(List<SlashCommand> commands, String input) {
        if (input == null) return null;
        String t = input.trim();
        for (SlashCommand c : commands) {
            if (c.matchesInvocation(t)) return c;
        }
        return null;
    }

    /**
     * Returns the token prefix without leading slash ("/wea" -> "wea").
     * Returns null when the composer doesn't start with a slash command token.
     */
    public static String slashTokenPrefix(String composerValue) {
        if (composerValue == null) return null;
        String trimmed = composerValue.stripLeading();
        if (!trimmed.startsWith("/")) return null;
        String rest = trimmed.substring(1);
        int space = rest.indexOf(' ');
        if (space >= 0) rest = rest.substring(0, space);
        return rest.trim();
    }

    private static final class Quit implements SlashCommand {
        @Override
        public String name() {
            return "/quit";
        }

        @Override
        public String description() {
            return "Quit";
        }

        @Override
        public boolean matchesInvocation(String input) {
            return input != null && input.equals(name());
        }

        @Override
        public boolean quits() {
            return true;
        }

        /** No-op; quit is handled via {@link #quits()} returning true. */
        @Override
        public String run(String input) {
            return "";
        }
    }
}
