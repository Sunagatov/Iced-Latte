# Architecture

## Stack
Java 25 · Spring Boot 3.5 · PostgreSQL + Liquibase · Redis · AWS S3 · Stripe · JWT (JJWT 0.12) · MapStruct · Lombok · OpenAPI 3 / SpringDoc · JUnit 5 + Testcontainers + REST Assured

## Module Map
```
src/main/java/com/zufar/icedlatte/
├── security/     # JWT auth, Google OAuth2, registration, login, rate limiting
├── user/         # User profile, avatar, delivery address, password
├── product/      # Product catalog, filtering, pagination, image URLs
├── cart/         # Shopping cart CRUD
├── order/        # Order creation (Stripe session → order)
├── review/       # Product reviews, ratings, AI moderation
├── favorite/     # Favorites list
├── email/        # Email verification tokens, notifications
├── filestorage/  # AWS S3 upload/download/presign
├── openai/       # LangChain4j AI integration (moderation)
├── payment/      # Stripe webhook, session, redirect handling
├── telemetry/    # Frontend performance reports
├── common/       # Exception handlers, audit, cache config, correlation
└── astartup/     # Startup data migration (S3 images, ratings recalc)
```

## Key Patterns
- Every module has: `endpoint/` → `api/` (service) → `repository/` → `entity/`
- DTOs via MapStruct converters in `converter/`
- Exception handlers per module (`@RestControllerAdvice`) + `GlobalExceptionHandler` with `@Order(Ordered.LOWEST_PRECEDENCE)`
- `SecurityPrincipalProvider` to get current userId anywhere
- `IntegrationTestBase` — all integration tests extend this, single shared Spring context

## Caching
- `productImageUrl` — 50min TTL (presigned S3 URLs)
- `brands`, `sellers` — 24h TTL
- JWT blacklist — Redis (`RedisJwtBlacklistService`) or in-memory fallback
- Email tokens — Redis (`TokenCache`, `TokenTimeExpirationCache`)

## Security Notes
- JWT access token: `PT24H`, refresh: `PT24H` (ISO-8601 in yaml — plain numbers = nanoseconds, not ms)
- `/auth/refresh` skips `JwtAuthenticationFilter` (uses `JwtRefreshTokenValidator` with refresh key)
- Rate limiting: `RateLimitingFilter` uses `request.getRemoteAddr()` only (no X-Forwarded-For)
- `GlobalExceptionHandler` must keep `@Order(Ordered.LOWEST_PRECEDENCE)` — otherwise it steals domain exceptions

## Logging Rules
- `INFO` = business event, `WARN` = expected client error (4xx), `ERROR` = unexpected server error (5xx) with stack
- No PII in logs (no email, name, phone) — use `userId` (UUID) only
- Handler is the single log point for exceptions — services must not log before throwing
- Format: `domain.event: field=value` (e.g. `cart.items.added: cartId={}`)

## Restore Points
- **Stripe/payment removed** — to restore: `git log --all --grep="STRIPE_RESTORE_POINT"`
  - Removed: entire `payment/` package, `payment-openapi.yaml`, `PaymentEmailConfirmation.java`
  - Modified: `OrderCreator`, `SecurityConstants`, `SpringSecurityConfiguration`, `RateLimitingFilter`, `application.yaml`, `pom.xml`
