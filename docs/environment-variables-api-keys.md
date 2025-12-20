# Environment Variables & API Keys

Brief requires an OpenAI API key (or compatible provider) for chat completions.

## Getting an OpenAI API Key

1. Go to [platform.openai.com](https://platform.openai.com) and sign up or log in
2. Navigate to **API Keys** in the left sidebar (or go directly to [platform.openai.com/api-keys](https://platform.openai.com/api-keys))
3. Click **Create new secret key**, give it a name, and copy the key
4. Add billing details under **Settings → Billing** (API access requires a paid account)

For the full walkthrough, see the [OpenAI Quickstart Guide](https://platform.openai.com/docs/quickstart).

---

## For Users (Homebrew Install)

If you installed Brief via Homebrew (`brew install williamagh/tap/brief`), configuration is simple:

### Option 1: In-App Prompt (Easiest)

Just run `brief` — the app will prompt you for your API key on first launch and save it to:

```
~/.config/brief/config
```

Your key is stored locally and never transmitted anywhere except to OpenAI (or your configured provider).

### Option 2: Environment Variable

Set `OPENAI_API_KEY` in your shell config (`~/.zshrc` or `~/.bashrc`):

```bash
export OPENAI_API_KEY="sk-..."
```

Reload with `source ~/.zshrc` (or restart your terminal), then run `brief`.

> **Priority:** Environment variables override the config file. This lets you temporarily use a different key without modifying your saved configuration.

---

## For Developers (Local Development)

If you're building Brief from source, additional options are available:

### Option 1: `.env` File (Recommended for Development)

The Makefile automatically loads a `.env` file from the project root:

```bash
cp .env-example .env
```

Edit `.env` with your API key, then:

```bash
make run
```

The `.env` file is in `.gitignore`, so your keys won't be committed.

> **Note:** The `.env` file only works with `make run`. It's not used when running the installed `brief` binary directly.

### Option 2: Inline with Command

Set variables for a single command:

```bash
OPENAI_API_KEY="sk-..." make run
```

### Option 3: Shell Profile

Same as the user option above — add to `~/.zshrc` or `~/.bashrc` for persistent access.

---

## Optional Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | Your API key. Required for chat completions. |
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
