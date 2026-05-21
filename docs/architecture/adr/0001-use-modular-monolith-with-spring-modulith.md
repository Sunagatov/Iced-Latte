# ADR-0001: Use modular monolith with Spring Modulith

## Status

Accepted

## Date

2026-05-19 (updated 2026-05-21)

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

Two infrastructure modules remain **OPEN** (all subpackages accessible):
- `ratelimit` — Servlet filter infrastructure shared across authenticated and unauthenticated request paths
- `openapi` — generated HTTP-edge code used by endpoint implementations

## Current module state

| Module | Type | Exposed subpackages (@NamedInterface) | Internal |
|--------|------|---------------------------------------|----------|
| cart | CLOSED | `api/` | service, repository, entity, converter, endpoint, exception |
| order | CLOSED | `api/`, `exception/` | repository, entity, endpoint, converter, event, specification |
| payment | CLOSED | none | everything |
| product | CLOSED | `api/`, `exception/` | service, entity, converter, repository, endpoint, validator |
| review | CLOSED | `api/` | everything else |
| favorite | CLOSED | none | everything |
| filestorage | CLOSED | `api/`, `api/dto/`, `exception/`, `aws/` | service, repository, converter |
| email | CLOSED | `api/token/`, `exception/`, `sender/` | config |
| common | CLOSED | all subpackages (shared infrastructure) | — |
| astartup | CLOSED | none | everything |
| ratelimit | OPEN | all | — |
| security | CLOSED | `api/`, `api/dto/` | config, endpoint, entity, exception, repository, service |
| user | CLOSED | `api/`, `api/dto/`, `exception/` | converter, endpoint, entity, repository, service |
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
- `product.api` must not depend on product services, repositories, entities, converters, or specifications.
- `cart.api` must not depend on cart services, repositories, entities, or converters.
- `review.api` must not depend on review services, repositories, entities, converters, or AI internals.
- Non-order modules must not depend on `order.service`.
- Non-product modules must not depend on `product.service`.
- Non-product modules must not depend on `product.entity` or `product.converter`.
- Non-cart modules must not depend on `cart.service`.
- Non-payment modules must not depend on `payment.service`.
- Public module API packages must not depend on generated OpenAPI DTOs.
- `user.api` must not depend on user repositories, entities, converters, or services.
- `security.api` must not depend on security config, repositories, entities, services, or exceptions.
- Non-user modules must not depend on user implementation packages.
- Non-security modules must not depend on security implementation packages.

## Key architectural changes made

1. **auth → security.oauth** — merged OAuth into security module (eliminated auth↔security cycle).
2. **SentryUserContextFilter → security.monitoring** — removed common→security dependency.
3. **AuditConfig → Identifiable interface** — removed common→user dependency.
4. **EmailVerificationService → security.api** — eliminated email↔security cycle.
5. **Repositories made internal** — cart, order, product, review repositories are no longer accessible from other modules. Cross-module access goes through service APIs.
6. **OrderSnapshot DTO** — payment uses a record DTO instead of Order entity directly.
7. **order.api contract split** — moved concrete order services to `order.service`.
   `order.api` now exposes narrow contracts: `OrderCheckoutApi`, `OrderPaymentApi`,
   and `OrderSnapshot`. Payment depends only on those contracts.
8. **filestorage.api contract** — cross-module callers depend on `FileStorageApi`
   instead of the concrete `FileStorageService`.
9. **Product entity/converter exposure removed** — cart and favorite now store product IDs
   and load catalog data through product APIs instead of depending on product JPA entities
   or MapStruct converters.
10. **product.api contract split** — moved concrete product services to `product.service`.
    `product.api` now exposes `ProductCatalogApi` and `ProductReviewProductApi`.
    Review, cart, favorite, order, and startup code depend on those contracts rather than
    product implementation services.
11. **cart.api contract split** — moved `ShoppingCartService` to `cart.service`.
    `cart.api` now exposes only the checkout/reorder contract used by order and payment.
12. **payment service package cleanup** — renamed the misleading `payment.api` implementation
    package to `payment.service`; payment remains closed and exposes no named API.
13. **Public API DTO packages** — public module DTOs now live under `api/dto`
    where the contract needs a DTO namespace. Generated OpenAPI DTOs remain HTTP-edge
    implementation types and are not exposed through public module APIs.
14. **security/user API snapshots** — cross-module user identity lookup now uses
    `CurrentUserProvider`, `CurrentUserSnapshot`, `UserLookupApi`, and
    `UserLookupSnapshot` instead of exposing generated user DTOs through module APIs.
15. **security/user modules closed** — password-change session revocation now uses
    `UserSessionsRevocationRequestedEvent`, so `user` no longer calls security services
    directly. Authenticated token identity lookup is exposed through
    `AuthenticatedTokenIdentityProvider`, keeping JWT internals inside security.

## Known remaining coupling (acceptable)

- `ratelimit` remains OPEN as shared request-filter infrastructure. It depends on
  `security.api` for authenticated-token identity, not on JWT implementation classes.

## Future work

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

- `ratelimit` remains OPEN while request-filter infrastructure is still shared broadly.
- `@NamedInterface` annotations add `package-info.java` files to exposed subpackages.

## References

- [Spring Modulith documentation](https://docs.spring.io/spring-modulith/reference/)
- [ArchUnit documentation](https://www.archunit.org/userguide/html/000_Index.html)
- [Feature Packaging guide](../feature-packaging.md)
