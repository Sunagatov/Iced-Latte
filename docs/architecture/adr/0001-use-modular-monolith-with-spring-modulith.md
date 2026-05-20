# ADR-0001: Use modular monolith with Spring Modulith

## Status

Accepted

## Date

2026-05-19 (updated 2026-05-20)

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

We keep the backend as a **modular monolith**. Business capabilities are
represented as Spring Modulith application modules (direct subpackages of
`com.zufar.icedlatte`).

We adopt two complementary tools:

1. **Spring Modulith 2.0.6** — detects application modules from package
   structure, verifies module boundaries, and flags illegal cross-module
   access.
2. **ArchUnit 1.4.2** — enforces additional architecture rules such as
   layering constraints, cycle detection, and package dependency
   restrictions.

## Module boundary model

Each module is **CLOSED** by default. Only subpackages explicitly annotated
with `@NamedInterface` are accessible from other modules. Subpackages
without this annotation are internal — Spring Modulith will fail the build
if another module tries to access them.

Three infrastructure modules remain **OPEN** (all subpackages accessible):
- `security` — bidirectional coupling with `user` (inherent)
- `user` — bidirectional coupling with `security` (inherent)
- `openapi` — generated code used by all modules

## Current module state

| Module | Type | Exposed subpackages (@NamedInterface) | Internal |
|--------|------|---------------------------------------|----------|
| cart | CLOSED | `api/` | repository, entity, converter, endpoint, exception |
| order | CLOSED | `api/`, `exception/` | repository, entity, endpoint, converter, event, specification |
| payment | CLOSED | none | everything |
| product | CLOSED | `api/`, `api/filestorage/`, `entity/`, `converter/`, `exception/` | repository, endpoint, validator |
| review | CLOSED | `api/` | everything else |
| favorite | CLOSED | none | everything |
| filestorage | CLOSED | top-level, `dto/`, `exception/`, `aws/` | repository, converter |
| email | CLOSED | `api/token/`, `exception/`, `sender/` | config |
| common | CLOSED | all subpackages (shared infrastructure) | — |
| astartup | CLOSED | none | everything |
| security | OPEN | all | — |
| user | OPEN | all | — |
| openapi | OPEN | all (generated) | — |

## Rules enforced

### Spring Modulith (ModularityTests)
- Module structure verification passes (no illegal cross-module access).
- No dependency cycles between modules.
- Named interface snapshot test prevents accidental exposure of new subpackages.

### ArchUnit (ArchitectureRulesTest)
- REST controllers must not access repositories directly.
- `common` must not depend on feature modules.
- No module may depend on `astartup`.
- Business feature modules must be free of dependency cycles.
- `order.api` must not depend on order repositories, entities, or converters.
- Non-order modules must not depend on `order.internal`.

## Key architectural changes made

1. **auth → security.oauth** — merged OAuth into security module (eliminated auth↔security cycle).
2. **SentryUserContextFilter → security.monitoring** — removed common→security dependency.
3. **AuditConfig → Identifiable interface** — removed common→user dependency.
4. **EmailVerificationService → security.api** — eliminated email↔security cycle.
5. **Repositories made internal** — cart, order, product, review repositories are no longer accessible from other modules. Cross-module access goes through service APIs.
6. **OrderSnapshot DTO** — payment uses a record DTO instead of Order entity directly.
7. **order.api contract split** — moved concrete order services to `order.internal`.
   `order.api` now exposes narrow contracts: `OrderCheckoutApi`, `OrderPaymentApi`,
   and `OrderSnapshot`. Payment depends only on those contracts.

## Known remaining coupling (acceptable)

- `product.entity` exposed — cart and favorite have `@ManyToOne ProductInfo` (JPA relationship requires entity visibility; fixing requires DB schema change).
- `product.converter` exposed — cart's MapStruct mapper uses `ProductInfoDtoConverter`.
- `security ↔ user` cycle — inherent bidirectional coupling; both remain OPEN.

## Future work

- Apply the same API-purity pattern to product, cart, review, user, and security.
- Remove `product.entity` and `product.converter` named interfaces after replacing
  cart/favorite JPA entity coupling with product snapshots (requires Liquibase migration).
- Break `security ↔ user` cycle (extract `UserLookupApi` interface, close both modules).
- Introduce domain events for async cross-module communication.
- Tighten `common` — move module-specific types out of common into their owning modules.
- Add explicit `@ApplicationModule(allowedDependencies = ...)` after module APIs stabilize.

## Consequences

### Positive

- Architecture violations caught automatically in CI (build fails).
- Repositories are encapsulated — no module can bypass service APIs.
- Clear documentation of what each module exposes.
- Easier future extraction of modules into separate services.
- New contributors see explicit module contracts via `@NamedInterface`.

### Negative

- `product.entity` remains exposed due to JPA relationship constraints.
- `security` and `user` remain OPEN (inherent coupling).
- `@NamedInterface` annotations add `package-info.java` files to exposed subpackages.

## References

- [Spring Modulith documentation](https://docs.spring.io/spring-modulith/reference/)
- [ArchUnit documentation](https://www.archunit.org/userguide/html/000_Index.html)
- [Feature Packaging guide](../feature-packaging.md)
