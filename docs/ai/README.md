# Iced Latte Backend AI Notes

This folder owns AI-specific routing notes for this backend source repository.

Use:

- `AGENTS.md` for the shared agent bootloader
- `README.md` for product and repo overview
- `docs/getting-started.md` for local run setup
- `docs/architecture/feature-packaging.md` for modular-monolith package rules
- `docs/architecture/adr/` for accepted architecture decisions

Vault owns production runtime, deployment, secrets, observability, backups, and
infra wiring. If a task is about those surfaces, switch to the local Vault
checkout and read its `AGENTS.md`.

Keep this folder small. Do not turn it into a duplicate backend encyclopedia.

## Local Skills Policy

Keep backend-local skills small and boundary-aware.

- Prefer repo architecture docs, tests, and source over broad generic bundles.
- Add local skills only when they improve Spring, contract, debugging, or verification workflows for this repo.
