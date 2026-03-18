# Known False Positives — Do Not Flag

## application.yaml
- `liquibase.drop-first: true` — intentional for open-source contributors, clean DB on every start
- `ai.api-key: "sk-api-key"` — placeholder; real key injected via env var

## OpenAPI specs
- `http://localhost:*` server URLs — intentional per spec, do not change scheme
- `401` on `/api/v1/users/password/reset` and `/api/v1/users/password/reset/confirm` — deliberate design, do not add or remove

## Java — Do Not Touch
- `JwtAuthenticationFilter.shouldNotFilter` for `/api/v1/auth/refresh` — intentional, refresh token validated internally by `JwtRefreshTokenValidator` with a different key
- `JwtClaimExtractor` catch block wrapping all exceptions into `JwtTokenHasNoUserEmailException` — intentional contract
- `SpringSecurityConfiguration` lines 44-45 — Spring Security DSL chaining, not log injection (not CWE-117/CWE-93)
- `FileDeleter` / `FileProvider` log statements — static strings only, not CWE-200
- `JwtAuthenticationFilter` lines 131-132 — no XSS vector, not CWE-79
- `JwtAuthenticationProvider` lines 32-33 — named variable intentional for stack clarity, do not inline

## Java — Unnamed Pattern Variables
- `_` in switch arms (e.g. `case SomeException _ ->`) requires Java 22+. This project is Java 21. Always use a named variable (e.g. `e`).

## Hardcoded Credentials — All Intentional Placeholders
- `local.env` / `.env` — `APP_JWT_SECRET`, `APP_JWT_REFRESH_SECRET`, `AWS_ACCESS_KEY`, `STRIPE_*`, `GOOGLE_*` — local dev placeholders only
- `application-test.yaml` — `datasource.password: postgres`, `jwt.secret`, `jwt.password`, `stripe.*`, `google.client-secret` — test fixtures
- `.github/workflows/*.yml` — all use `${{ secrets.* }}` references, not hardcoded values

## SQL — UPDATE Without WHERE
- `ProductInfoRepository` / `ProductReviewRepository` — `updateAverageRating`, `updateReviewsCount`, `updateLikesCount`, `updateDislikesCount` all have WHERE clauses; scanner misreads subquery WHERE
- `updateAll*` methods — intentional bulk recalculation, suppressed with `@SuppressWarnings("SqlWithoutWhereClause")`
