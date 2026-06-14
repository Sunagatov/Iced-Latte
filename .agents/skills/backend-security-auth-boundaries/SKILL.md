---
name: backend-security-auth-boundaries
description: Use when changing backend JWT auth, sessions, Google OAuth, security filters, auth endpoints, authentication or authorization rules, or security-owned module boundaries in Iced Latte.
---

# Backend Security Auth Boundaries

Security changes in this repo affect both feature boundaries and externally visible auth behavior.

## Read Order

1. `AGENTS.md`
2. `README.md`
3. The smallest relevant files under:
   - `security/`
   - `auth/`
   - related tests
4. Relevant error/contract docs when behavior is changing

## Core Rules

- Keep security implementation inside security-owned packages.
- Non-security modules should depend on `security :: api`, not security internals.
- Preserve JWT/session/OAuth behavior unless the task explicitly changes the contract.
- Treat filter responses, authentication entry points, and auth exceptions as public behavior surfaces.
- Avoid leaking sensitive auth details in error messages or logs.

## Verification

- Run the narrowest security/auth tests first.
- Include architecture or modularity checks when the change risks crossing security boundaries.
- Broaden verification when login, session refresh, or OAuth callback behavior changes.
