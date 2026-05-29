# Iced Latte Backend Agent Guide

These instructions are for Claude, Codex, Amazon Q / Kiro, Copilot, ChatGPT,
and other AI agents working in this backend source repository.

## Mission

Work as a targeted backend code assistant. Preserve the modular-monolith
architecture, existing Java/Spring style, public API contracts, and production
runtime boundary.

Default behavior: narrow reads, feature-owned changes, focused verification.

## Source-Of-Truth Boundary

- This repository owns backend source code, local development docs, tests,
  OpenAPI/source contracts, migrations, and CI behavior.
- `Iced-Latte-Frontend` owns the customer-facing frontend source.
- Vault owns production runtime files, deployment, secrets, observability,
  backups, and infra wiring when a local Vault checkout is available.
- Do not copy Vault secret/deploy procedures into this repo's adapters. For
  production runtime questions, switch to Vault and read its `AGENTS.md`.

## Vault Context

The sibling `Vault` checkout is the private operations and knowledge-base repo
shared across these pet projects. Use it only when a task needs
production/runtime facts: deployment flow, Docker Compose on the host, systemd or
host setup, SOPS-managed secrets, backups/restores, observability, reverse proxy,
infra inventory, or cross-project operational decisions. Start with Vault's
`AGENTS.md` and follow its routing docs instead of asking where production,
secrets, monitoring, or server-state information lives.

## Minimal Read Order

1. `AGENTS.md`
2. `README.md` for product/repo overview
3. `docs/getting-started.md` for local run setup
4. `docs/architecture/feature-packaging.md` for package ownership rules
5. The smallest relevant source files, tests, migration, or API spec

Use `docs/ai/README.md` for AI-specific routing notes. Do not scan the whole
repo, generated files, or unrelated feature packages by default.

## Durable Repo Facts

- Java 25, Spring Boot 4, Spring Framework 7, Maven.
- Modular monolith with feature packages under `src/main/java/com/zufar/icedlatte/`.
- PostgreSQL, Liquibase, Redis, MinIO/S3, Stripe, Kafka, OpenTelemetry, Sentry,
  Loki/Datadog-related observability dependencies.
- Tests use JUnit 5 and Testcontainers; Docker is needed for broad integration
  test coverage.
- Formatting is Palantir Java Format through Maven Spotless.

## Architecture Rules

1. Keep business code inside the owning feature package.
2. Put only interfaces, records, and stable boundary DTOs in `api/`.
3. Do not move feature-specific code into `common` unless at least two
   independent features need it and no feature owns the concept.
4. Keep refactors separate from behavior changes unless the refactor is required
   to make the behavior change safely.
5. Preserve OpenAPI, migration, and event-schema contracts when changing public
   behavior.

## Coding Rules

- Follow existing Java and Spring Boot style.
- Use Palantir Java Format through Maven.
- Do not reformat unrelated files.
- Do not touch unrelated dirty files.
- Do not read or print secrets.
- Prefer focused tests over full builds when the change is narrow.

## Verification

For Java formatting after code edits:

```bash
mvn spotless:apply
```

For normal local verification:

```bash
mvn clean package
```

For narrow changes, run the related Maven test class or module-specific command
first, then broaden only when risk justifies it.
