- SRC1 Never make assumptions; if unsure, stop and verify. Read existing codebase first before proposing changes.
- SRC2 For dependency code questions, inspect ~/.m2 JARs first; fallback to upstream GitHub; never answer without referencing code.
- UPS1 Upstream dependencies (e.g., tui4j) are stable public libraries; do not modify without explicit request.
- UPS2 tui4j is a 1:1 port of Charmbracelet Go libraries; edits MUST match upstream file structure and logic exactly—no exceptions.
- NME1 Use clear, specific names; avoid abbreviations unless standard.
- FUN1 Keep functions small and focused; one responsibility per function.
- DRY1 Remove duplication; reuse existing utilities instead of rewriting logic.
- SS1 One semantic owner only: for any governed concept, exactly one file/module may define its field inventory, names, allowed keys, dependency graph, or behavior selection.
- SS2 No mirror owners anywhere: tests, fixtures, docs, examples, generators, tui4j wrappers, and helper modules are NOT exempt; they must bind/import/project the canonical owner instead of restating it.
- SS3 Projection rule: every non-canonical location may only project the canonical owner with identical governed names; renaming governed fields in projections is prohibited.
- SS4 No alias surfaces: plural/singular variants, compatibility aliases, label identifiers, alternate display-name identifiers, and convenience transport names for a governed concept are prohibited.
- SS5 Stop-work trigger: if an implementation requires listing the same governed fields/keys/rules in a second place, stop and redesign before editing.
- SS6 No positional-null constructor sludge: constructor/factory calls with repeated placeholder nulls or low-legibility optional argument trains are prohibited; use named factories/builders, parameter objects, or bind/import the canonical owner.
- TB1 Tracer bullet: build one tiny end-to-end slice through all layers first; validate it works; then expand — never build horizontal layers in isolation.
- KISS1 Simplest solution that works; achieve by removing, not adding; use platform/framework defaults unless deviation is proven necessary.
- ERR1 Use exceptions for exceptional cases; avoid defensive checks on trusted inputs.
- CMT1 Comments and Javadocs only when they add clarity; avoid academic tags like @author/@since/@version.
- FMT1 Keep formatting and style consistent with the surrounding file.
- TST1 Update or add tests when behavior changes; do not change behavior without coverage.
- PRC1 Repo-file-editing tasks start in a dedicated git worktree on a task branch (review/read-only tasks exempt); finish = tests green, worktree commits merged to `dev`, pushed, and any CI run watched to a terminal verdict.
- SYCO1 No empty confirmations ("You're right", "Absolutely") before investigation; verify then cite evidence.
- SLOP1 No `*Adapter`, `*Transformer`, `*Normalizer`, `*Bridge`, `*Converter`, `*Mapper`, `*Compatibility`, `*Transition` modules that exist solely to reshape data between equivalent types; fix the type mismatch at source.
- DEP1 Avoid unnecessary dependencies and unused code.
- LOC1 Line Count: All written, non-generated source files <= 500 lines (SRP Enforcer; Zero Tolerance).
- MO1 No Monoliths: Strict SRP; Decision Logic (New/Edit/Extract); OCP Extension.

## Details

- SRC1 Verify with primary sources before answering; do not infer behavior without evidence.
- SRC2 Use dependency source JARs or decompiled classes from ~/.m2 to confirm behavior; if not available, consult the dependency's GitHub repo; always cite file paths or class names used.
- UPS1 Do not add, remove, or refactor code in upstream dependencies unless the user explicitly requests it.
- UPS2 When editing tui4j: each Java file in `compat/` must map 1:1 to an upstream Go file; do not combine logic from multiple upstream files into one Java file; verify against `~/Developer/git/cursor/bubbletea`, `~/Developer/git/cursor/bubbles`, `~/Developer/git/cursor/lipgloss`, `~/Developer/git/cursor/x`.
- NME1 Prefer domain terms and intent-revealing names; rename unclear identifiers.
- FUN1 Split large methods; reduce branching and nested blocks when it improves readability.
- DRY1 Replace repeated logic with a shared function, utility, or existing helper.
- SS1 A governed concept must have exactly one semantic owner; if a file "knows the whole list" and is not that owner, the design is wrong.
- SS2 Tests, fixtures, docs, examples, generators, and helper modules are not exempt from the single-owner rule.
- SS3 Contract cleanup handoff must name the canonical owner, list each duplicate owner removed, prove that tests/fixtures now bind or import the canonical owner, and explicitly call out any remaining duplicate owner as a blocker.
- ERR1 Do not add guard clauses or try/catch in trusted codepaths unless required by the surrounding code or error model.
- CMT1 Keep documentation short and direct; explain why, not what; keep Javadocs concise and human.
- FMT1 Follow existing spacing, imports, and ordering; avoid style changes unrelated to the task.
- TST1 Prefer fast, focused tests; keep tests aligned with the public contract.
- DEP1 Remove unused imports, dependencies, and dead code.
- LOC1 Hard Cap: all written files <= 500 LOC. Enforcer: SRP/DDD. Zero Tolerance: no edits to >500 LOC files; split/retrofit first. Exempt: generated content.
- MO1 Monoliths: avoid multi-concern files. Logic: New → New File; Bug → Edit; Logic → Extract. Strict SRP: separate logic by actor/reason. Boundary: typed contracts, inward deps.
- Contract: `docs/contracts/code-change.md`

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
