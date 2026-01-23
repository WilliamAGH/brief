package com.williamcallahan.chatclient.ui.slash;

/**
 * Slash command for searching places using Apple Maps.
 * Opens an interactive overlay for browsing and selecting places.
 *
 * Usage: /locate <query>
 * Examples:
 *   /locate coffee shops in San Francisco
 *   /locate "Blue Bottle Coffee"
 *   /locate restaurants near me
 *
 * This command doesn't execute directly — it signals the UI to open the places overlay.
 */
public final class LocateSlashCommand {

    private LocateSlashCommand() {}

    public static final class Command implements SlashCommand {

        @Override
        public String name() {
            return "/locate";
        }

        @Override
        public String description() {
            return "Search places with Apple Maps";
        }

        @Override
        public boolean matchesInvocation(String input) {
            if (input == null) return false;
            String lower = input.toLowerCase();
            return lower.equals(name()) || lower.startsWith(name() + " ");
        }

        @Override
        public String run(String input) {
            // Handled by ChatConversationScreen — opens the places overlay
            return null;
        }

        /**
         * Does not require arguments - selecting /locate opens the interactive overlay.
         */
        @Override
        public boolean requiresArguments() {
            return false;
        }
    }

    /**
     * Creates a user-facing request from the /locate input.
     * Used when routing to the LLM for conversational responses.
     */
    public static String toUserRequest(String inputLine) {
        String query = parseQuery(inputLine);
        if (query.isBlank()) {
            return "Find a place or location for me.";
        }
        return "Find " + query + " for me.";
    }

    /**
     * Creates a system prompt instructing the LLM how to use Apple Maps tools.
     */
    public static String toLlmPrompt(String inputLine) {
        String query = parseQuery(inputLine);

        StringBuilder sb = new StringBuilder();
        sb.append("User requested location/place search via /locate.\n");
        if (!query.isBlank()) {
            sb.append("Query: \"").append(query).append("\".\n");
        }
        sb.append("\n");
        sb.append("You have two Apple Maps tools available:\n");
        sb.append("\n");
        sb.append("1. search_places - Search for businesses, POIs, or places by name/type.\n");
        sb.append("   Use for: \"coffee shops\", \"Stripe\", \"restaurants near downtown\"\n");
        sb.append("   Parameters:\n");
        sb.append("   - query (required): Natural language search query\n");
        sb.append("   - country_code (optional): ISO 3166-1 alpha-2 code like \"US\"\n");
        sb.append("\n");
        sb.append("2. geocode_address - Convert a street address to coordinates.\n");
        sb.append("   Use for: \"880 Harrison St, San Francisco, CA 94107\"\n");
        sb.append("   Parameters:\n");
        sb.append("   - address (required): Street address with city/state/country as known\n");
        sb.append("   - country_code (optional): ISO 3166-1 alpha-2 code like \"US\"\n");
        sb.append("\n");
        sb.append("Guidelines:\n");
        sb.append("- If the query looks like a street address, use geocode_address.\n");
        sb.append("- If the query is a place name, business, or category, use search_places.\n");
        sb.append("- If ambiguous (e.g. just \"restaurants\"), ask for location context.\n");
        sb.append("- Summarize results conversationally with name, address, and category.\n");
        sb.append("- Include coordinates only if the user explicitly asks for them.\n");

        return sb.toString().trim();
    }

    private static String parseQuery(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (!trimmed.toLowerCase().startsWith("/locate")) return "";
        String rest = trimmed.substring("/locate".length()).trim();
        // Remove quotes if present
        if (rest.length() >= 2 && rest.startsWith("\"") && rest.endsWith("\"")) {
            rest = rest.substring(1, rest.length() - 1);
        }
        return rest;
    }
}
