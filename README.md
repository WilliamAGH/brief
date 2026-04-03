[![Context7](src/main/resources/static/img/context7-badge.svg)](https://context7.com/williamagh/brief)
[![DeepWiki](src/main/resources/static/img/deepwiki-badge.svg)](https://deepwiki.com/WilliamAGH/brief)
[![Docs](https://img.shields.io/badge/docs-mintlify-18b884)](https://www.mintlify.com/WilliamAGH/brief)

# Brief

A terminal ChatGPT client for fast, keyboard-first chat. Includes slash commands, tool execution, and support for OpenAI-compatible providers.

![Brief screenshot](src/main/resources/static/img/brief-screenshot-1.png)

Built with [TUI4J](https://github.com/WilliamAGH/tui4j) — a Java port of [Bubble Tea](https://github.com/charmbracelet/bubbletea). Location features powered by [Apple Maps Java](https://github.com/WilliamAGH/apple-maps-java).

![Apple Maps in Brief](src/main/resources/static/img/apple-maps-java-screenshot.png)

## Quick Start

### Homebrew (macOS)

```bash
brew install williamagh/tap/brief
brief
```

The app prompts for your API key on first launch and saves it to `~/.config/brief/config`.

For alternative providers (OpenRouter, Ollama, LMStudio), see the [configuration guide](docs/environment-variables-api-keys.md).

### GitHub Releases

Download from [releases](https://github.com/WilliamAGH/brief/releases/latest). Requires Java 25.

Each release includes a distribution ZIP (with shell wrapper scripts) and a standalone fat JAR.
To run the JAR directly:

```bash
java -jar brief.jar
```

Dev builds: [main](https://github.com/WilliamAGH/brief/releases/tag/snapshot-main) · [dev](https://github.com/WilliamAGH/brief/releases/tag/snapshot-dev) (updated on every push)

## Development

### Prerequisites

This project uses **Gradle Toolchains** with **Temurin JDK 25** and **mise** (or **asdf**) for reproducible builds.

**Option 1: Using mise (recommended)**

```bash
# Install mise if you don't have it: https://mise.jdnow.dev/
mise install
```

**Option 2: Using asdf**

```bash
# Install asdf if you don't have it: https://asdf-vm.com/
asdf plugin add java https://github.com/halcyon/asdf-java.git
asdf install
```

**What happens**: Gradle Toolchains will auto-download Temurin JDK 25 on first build if not present locally. The `mise`/`asdf` setup ensures your shell and IDE use the correct Java version.

### Running

```bash
git clone https://github.com/WilliamAGH/brief.git
cd brief
cp .env-example .env   # add your API key
make run
```

Commands: `make run` | `make build` | `make clean` | `make dist`

See [docs/development.md](docs/development.md) for more details.

## Contributing

[Open an issue](https://github.com/WilliamAGH/brief/issues/new) for bugs or feature requests. PRs welcome.

---

Made by [William Callahan](https://williamcallahan.com) · [Repo](https://github.com/WilliamAGH/brief)

[Other projects I've made](https://williamcallahan.com/projects)
