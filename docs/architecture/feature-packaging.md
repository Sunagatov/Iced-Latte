# Feature Packaging Rule

This codebase follows a modular-monolith rule: every business feature package should be treated as a future microservice candidate.

## Core rule

If a class belongs to one business feature, it stays inside that feature package.

Examples:

- `order/exception/OrderNotFoundException`
- `payment/service/PaymentService`
- `user/repository/UserRepository`
- `review/validator/ProductReviewValidator`

Do not move feature-specific code into `common` just because it looks reusable.

## What belongs in `common`

`common` is only for genuinely shared, cross-cutting concerns that are not owned by one business feature.

Allowed examples:

- framework configuration
- shared web error response models
- correlation/request filters
- security support abstractions
- generic utility code
- generic validation helpers used by multiple features

`common` must not become a dumping ground for:

- feature-specific exceptions
- feature DTOs
- feature entities
- feature repositories
- feature services
- feature validators
- feature mappers
- feature constants

## Decision rule

Ask these questions before placing a class:

1. Is it specific to one business feature?
2. Would it move with that feature if we extracted the feature into a separate service?
3. Is it truly shared by at least two independent features without belonging naturally to either?

If the answer to `1` or `2` is yes, keep it inside the feature package.

Only place code in `common` when `3` is clearly true.

## Cross-feature dependency rule

Avoid direct dependencies on another feature's repositories, entities, and other internals.

Prefer:

- a small feature API or port
- a stable feature-level service boundary

Avoid:

- `order` importing `payment.repository.*`
- `favorite` importing `product.entity.*`
- `review` importing `product.repository.*` when a narrower product-facing API would do

## Exception handling rule

Each feature owns its own business exceptions.

Preferred:

- `review/exception/ProductReviewNotFoundException`
- `product/exception/ProductNotFoundException`
- `user/exception/UserNotFoundException`

`common` may keep framework-level error handling such as:

- bean validation failures
- malformed JSON
- unsupported media type
- fallback 500 handling

If a feature needs custom status or payload mapping, that mapping should live in the feature package.

## Practical target

A developer should be able to open a feature package and find nearly everything needed for that slice there:

- API layer
- service logic
- repositories
- DTOs
- entities
- validators
- mappers
- exceptions
- feature-local configuration

That is the default direction for all new code and for incremental refactors of existing code.
