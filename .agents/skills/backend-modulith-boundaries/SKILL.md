---
name: backend-modulith-boundaries
description: Use when backend changes risk crossing feature boundaries, changing exported `api/` surfaces, or violating Spring Modulith dependency expectations in Iced Latte.
---

# Backend Modulith Boundaries

The modular-monolith structure is an architectural contract, not just packaging style.

## Read Order

1. `AGENTS.md`
2. `docs/architecture/feature-packaging.md`
3. The smallest relevant feature packages and public `api/` surfaces

## Core Rules

- Keep feature internals private by default.
- Export only small intentional contracts from feature `api/` packages.
- Avoid direct dependencies on another feature’s repositories, entities, or service internals.
- Do not move generated HTTP-edge DTOs into public module contracts.

## Smell Checks

- Is a feature reaching through another feature’s internals instead of a public API?
- Is `api/` leaking implementation classes?
- Would this change make future feature extraction harder?

## Verification

- Run the narrowest relevant tests first.
- If the repo has existing modularity tests for the touched area, include them when the boundary risk is real.
