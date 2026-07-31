---
title: "Code change policy contract"
usage: "Use whenever creating/modifying files: where to put code, when to create new types, and how to stay SRP compliant"
description: "Evergreen contract for change decisions (new file vs edit), repository structure, and library architecture; references rule IDs in `AGENTS.md`"
---

# Code Change Policy Contract

See `AGENTS.md` ([LOC1], [MO1], [DEP1]).

## Non-negotiables (applies to every change)

- **SRP only**: each new type/method has one reason to change ([MO1]).
- **New feature → new file**; do not grow monoliths ([MO1]).
- **No edits to >500 LOC files**; first split/retrofit ([LOC1]).
- **Upstream Alignment**: Matches tui4j structure where relevant.

## Decision matrix: create new file vs edit existing

Use this as a hard rule, not a suggestion.

| Situation | MUST do | MUST NOT do |
|----------|---------|-------------|
| New library feature | Add a new, narrowly scoped type in the correct package ([MO1]) | “Just add a method” to an unrelated class ([MO1]) |
| Bug fix (existing behavior wrong) | Edit the smallest correct owner; add/adjust tests to lock behavior | Create a parallel/shadow implementation |
| Logic change in stable code | Extract/replace via composition; keep stable code stable ([MO1]) | Add flags, shims, or “compat” paths to hide uncertainty |
| Touching a large/overloaded file | Extract at least one seam (new type + typed contract) ([LOC1], [MO1]) | Grow the file further |

### When adding a method is allowed

Adding to an existing type is allowed only when all are true:

- It is the **same responsibility** as the type’s existing purpose ([MO1]).
- It does not pull in a new dependency direction.

If any bullet fails, create a new type.

## Create-new-type checklist (before you write code)

1. **Search/reuse first**: confirm a type/pattern doesn’t already exist.
2. **Pick the correct package**.
3. **Name by role** (ban generic names; suffix declares meaning).
4. **Keep the file small** (stay comfortably under 500 LOC; split by concept early) ([LOC1]).
5. **Add/adjust tests** using existing patterns/utilities.
6. **Verify** with repo-standard commands.

## Repository structure and naming (placement is part of the contract)

### Package structure

- `com.williamcallahan.chatclient.**`: Core library packages.

### Naming conventions

- Classes: `PascalCase`.
- Interfaces: `PascalCase`.
- Methods: `camelCase`.

## Verification gates (do not skip)

- LOC enforcement: manual check / script ([LOC1]).
- Build/test: `./gradlew build`.
