package com.williamcallahan.chatclient.ui.slash;

import java.util.List;

/** Slash command for weather requests. */
public final class WeatherSlashCommand {
    private static final String DEFAULT_CITY = "San Francisco";

    private WeatherSlashCommand() {}

    public static final class Command implements SlashCommand {
        @Override
        public String name() {
            return "/weather";
        }

        @Override
        public String description() {
            return "Ask the assistant for weather";
        }

        @Override
        public boolean matchesInvocation(String input) {
            if (input == null) return false;
            return input.equals(name()) || input.startsWith(name() + " ");
        }

        /** No-op; ChatConversationScreen routes this to LLM for tool invocation. */
        @Override
        public String run(String input) {
            return "";
        }
    }

    public static String toLlmPrompt(String inputLine) {
        ParsedArgs parsed = parseArgs(inputLine);
        String location = parsed.location.isBlank() ? DEFAULT_CITY : parsed.location;

        StringBuilder sb = new StringBuilder();
        sb.append("User requested weather via /weather.\n");
        sb.append("Location input: \"").append(location).append("\".\n");
        sb.append("Get current weather + a 5-day forecast.\n");
        sb.append("Use the get_weather_forecast tool.\n");
        sb.append("When summarizing results, use the plain-language condition text.\n");
        sb.append("Do not mention numeric condition codes.\n");
        sb.append("When calling the tool:\n");
        sb.append("- Put only the place name in city (no comma qualifiers).\n");
        sb.append("- Put disambiguation into country_code/admin1/admin2 when available (e.g. US + California).\n");
        sb.append("- If ambiguous, ask a single clarification question instead of guessing.\n");
        if (parsed.json) {
            sb.append("Return only the JSON tool output.\n");
        }
        return sb.toString().trim();
    }

    public static String toUserRequest(String inputLine) {
        ParsedArgs parsed = parseArgs(inputLine);
        String location = parsed.location.isBlank() ? DEFAULT_CITY : parsed.location;
        return "What is the current weather and 5-day forecast for " + location + "?";
    }

    private static final class ParsedArgs {
        final String location;
        final boolean json;

        ParsedArgs(String location, boolean json) {
            this.location = location;
            this.json = json;
        }
    }

    private static ParsedArgs parseArgs(String input) {
        if (input == null) return new ParsedArgs("", false);
        String t = input.trim();
        if (!t.startsWith("/weather")) return new ParsedArgs("", false);

        String rest = t.substring("/weather".length()).trim();
        if (rest.isEmpty()) return new ParsedArgs("", false);

        List<String> tokens = List.of(rest.split("\\s+"));
        boolean json = tokens.stream().anyMatch(s -> s.equalsIgnoreCase("--json") || s.equalsIgnoreCase("-j"));

        StringBuilder location = new StringBuilder();
        for (String tok : tokens) {
            if (tok.equalsIgnoreCase("--json") || tok.equalsIgnoreCase("-j")) continue;
            if (tok.startsWith("--")) continue;
            if (!location.isEmpty()) location.append(' ');
            location.append(tok);
        }
        return new ParsedArgs(location.toString().trim(), json);
    }
}
