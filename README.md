# Brief

Brief is a terminal UI app built with [tui4j](https://github.com/williamcallahan/tui4j).

Canonical repo: https://github.com/WilliamAGH/brief

Made by [William Callahan](https://williamcallahan.com).

## What it does

Brief is a terminal-first chat client with a slash-command palette, local tool execution (e.g., live weather API), and OpenAI chat completion integration. Persistence and broader provider support are not implemented yet.

## Inspiration

Brief is a showcase of what's possible in modern Java in 2026, and the interface library used (tui4j) is a Java port of the popular Go library called [BubbleTea](https://github.com/charmbracelet/bubbletea) from [Charm](https://charm.land/).

## Installation

### Homebrew (macOS)

[Homebrew](https://brew.sh/) (package manager)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

```bash
brew install williamagh/tap/brief
```

Or install nightly (latest from main):

```bash
brew install --head williamagh/tap/brief
```

Then run `brief` — the app will prompt you for your API key on first launch and save it to `~/.config/brief/config`.

```bash
brief
```

For alternative providers (OpenRouter, Ollama, LMStudio) or advanced configuration, see the [setup guide](docs/environment-variables-api-keys.md).

## Development

### Requirements

- Java 25
- Gradle 9.x
- OpenAI API key (or compatible provider)

### Setup

```bash
git clone https://github.com/WilliamAGH/brief.git
cd brief
cp .env-example .env   # edit with your API key (development only)
make run
```

> **Note:** The `.env` file is for local development with `make run`, and only for users who cloned this repository from GitHub. End users installing via Homebrew should use the in-app prompt or set `OPENAI_API_KEY` in their shell. See the [setup guide](docs/environment-variables-api-keys.md) for all options.

### Development Commands

```bash
make run     # Build and run
make build   # Build only
make clean   # Clean build artifacts
```

## Package Structure

```
src/main/java/com/williamcallahan/
├── Main.java
├── domain/
│   ├── ChatMessage.java
│   ├── Conversation.java
│   ├── Role.java
│   └── ToolCall.java
├── service/
│   ├── ChatCompletionService.java
│   ├── OpenAiService.java
│   ├── ToolExecutor.java
│   └── tools/
│       ├── Tool.java
│       └── WeatherForecastTool.java
└── ui/
    ├── ApiKeyPromptScreen.java
    ├── ChatConversationScreen.java
    ├── ConfigPromptScreen.java
    ├── HistoryViewport.java
    ├── MouseSelectionController.java
    ├── SlashCommandPalette.java
    ├── TuiTheme.java
    ├── WelcomeScreen.java
    └── slash/
        ├── AboutSlashCommand.java
        ├── ClearSlashCommand.java
        ├── NewSlashCommand.java
        ├── SlashCommand.java
        ├── SlashCommands.java
        └── WeatherSlashCommand.java
```

## Persistence

Persistence is not implemented yet.

## Contributing

Found a bug or have a feature request? Please [open an issue](https://github.com/WilliamAGH/brief/issues/new) on GitHub. Contributions and feedback are welcome, and Pull Requests (PRs) are encouraged!

## Upcoming Plans / Use Cases

Currently building an implementation for [aVenture.vc](https://aventure.vc) — a research tool that makes it possible to research any private company in seconds and get full details, all from the command line.
