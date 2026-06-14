---
name: backend-openapi-contract-changes
description: Use when changing public backend API behavior, OpenAPI-owned request or response shapes, generated DTO boundaries, endpoint contracts, or compatibility rules in Iced Latte backend.
---

# Backend OpenAPI Contract Changes

Public API work is a boundary change, not just an implementation edit.

## Read Order

1. `AGENTS.md`
2. `README.md`
3. The smallest relevant contract source or endpoint
4. Related docs such as `docs/order-module-openapi-spec.md` when present

## Rules

- Preserve public API contracts unless the task explicitly changes them.
- Keep only stable boundary DTOs, interfaces, and records in feature `api/` packages.
- Do not hide a contract problem by patching only internal code while leaving generated or documented API surfaces stale.
- If a compatibility transition is needed, prefer additive change and explicit migration steps over abrupt replacement.

## Verification

- Run the narrowest relevant Maven tests first.
- Broaden to endpoint or contract-related tests when the change affects request or response shape.
- Keep generated/OpenAPI-related changes in the same change set when they are part of one contract edit.
