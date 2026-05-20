# 🧩 Feature Packaging Rule

Iced Latte is a modular monolith. That means the application is deployed as one backend, but the code is organized as independent business slices that could become separate services later.

The goal is simple: when a contributor opens a feature package, most of the code for that feature should be there.

---

## ✅ Core Rule

If a class belongs to one business feature, keep it inside that feature package.

Good examples:

| Class | Package | Why |
|---|---|---|
| `OrderNotFoundException` | `order/exception` | order-owned business error |
| `PaymentStatusService` | `payment/api` | payment-owned contract exposed to other features |
| `UserRepository` | `user/repository` | persistence for user-owned entities |
| `ProductReviewValidator` | `review/validator` | review-specific validation |
| `ProductEndpoint` | `product/endpoint` | product REST API entry point |
| `CartItemDtoConverter` | `cart/converter` | cart-specific DTO mapping |

Do not move feature-specific code into `common` just because it looks reusable.

---

## 📦 Feature Packages

Current backend feature packages:

| Package | Owns |
|---|---|
| `security` | registration, login, JWT, sessions, rate limiting, OAuth |
| `user` | user profiles, addresses, avatars |
| `product` | catalog, product filters, product images |
| `cart` | shopping cart state and operations |
| `order` | orders, order lifecycle, order history |
| `payment` | Stripe checkout, payments, webhooks |
| `review` | product reviews, ratings, AI moderation |
| `favorite` | favorites list |
| `email` | email verification and notifications |
| `filestorage` | file upload, download, S3/MinIO metadata |
| `astartup` | startup data migration and bootstrap tasks |

---

## 🏗️ Package Shape

Most feature packages use a shape like this:

```text
feature/
├── api/          # public module boundary: interfaces, records, stable DTOs
├── service/      # concrete application services (hidden from other modules)
├── endpoint/     # REST endpoints
├── entity/       # JPA entities owned by the feature
├── repository/   # persistence access for feature-owned entities
├── converter/    # DTO/entity mapping
├── validator/    # feature-specific validation
└── exception/    # feature-specific errors and handlers
```

Not every feature needs every folder. Add folders when they are useful, not just to match a template.

> **Convention:** New code should place only interfaces, records, and stable boundary DTOs in `api/`. Concrete application services belong in `service/` (or directly in the feature root for very small features). Legacy modules may still have concrete services in `api/` until refactored — see `order/` for the target pattern.

---

## 🔧 What Belongs In `common`

`common` is only for genuinely shared, cross-cutting concerns that are not owned by one business feature.

Allowed examples:

- framework configuration
- shared web error response model
- correlation/request filters
- monitoring helpers
- generic HTTP helpers
- generic pagination support
- generic validation helpers used by multiple independent features
- small utility code with no business ownership

`common` must not become a dumping ground for:

- feature-specific exceptions
- feature DTOs
- feature entities
- feature repositories
- feature services
- feature validators
- feature mappers
- feature constants
- "temporary" business logic

---

## ✅ `common` Promotion Rule

Start code inside the feature that needs it.

Move code to `common` only when all of these are true:

- at least two independent features use it
- it has no natural business owner
- the API is stable enough to share
- moving it reduces duplication without hiding business logic

If you are unsure, keep it in the feature package first. It is easier to promote stable shared code later than to untangle business logic from `common`.

---

## 🧭 Placement Decision

Before placing a class, ask:

1. Is this specific to one business feature?
2. Would this class move with that feature if we extracted it into a separate service?
3. Is this truly shared by at least two independent features?
4. Does this have business meaning, or is it generic infrastructure?

Use this rule:

| Answer | Place it in |
|---|---|
| Specific to one feature | that feature package |
| Would move with one feature | that feature package |
| Shared by multiple independent features and has no business owner | `common` |
| Shared but still clearly owned by one feature | owner feature, expose a small `api` |

---

## 🧠 Placement Examples

| New class | Correct place | Why |
|---|---|---|
| `OrderStatusTransitioner` | `order/` | Order lifecycle logic belongs to the order feature |
| `StripeWebhookVerifier` | `payment/` | Stripe webhook behavior is payment-owned |
| `ProductReviewValidator` | `review/validator` | Review validation belongs to the review feature |
| `ProductImageUploader` | `product/` or `filestorage/api` | Use `product` if the use case is product-owned; expose `filestorage/api` if product only needs a storage contract |
| `PaginationConfig` | `common/pagination` | Pagination is generic and used across independent features |
| `ReviewModerationException` | `review/exception` | Review-owned error |
| `JwtAuthenticationFilter` | `security/filter` | Authentication infrastructure belongs to security |
| `CorrelationIdFilter` | `common/correlation` | Request correlation is cross-cutting infrastructure |

---

## 🔌 Cross-Feature Dependencies

Avoid direct dependencies on another feature's internals.

Avoid:

```text
order -> payment.repository.*
favorite -> product.entity.*
review -> product.repository.*
cart -> product.entity.*
```

Prefer:

```text
order -> payment.api.PaymentStatusService
favorite -> product.api.ProductLookupService
review -> product.api.ProductExistenceChecker
cart -> product.api.ProductPriceProvider
```

The `api` package is the boundary a feature intentionally exposes to other features.

---

## 🚫 Bad Package Direction

Do not organize backend code by technical layer globally:

```text
controller/
service/
repository/
dto/
entity/
exception/
```

That structure makes features hard to understand because one business flow is scattered across the whole codebase.

Prefer feature-first organization:

```text
order/
├── endpoint/
├── api/
├── entity/
├── repository/
├── converter/
└── exception/
```

---

## ⚠️ Exception Handling Rule

Each feature owns its own business exceptions.

Preferred:

```text
review/exception/ProductReviewNotFoundException
product/exception/ProductNotFoundException
user/exception/UserNotFoundException
payment/exception/PaymentNotFoundException
```

`common` may keep framework-level or cross-cutting error handling:

- bean validation failures
- malformed JSON
- unsupported media type
- request binding errors
- fallback 500 handling
- shared ProblemDetail helpers if used across the app

If a feature needs custom status or payload mapping, that mapping should live in the feature package.

---

## 🧪 Testing Rule

Tests should follow the same ownership idea.

Feature behavior should be tested near the feature it belongs to:

```text
src/test/java/com/zufar/icedlatte/order/
src/test/java/com/zufar/icedlatte/payment/
src/test/java/com/zufar/icedlatte/review/
```

Shared infrastructure behavior can live under:

```text
src/test/java/com/zufar/icedlatte/common/
```

---

## ✅ Contributor Checklist

Before adding or moving a class:

- Does this belong to one feature?
- Would it move with that feature in a future service extraction?
- Am I depending on another feature's internals?
- Can I depend on a small `api` contract instead?
- Am I putting business logic into `common`?
- Did I update tests for the feature that owns the behavior?

When in doubt, keep code closer to the business feature that owns it.

---

## 🎯 Practical Target

A contributor should be able to open a feature package and find nearly everything needed for that slice:

- REST endpoints
- service contracts
- business logic
- repositories
- DTO mapping
- entities
- validators
- exceptions
- feature-local configuration
- tests

That is the default direction for all new code and for incremental refactors of existing code.
