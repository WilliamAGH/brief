# Environment Variables & API Keys

Brief requires an OpenAI API key and supports optional configuration through environment variables.

## Required

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | Your OpenAI API key. Required for chat completions. |

## Optional

| Variable | Description |
|----------|-------------|
| `OPENAI_BASE_URL` | Custom API endpoint for OpenAI-compatible services (OpenRouter, LMStudio, etc.) |
| `LLM_MODEL` | Default model ID (e.g., `gpt-4o`, `gpt-4o-mini`). Falls back to SDK default if unset. |

### Debug & Display Flags

| Variable | Values | Description |
|----------|--------|-------------|
| `BRIEF_ALT_SCREEN` | `1` | Use alternate screen buffer (clears on exit) |
| `BRIEF_MOUSE` | `all`, `btn`, `off` | Mouse tracking mode |
| `BRIEF_SCROLLBACK` | `1` | Print debug output to scrollback |
| `BRIEF_SHOW_TOOLS` | `1` | Display tool call messages in the UI |
| `BRIEF_AUTOWRAP` | `1` | Enable terminal autowrap |

## Setting Environment Variables

### Method 1: `.env` File (Recommended)

The Makefile automatically loads a `.env` file from the project root. Copy the example and fill in your key:

```bash
cp .env-example .env
```

Then edit `.env` with your API key and run:

```bash
make run
```

The `.env` file is already in `.gitignore`, so your keys won't be committed.

### Method 2: Inline with Command

Set variables for a single command:

```bash
OPENAI_API_KEY="sk-..." make run
```

### Method 3: Shell Profile (Persistent, Global)

Add to your shell config (`~/.zshrc` or `~/.bashrc`) if you want the same keys available across all projects:

```bash
export OPENAI_API_KEY="sk-..."
```

Reload with `source ~/.zshrc` (or restart your terminal).

## Using Alternative Providers

### OpenRouter

```bash
export OPENAI_API_KEY="sk-or-..."
export OPENAI_BASE_URL="https://openrouter.ai/api/v1"
export LLM_MODEL="anthropic/claude-3.5-sonnet"
```

### LMStudio (Local)

```bash
OPENAI_API_KEY=lm-studio
OPENAI_BASE_URL=http://localhost:1234/v1
```

### Ollama (Local)

```bash
OPENAI_API_KEY=ollama
OPENAI_BASE_URL=http://localhost:11434/v1
LLM_MODEL=llama3.2
```

Ollama must be running (`ollama serve`) with your model pulled (`ollama pull llama3.2`).

## Troubleshooting

**"API key not found" error:**
Ensure `OPENAI_API_KEY` is exported in the same shell session where you run Brief.

**Model not available:**
Check that your API key has access to the model specified in `LLM_MODEL`. Some models require specific API tiers.

**Using a proxy or corporate network:**
You may need to configure `HTTP_PROXY`/`HTTPS_PROXY` environment variables for network access.
