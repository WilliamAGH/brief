# Brief

Brief is a terminal UI app built with [Latte TUI](https://github.com/flatscrew/latte).

Canonical repo: https://github.com/WilliamAGH/brief

Made by [William Callahan](https://williamcallahan.com).

## What it does

Brief is a terminal-first chat client with a slash-command palette, local tool execution (e.g., live weather API), and OpenAI chat completion integration. Persistence and broader provider support are not implemented yet.

## Inspiration

Brief is a showcase of what's possible in modern Java in 2026, and the interface library used (Latte) is a Java port of the popular Go library called [BubbleTea](https://github.com/charmbracelet/bubbletea) from [Charm](https://charm.land/).

## Installation

### Homebrew (macOS)

```bash
brew install williamagh/tap/brief
```

Or install nightly (latest from main):

```bash
brew install --head williamagh/tap/brief
```

Then configure your API key ([setup guide](docs/environment-variables-api-keys.md)) and run:

## Running the Application
```bash
brief
```

## Development

### Requirements

- Java 25
- Gradle 9.x
- API key ([setup guide](docs/environment-variables-api-keys.md))

### Setup

```bash
git clone https://github.com/WilliamAGH/brief.git
cd brief
cp .env-example .env   # then edit with your API key
make run
```

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
│   ├── OpenAiService.java
│   └── tools/
│       ├── Tool.java
│       └── WeatherForecastTool.java
└── lattetui/
    ├── ChatConversationScreen.java
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
