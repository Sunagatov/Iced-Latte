---
name: backend-feature-packaging
description: Use when deciding where backend code should live in Iced Latte, especially feature ownership, `api/` boundaries, `common` vs feature placement, or cross-feature module dependencies in the modular monolith.
---

# Backend Feature Packaging

This backend is a modular monolith. Placement is part of the design.

## Read Order

1. `AGENTS.md`
2. `docs/architecture/feature-packaging.md`
3. The smallest relevant feature package and tests

## Core Rules

- If a class belongs to one business feature, keep it inside that feature package.
- Put only stable contracts, interfaces, records, and boundary DTOs in feature `api/` packages.
- Use `common` only for truly cross-cutting infrastructure with no natural business owner.
- Avoid direct dependencies on another feature’s internals; prefer a small exported feature API.

## Smell Checks

- Is a feature leaking implementation through `api/`?
- Is `common` collecting business logic or feature-owned exceptions?
- Would this class move with one feature if the system were extracted later?

## Verification

- Run the narrowest relevant Maven tests in the owning feature.
- Broaden only if the change crosses feature boundaries or public module contracts.
