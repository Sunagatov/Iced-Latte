# ADR-0001: Use modular monolith with Spring Modulith

## Status

Accepted

## Date

2026-05-19

## Context

Iced-Latte is a marketplace backend with product, cart, order, payment,
review, user, email, file-storage, security, and other domains. The system
is a single Spring Boot application. Splitting into microservices would add
network, deployment, observability, and data-consistency complexity that is
not justified at the current scale.

However, as the codebase grows, uncontrolled coupling between modules
becomes the main architectural risk. Without enforcement, services in one
domain silently start accessing repositories, entities, and internal
implementation details of other domains.

## Decision

We will keep the backend as a **modular monolith**. Business capabilities
are represented as Spring Modulith application modules (direct subpackages
of `com.zufar.icedlatte`).

We adopt two complementary tools:

1. **Spring Modulith** — detects application modules from package structure,
   verifies module boundaries, and flags illegal cross-module access.
2. **ArchUnit** — enforces additional architecture rules such as layering
   constraints, cycle detection, and package dependency restrictions.

### Rules enforced from Phase 1

- Controllers must not access repositories directly.
- The `common` package must not depend on feature modules.
- Feature packages must be free of dependency cycles.
- Spring Modulith module verification must pass (cross-module internal
  access is flagged).

### Future phases

- Extract public module APIs (interfaces) per domain.
- Move cross-module calls from repository/entity access to public APIs.
- Introduce domain events for async cross-module communication.
- Add documentation generation from module model.

## Consequences

### Positive

- Clearer boundaries between business domains.
- Architecture violations caught automatically in CI.
- Easier future extraction of modules into separate services if needed.
- Better onboarding — new contributors see explicit module contracts.
- Testable architecture, not just diagrams.

### Negative

- Some existing cross-module coupling must be refactored over time.
- Extra discipline required around package visibility.
- Public module APIs must be designed carefully to avoid leaking internals.

## References

- [Spring Modulith documentation](https://docs.spring.io/spring-modulith/reference/)
- [ArchUnit documentation](https://www.archunit.org/userguide/html/000_Index.html)
- [Feature Packaging guide](../feature-packaging.md)
