# Configuration

Brief uses OpenAI-compatible APIs. Configuration can come from environment variables or `~/.config/brief/config`.

## Quick Start

Run `brief` — the app prompts for your API key and saves it. Done.

Or set it in your shell:

```bash
export OPENAI_API_KEY="sk-..."
```

## Config File

Settings are stored in `~/.config/brief/config`:

```properties
openai.api_key=sk-...
openai.base_url=https://api.openai.com/v1
model=gpt-4o
user.name=Your Name
config.priority=env
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | API key (required) |
| `OPENAI_BASE_URL` | Custom endpoint for alternative providers |
| `LLM_MODEL` | Default model ID |
| `BRIEF_CONFIG_PRIORITY` | `env` (default) or `config` — which source wins when both set |

### Display Flags

| Variable | Values | Description |
|----------|--------|-------------|
| `BRIEF_ALT_SCREEN` | `1` | Alternate screen buffer (clears on exit) |
| `BRIEF_MOUSE` | `all`, `btn`, `off` | Mouse tracking mode |
| `BRIEF_SHOW_TOOLS` | `1` | Show tool call messages |

## Alternative Providers

### OpenRouter

```bash
export OPENAI_API_KEY="sk-or-..."
export OPENAI_BASE_URL="https://openrouter.ai/api/v1"
export LLM_MODEL="anthropic/claude-sonnet-4-20250514"
```

### Ollama (Local)

```bash
export OPENAI_API_KEY="ollama"
export OPENAI_BASE_URL="http://localhost:11434/v1"
export LLM_MODEL="llama3.2"
```

Requires: `ollama serve` and `ollama pull llama3.2`

### LMStudio (Local)

```bash
export OPENAI_API_KEY="lm-studio"
export OPENAI_BASE_URL="http://localhost:1234/v1"
```

## Development

For local development, copy `.env-example` to `.env`:

```bash
cp .env-example .env
make run
```

The `.env` file is gitignored and only used with `make run`.
