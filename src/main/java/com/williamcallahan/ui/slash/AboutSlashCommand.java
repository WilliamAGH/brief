package com.williamcallahan.ui.slash;

/** /about output. */
public final class AboutSlashCommand {
    private AboutSlashCommand() {}

    public static final class Command implements SlashCommand {
        @Override
        public String name() {
            return "/about";
        }

        @Override
        public String description() {
            return "About the author";
        }

        @Override
        public boolean matchesInvocation(String input) {
            return input != null && input.equals(name());
        }

        @Override
        public ContextType contextType() {
            // SYSTEM context so LLM can answer follow-up questions about the app/author
            return ContextType.SYSTEM;
        }

        @Override
        public String run(String input) {
            return """

            *About the App*:
            Message: Made with  ❤️  by William Callahan in San Francisco

            GitHub Repo: https://github.com/WilliamAGH/brief

            *About the Author*:
            Website: williamcallahan.com
            Email: william@williamcallahan.com
            Twitter: https://x.com/WilliamCallahan
            LinkedIn: https://www.linkedin.com/in/williamacallahan/
            """.trim();
        }
    }
}
