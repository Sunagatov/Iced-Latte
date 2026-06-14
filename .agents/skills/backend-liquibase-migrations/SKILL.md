---
name: backend-liquibase-migrations
description: Use when adding or changing database schema, Liquibase changelogs, SQL migration files, rollout order, or schema-backed backend behavior in Iced Latte backend.
---

# Backend Liquibase Migrations

Schema changes are behavioral changes with rollout risk.

## Read Order

1. `AGENTS.md`
2. `docs/getting-started.md` only if local DB/test setup matters
3. The exact changelog entry and owning feature code
4. The smallest related tests

## Rules

- Keep migrations feature-scoped when the change belongs to one business slice.
- Do not mix unrelated refactors into migration changes.
- Preserve backward-compatible rollout sequencing when old and new code may coexist.
- If a public contract depends on a schema transition, coordinate the contract and migration steps deliberately.

## Verification

- Run the narrowest relevant Maven test class first.
- Prefer focused integration coverage for migration-backed behavior.
- Broaden verification only when the schema change crosses feature boundaries.
