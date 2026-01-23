- SRC1 Never make assumptions; if unsure, stop and verify.
- SRC2 For dependency code questions, inspect ~/.m2 JARs first; fallback to upstream GitHub; never answer without referencing code.
- UPS1 Upstream dependencies (e.g., tui4j) are stable public libraries; do not modify without explicit request.
- UPS2 tui4j is a 1:1 port of Charmbracelet Go libraries; edits MUST match upstream file structure and logic exactly—no exceptions.
- NME1 Use clear, specific names; avoid abbreviations unless standard.
- FUN1 Keep functions small and focused; one responsibility per function.
- DRY1 Remove duplication; reuse existing utilities instead of rewriting logic.
- ERR1 Use exceptions for exceptional cases; avoid defensive checks on trusted inputs.
- CMT1 Comments and Javadocs only when they add clarity; avoid academic tags like @author/@since/@version.
- FMT1 Keep formatting and style consistent with the surrounding file.
- TST1 Update or add tests when behavior changes; do not change behavior without coverage.
- DEP1 Avoid unnecessary dependencies and unused code.

## Details

- SRC1 Verify with primary sources before answering; do not infer behavior without evidence.
- SRC2 Use dependency source JARs or decompiled classes from ~/.m2 to confirm behavior; if not available, consult the dependency's GitHub repo; always cite file paths or class names used.
- UPS1 Do not add, remove, or refactor code in upstream dependencies unless the user explicitly requests it.
- UPS2 When editing tui4j: each Java file in `compat/` must map 1:1 to an upstream Go file; do not combine logic from multiple upstream files into one Java file; verify against `~/Developer/git/cursor/bubbletea`, `~/Developer/git/cursor/bubbles`, `~/Developer/git/cursor/lipgloss`, `~/Developer/git/cursor/x`.
- NME1 Prefer domain terms and intent-revealing names; rename unclear identifiers.
- FUN1 Split large methods; reduce branching and nested blocks when it improves readability.
- DRY1 Replace repeated logic with a shared function, utility, or existing helper.
- ERR1 Do not add guard clauses or try/catch in trusted codepaths unless required by the surrounding code or error model.
- CMT1 Keep documentation short and direct; explain why, not what; keep Javadocs concise and human.
- FMT1 Follow existing spacing, imports, and ordering; avoid style changes unrelated to the task.
- TST1 Prefer fast, focused tests; keep tests aligned with the public contract.
- DEP1 Remove unused imports, dependencies, and dead code.

## Project-Specific

### Architecture
- Brief is a terminal chat client built on **tui4j** (https://github.com/WilliamAGH/tui4j).
- Uses The Elm Architecture: `Model` with `init()`, `update(Message)`, `view()`.

### Upstream References
When debugging TUI behavior or adding UI features:
- **tui4j**: https://github.com/WilliamAGH/tui4j — Java port of Bubble Tea
- **Bubble Tea** (Go): https://github.com/charmbracelet/bubbletea — original TUI framework
- **Bubbles** (Go): https://github.com/charmbracelet/bubbles — component patterns
- **Lip Gloss** (Go): https://github.com/charmbracelet/lipgloss — styling reference

### Package Mapping (tui4j)
| tui4j Package                                  | Purpose                        |
|-----------------------------------------------|--------------------------------|
| com.williamcallahan.tui4j                      | Program, Model, Message, Cmd   |
| com.williamcallahan.tui4j.compat.bubbletea.bubbles.* | Components (list, textinput)   |
| com.williamcallahan.tui4j.compat.bubbletea.lipgloss   | Styling, colors, borders       |

### Dependencies
- **tui4j** (com.williamcallahan:tui4j): TUI framework
- Check tui4j STATUS.md for available bubbles before building custom components.
