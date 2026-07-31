# Task: Audit and Decompose ChatConversationScreen.java

## Context

`src/main/java/com/williamcallahan/chatclient/ui/ChatConversationScreen.java` is **1631 lines** — 3.3x over the project's 500 LOC hard cap (LOC1). It is the main Elm Architecture `Model` for the chat view, but it has accumulated rendering, input handling, overlay management, mouse selection, LLM transport, message history rendering, slash command routing, and paste handling into a single file.

This is a **Java TUI chat client** built on the **tui4j** framework (Elm Architecture: `init()`, `update(Message)`, `view()`).

## Objective

Audit the file for **duplicated logic** — both within the repo and against tui4j dependency — then decompose what remains into SRP-compliant units (≤500 LOC each).

**Order matters: deduplicate FIRST, decompose SECOND.** Splitting before deduplicating just spreads the smell across more files.

## Skills to Invoke

1. **`cleanup`** — canonical ownership audit. Check for mirror owners, adapter layers, duplicate logic across the `ui/` package.
2. **`dependencies`** — verify every custom utility against tui4j's actual API (decompile from `~/.gradle/caches/`). If tui4j provides it, delete the local version.

## Mandatory Rules (from AGENTS.md)

- **LOC1**: All written, non-generated source files ≤ 500 lines. Zero tolerance.
- **MO1**: No monoliths. New logic → new file. Duplicate logic → extract.
- **DRY1**: Remove duplication; reuse existing utilities.
- **SS1-SS5**: Single semantic owner. No mirror owners.
- **SLOP1**: No adapter/bridge/wrapper modules that reshape between equivalent types.
- **KISS1**: Simplest solution. Achieve by removing, not adding.
- **SRC2**: For dependency questions, inspect `~/.m2` or `~/.gradle/caches` JARs first. Never answer without referencing actual code.
- **UPS1**: Do not modify tui4j. Use what it provides.
- **DEP1**: Remove unused imports, dependencies, and dead code.

## Phase 1: Dependency Duplication Audit

tui4j (`com.williamcallahan:tui4j:0.3.2-preview`) already ships these components. For each, decompile from the JAR at `~/.gradle/caches/modules-2/files-2.1/com.williamcallahan/tui4j/0.3.2-preview/` and compare against the local implementation:

### 1a. Viewport vs HistoryViewport

**tui4j provides**: `com.williamcallahan.tui4j.compat.bubbles.viewport.Viewport`
- Full Elm Architecture model (`init()`, `update()`, `view()`)
- `scrollUp(n)`, `scrollDown(n)`, `pageUp()`, `pageDown()`, `gotoTop()`, `gotoBottom()`
- `setContent(String)`, `atTop()`, `atBottom()`, `scrollPercent()`
- Mouse wheel handling built in

**Local reimplementation**: `src/main/java/com/williamcallahan/chatclient/ui/HistoryViewport.java`

**Audit question**: Can `HistoryViewport` be replaced by `Viewport`, or does it provide behavior Viewport doesn't? Decompile both and compare method-by-method.

### 1b. MouseSelectionTracker vs MouseSelectionController

**tui4j provides**: `com.williamcallahan.tui4j.input.MouseSelectionTracker`
- `update(MouseMessage)` → `MouseSelectionUpdate`
- `isSelecting()`, `lastColumn()`, `lastRow()`

**tui4j also provides**: `com.williamcallahan.tui4j.input.MouseSelectionAutoScroller`
- Edge-drag auto-scrolling with configurable edge rows and interval
- Runs on a `ScheduledExecutorService`, fires scroll messages automatically

**Local reimplementation**: `src/main/java/com/williamcallahan/chatclient/ui/MouseSelectionController.java` (349 lines)
- Custom selection tracking, edge-drag hints (manual, not timer-based), clipboard copy, link opening

**Audit question**: How much of MouseSelectionController is already provided by MouseSelectionTracker + MouseSelectionAutoScroller? The link-opening and clipboard-copy are app-specific, but the selection state machine and edge-drag may be pure duplication.

### 1c. Text utilities

**tui4j provides**: `TextWrapper`, `WordWrap`, `HardWrap`, `Strip`, `Truncate`, `StringWidth`, `TextWidth`

**Local code to check**: `TuiTheme.java` (`truncatePreservingAnsi`, `visualWidth`), inline ANSI strip/wrap logic in `ChatConversationScreen`, `renderHistory`, `renderMessageBlock`, `sanitizeForDisplay`, `highlightSelection`, `rstrip`.

**Audit question**: Which local text manipulation reimplements tui4j utilities?

## Phase 2: Intra-Repo Duplication Audit

Scan all files in `src/main/java/com/williamcallahan/chatclient/ui/` for:

1. **Duplicated key handling patterns** — `ModelPalette`, `ConfigPalette`, `PlacesOverlay`, `SlashCommandPalette` all handle keyboard input. Is there a shared pattern that should be extracted?
2. **Duplicated overlay management** — `handleModelPaletteKey()`, `handleConfigPaletteKey()`, `handleSlashPaletteKey()`, `handlePlacesOverlayKey()` in ChatConversationScreen are structurally identical. Can these be unified?
3. **Duplicated rendering patterns** — palette border/chrome rendering across overlay classes.

Note: `PlacesOverlay.java` is also over LOC1 at 583 lines and `ConfigPalette.java` at 447 lines. Flag these but focus on ChatConversationScreen first.

## Phase 3: Decomposition

After deduplication, the remaining ChatConversationScreen logic should decompose along these concern boundaries (suggested, not prescriptive — let the audit inform the actual splits):

| Concern | Current methods | Potential extraction |
|---------|----------------|---------------------|
| LLM transport | `submitToLlm`, `submitToLlmDetached`, `llmCall`, `formatLlmError`, `appendInternalSystemPrompt` | `LlmTransport` or similar |
| Message rendering | `renderHistory`, `renderAllHistory`, `renderMessageBlock`, `highlightSelection`, `sanitizeForDisplay` | `HistoryRenderer` |
| Title/chrome | `renderTitleBorder`, `joinLeftRight` | `TitleBar` |
| Slash routing | `slashOrChatCall`, `parseLocateQuery`, slash override map | `SlashRouter` |
| Mouse targets | `buildMouseTargets`, `addToolbarTargets`, `addOverlayTargets` | Part of view concerns |

**Do NOT create adapter/bridge classes.** Each extracted unit should own its concern and be called directly.

## Deliverables

1. List of local code confirmed as dependency duplication (with decompiled evidence)
2. List of intra-repo duplication (with file:line references)
3. Implementation: deletions first, then extractions, then wiring
4. All files ≤ 500 LOC after changes
5. All existing tests pass (`make test`)
6. Lint passes (`make lint`) with no new warnings

## Verification

After each extraction:
```bash
# Tests
make test

# Lint
make lint

# Line counts — every written file must be ≤ 500
find src/main/java -name '*.java' -exec wc -l {} + | sort -rn | head -20
```
