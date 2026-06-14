---
name: backend-testcontainers-routing
description: Use when choosing backend verification for changes that touch integration tests, PostgreSQL, Redis, Kafka, or any test flow that depends on Testcontainers in Iced Latte.
---

# Backend Testcontainers Routing

This repo has focused unit tests and heavier Docker-backed integration coverage. Choose deliberately.

## Read Order

1. `AGENTS.md`
2. `README.md`
3. The smallest affected test class and owning feature code

## Core Rules

- Prefer the narrowest relevant Maven test class first.
- Use Docker-backed integration coverage only when the change actually crosses DB, Redis, Kafka, or similar boundaries.
- Do not claim broad verification if Docker/Testcontainers were required but not run.

## Repo Facts

- Tests use JUnit 5 and Testcontainers.
- Docker is needed for broad integration coverage.

## Verification Ladder

1. Focused unit test or small test class
2. Focused integration test class using Testcontainers
3. Broader package/module verification only when risk justifies it
