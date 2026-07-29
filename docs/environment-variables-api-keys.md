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
reasoning.effort=high
user.name=Your Name
config.priority=env
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | API key (required) |
| `OPENAI_BASE_URL` | Custom endpoint for alternative providers |
| `LLM_MODEL` | Default model ID |
| `BRIEF_REASONING_EFFORT` | Optional canonical Chat Completions `reasoning_effort`; accepted values are defined by [`ReasoningEffort`](../src/main/java/com/williamcallahan/chatclient/ReasoningEffort.java) |
| `BRIEF_CONFIG_PRIORITY` | `env` (default) or `config` — which source wins when both set |

## Reasoning Effort

Leave reasoning effort unset to preserve the selected model or provider default. Set it explicitly with `BRIEF_REASONING_EFFORT`, `reasoning.effort` in the config file, or `brief --reasoning-effort <value>`; the command-line choice takes precedence over the configured source.

Brief forwards every explicit value as the standard Chat Completions `reasoning_effort` field. It does not infer capabilities from the model name or base URL, and it does not silently change an unsupported value. An OpenAI-compatible provider can therefore accept or reject the requested field according to its own API support.

[`ReasoningEffort`](../src/main/java/com/williamcallahan/chatclient/ReasoningEffort.java) is the sole accepted-value owner.

### Display Flags

| Variable | Values | Description |
|----------|--------|-------------|
| `BRIEF_ALT_SCREEN` | `1` | Alternate screen buffer (clears on exit) |
| `BRIEF_MOUSE` | `0`/`off`/`native`/`false`, `1`/`all`/`true`, `wheel`/`btn`/`buttons`, `select` | Mouse tracking mode (default: `select`) |
| `BRIEF_SHOW_TOOLS` | `1` | Show tool call messages |

Default `select` captures the mouse wheel so chat history scrolls inside Brief and drag selection copies chat text directly. Set `BRIEF_MOUSE=0` to keep all mouse behavior native to the terminal.

Ghostty on macOS and Linux supports using `Shift` to bypass application mouse reporting for terminal selection by default. If you prefer that workflow full-time, keep `BRIEF_MOUSE=0`.

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
