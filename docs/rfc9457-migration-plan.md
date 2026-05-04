# RFC 9457 ProblemDetail Migration Plan

## Goal

Replace the custom `ApiErrorResponse` with Spring Boot 4's built-in `ProblemDetail` (RFC 9457) so every error response — from `@ExceptionHandler` methods AND security filters — follows the internet standard. Align the OpenAPI specs and fix the frontend's broken error handling at the same time.

---

## Current state audit

### Backend: 4 distinct error response shapes

| Shape | Source | Content-Type | Fields | Files |
|---|---|---|---|---|
| **A** — `ApiErrorResponse` | 7 `@RestControllerAdvice` classes (36 handlers) | `application/json` | `message`, `httpStatusCode`, `timestamp`, `errors[]` | `GlobalExceptionHandler`, `SignInExceptionHandler`, `JwtTokenExceptionsHandler`, `OrderExceptionHandler`, `PaymentExceptionHandler`, `CommonExceptionHandler`, `UserExceptionHandler` |
| **B** — `JwtAuthenticationFilter` JSON | Filter-level, bypasses `@ExceptionHandler` | `application/json` | `error`, `message`, `status`, `timestamp` (ISO), `requestId` | `JwtAuthenticationFilter.java:84-130` |
| **C** — `RateLimitResponseWriter` JSON | Filter-level, bypasses `@ExceptionHandler` | `application/json` | `error`, `message`, `status`, `timestamp` (ISO), `retryAfter` | `RateLimitResponseWriter.java:29-45` |
| **D** — Spring default `sendError` | `authenticationEntryPoint` lambda | `application/json` | `timestamp`, `status`, `error`, `path` — **no `message`** | `SpringSecurityConfiguration.java:89` |

No `AccessDeniedHandler` is configured — 403 responses use Spring's default shape (same as D).

Additionally, 5 locations return `ResponseEntity` with **no body at all**:
- `UserEndpoint.java:82` — 404 (no avatar)
- `AuthEndpoint.java:72` — 503 (Google auth disabled)
- `GlobalExceptionHandler.java:166` — 406 (Not Acceptable)
- `GlobalExceptionHandler.java:174` — 405 (Method Not Allowed)
- `GlobalExceptionHandler` — `AsyncRequestNotUsableException` → 503 (void return, client disconnected)

### Backend: 29 custom exception classes + common base exceptions

All extend `RuntimeException`. Spread across: `cart` (4), `order` (4), `security` (8), `payment` (2), `filestorage` (2), `user` (2), `product` (1), `email` (2), `review` (1), `common` (4).

The 4 common base exceptions have `@ResponseStatus` and are used heavily (26 throw sites):
- `BadRequestException` (`@ResponseStatus(400)`) — 19 throw sites across `OrderCreator`, `CartEndpoint`, `CheckoutPaymentService`, `ProductReviewValidator`, `EmailVerificationService`, `PutUsersRequestValidator`, `GetProductsRequestValidator`, `GetReviewsRequestValidator`, `ProductsEndpoint`, `ProductReviewLikesUpdater`
- `NotFoundException` (`@ResponseStatus(404)`) — 1 throw site in `ProductReviewValidator`
- `UnauthorizedException` (`@ResponseStatus(401)`) — 1 throw site in `UserProfileService`
- `InternalServerErrorException` (`@ResponseStatus(500)`) — 0 throw sites found

### Backend: unhandled JDK exceptions (hit catch-all → 500)

14 `throw new IllegalStateException/IllegalArgumentException/SecurityException` sites have **no dedicated handler and no `@ResponseStatus`**, so they hit the catch-all and return 500:
- `SecurityPrincipalProvider.java:29` — `"No authenticated UserEntity in security context"` (should be 401)
- `GoogleAuthCallbackHandler.java:39` — `"Google account has no email"` (should be 400)
- `GoogleTokenExchanger.java:48` — `"Google ID token verification failed"` (should be 401)
- `AwsObjectStorage.java:84` — `SecurityException("Invalid directory path")` (should be 400)
- `ProductReviewDtoConverter.java:61` — `"Unexpected product's rating value: X"` (should be 400)
- `EmailVerificationService.java:104,112` — serialization failures (true 500)
- `StripeWebhookBusinessProcessor.java:229` — `"No orderId in Stripe session metadata"` (true 500)
- `SpringSecurityConfiguration.java:127,136,139` — startup config errors (not runtime)
- `RateLimitingFilter.java:63,67` — startup config errors (not runtime)
- `JwtBlacklistStore.java:62` — `"SHA-256 not available"` (true 500)

### Backend: `OrderExceptionHandler` scoping bug

`OrderExceptionHandler` is scoped to `basePackages = {"com.zufar.icedlatte.order.endpoint"}`. But `PaymentStatusService.java` (in the `payment` package) throws `OrderNotFoundException` and `OrderAccessDeniedException`. These bypass `OrderExceptionHandler` and hit the catch-all → **500 instead of 404/403**. These exceptions have no `@ResponseStatus`.

### Backend: `ReviewModerationException` never reaches a controller

`ReviewModerationException` is only thrown in the async `@TransactionalEventListener` path (`AsyncReviewProcessingService`), where it's caught and handled (review deleted). It **never propagates to a controller**. The frontend's hardcoded 422 message is dead code — no 422 is ever returned for review moderation. The first-pass finding H6 was incorrect.

### Backend: `UserNotFoundException` dual-status ambiguity

`UserNotFoundException` is handled by both `UserExceptionHandler` (HIGHEST_PRECEDENCE → 401) and `SignInExceptionHandler` (@Order(0) → 401). While functionally equivalent today, 401 is semantically wrong for non-auth contexts (e.g., admin user lookup). The migration should decide on a single semantic.

### Backend: handler ordering fragility

- `CommonExceptionHandler` (filestorage) and `GlobalExceptionHandler` both have **no @Order** — default to LOWEST_PRECEDENCE
- Two handlers at HIGHEST_PRECEDENCE: `UserExceptionHandler` (global) and `OrderExceptionHandler` (scoped)
- No conflicts today, but fragile for future additions

### OpenAPI spec: `ErrorResponse` schema (common-schemas.yaml)

```yaml
ErrorResponse:
  properties:
    message: string        # ← matches backend
    details: string[]      # ← backend sends errors: [{field, message}] — MISMATCH
    timestamp: date-time   # ← backend sends "yyyy-MM-dd HH:mm:ss" — FORMAT MISMATCH
    status: integer        # ← backend sends httpStatusCode — NAME MISMATCH
    path: string           # ← backend never sends this — MISSING
```

All 9 OpenAPI specs reference this same schema (some inline, all `$ref` to `common-schemas.yaml`).

### Frontend: error handling (`apiError.ts`)

```typescript
// src/shared/utils/apiError.ts — THE critical function
if (status === 401 || status === 403) return 'Incorrect email or password'  // WRONG for 403
if (status === 422) return 'Your review was rejected...'                     // WRONG; 422 never returned
return data.message || data.error || 'An unknown error occurred'
```

**Fields actually read at runtime:** `response.status`, `data.message`, `data.error` (fallback).
**Fields declared but never read:** `httpStatusCode`, `timestamp`, `errors[]`.

### Frontend: Next.js proxy layer — CRITICAL for migration

The proxy at `src/app/api/proxy/[...path]/route.ts` (line 174) parses backend responses:
```typescript
contentType.includes('application/json') && rawBody
  ? JSON.parse(rawBody) : rawBody
```

**This will NOT match `application/problem+json`.** ProblemDetail responses would be treated as raw text strings, not parsed as JSON objects. The frontend would receive a string instead of an object, and `data.message` would be `undefined`.

The proxy also emits synthetic errors with shape `{ error: 'API unavailable' }` (503) and `{ error: 'Invalid path' }` (400) for proxy-level failures — these are not ProblemDetail and must continue to work.

### Frontend: no error boundaries, no error logging

- Only one error boundary exists: `src/app/product/error.tsx` (product page only)
- No `global-error.tsx`, no root `error.tsx`, no `not-found.tsx`
- No error tracking (no Sentry, no DataDog)
- `react-toastify` is installed and `<ToastContainer />` is mounted in `layout.tsx`, but `toast()` is **never called** — dead code
- If the migration introduces a parsing error, most routes show Next.js's default error page with no recovery

### Frontend: error display patterns

| Pattern | Where | Behavior |
|---|---|---|
| `handleError()` → `errorMessage` state → rendered | `LoginForm`, `RegistrationForm`, `VerifyEmailCodeForm`, `ForgotPassForm`, `AuthResetPassForm`, `GuestResetPassForm`, `FormProfile`, `ReviewForm` | ✅ Works — shows `errorMessage` inline |
| `handleError()` → `errorMessage` state → **never rendered** | `ReviewsList`, `ImageUpload`, `UserBar`, `ProductWithReviews`, `ReviewsSection` | ❌ Error swallowed — user sees nothing |
| Hardcoded error strings | `OrderCard.tsx` (`'Could not cancel order.'`, etc.) | Backend message discarded |
| `.catch(() => {})` — silently swallowed | `cart.mutations.ts`, `cartStore.ts`, `useSearchBar.ts`, `ProfileScreen.tsx`, `session.ts`, `favorites.mutations.ts`, `favorites.sync.ts`, `useCheckoutForm.ts`, `addresses/store.ts`, `useLogout.ts` | ❌ Error invisible |
| `lastError` set in store, never rendered | `cartStore.ts` | ❌ Error invisible |
| `AuthInterceptor` → refresh → redirect | 401 handling | ✅ Functional but message always wrong |

---

## Inconsistencies and gaps found

### Critical (will break things or already broken)

| # | Issue | Backend | Frontend | Impact |
|---|---|---|---|---|
| **C1** | 403 shows "Incorrect email or password" | `OrderAccessDeniedException` → 403, `SessionOwnershipException` → 403 | `apiError.ts:16` hardcodes auth message for all 403 | User told wrong password when they lack permission |
| **C2** | 422 hardcoded to review rejection | `ReviewModerationException` is async-only — 422 is **never returned** to the client | `apiError.ts:18` hardcodes review message for all 422 | Dead code today, but will show wrong message for any future 422 |
| **C3** | No `AccessDeniedHandler` | Spring default 403 body has no `message` field | Falls to hardcoded "Incorrect email or password" anyway, but if fixed, `data.message` → `undefined` | |
| **C4** | OpenAPI `ErrorResponse` ≠ actual `ApiErrorResponse` | `httpStatusCode` vs `status`, `errors[{field,message}]` vs `details[string]`, timestamp format mismatch, `path` never sent | Any generated client would break | API docs lie to consumers |
| **C5** | **Proxy blocks `application/problem+json`** | ProblemDetail responses use `application/problem+json` content type | Proxy line 174: `contentType.includes('application/json')` won't match `+json` — response treated as raw text | **Every error becomes "An unknown error occurred"** |
| **C6** | `OrderExceptionHandler` scoping bug | `OrderNotFoundException`/`OrderAccessDeniedException` thrown from `payment` package bypass scoped handler → catch-all returns **500** | Frontend sees 500 instead of 404/403 | Pre-existing bug, must fix during migration |

### High (information leakage or lost functionality)

| # | Issue | Details |
|---|---|---|
| **H1** | `errors[]` validation array never read by frontend | Backend sends `[{field: "email", message: "must not be blank"}]` — user sees only "Validation failed" with no field details |
| **H2** | `retryAfter` from rate limiter ignored | No countdown/backoff UI |
| **H3** | `requestId` from JWT filter ignored | No log correlation for support |
| **H4** | 5 components call `handleError()` but never render `errorMessage` | `ReviewsList`, `ImageUpload`, `UserBar`, `ProductWithReviews`, `ReviewsSection` |
| **H5** | Cart errors silently swallowed | `.catch(() => {})` in `cart.mutations.ts`, `cartStore.ts` |
| **H6** | 14 JDK exception throw sites → 500 | `IllegalStateException`/`IllegalArgumentException`/`SecurityException` with no handler — several should be 400/401 |
| **H7** | Empty-body error responses | 5 locations return `ResponseEntity` with no body — clients expecting ProblemDetail get nothing |
| **H8** | No frontend error boundaries | Only `/product/[id]` has an error boundary — parsing failures during migration crash pages with no recovery |
| **H9** | No frontend error logging | No Sentry/DataDog — cannot detect if new response shape causes silent failures in production |
| **H10** | `UserNotFoundException` dual-status | Handled as 401 everywhere, but semantically wrong for non-auth contexts (admin lookup → should be 404) |

### Medium (inconsistency, not broken)

| # | Issue | Details |
|---|---|---|
| **M1** | 4 different JSON shapes from backend | Handlers (shape A), JWT filter (shape B), rate limiter (shape C), Spring default (shape D) |
| **M2** | Timestamp format inconsistency | Handlers: `"yyyy-MM-dd HH:mm:ss"`, Filters: ISO-8601, OpenAPI: `date-time` |
| **M3** | Status field name inconsistency | Handlers: `httpStatusCode`, Filters: `status`, OpenAPI: `status` |
| **M4** | `OrderCard.tsx` discards backend error messages | Uses hardcoded strings like "Could not cancel order." — loses context like "Cancellation window expired" |
| **M5** | Handler ordering fragility | `CommonExceptionHandler` and `GlobalExceptionHandler` both have no `@Order` — ambiguous resolution |
| **M6** | `react-toastify` installed but never used | `<ToastContainer />` mounted in `layout.tsx`, `toast()` never called — dead code, missed opportunity for non-form errors |
| **M7** | `ReviewModerationException` has no `@ResponseStatus` | Currently irrelevant (async-only), but if ever thrown synchronously, would hit catch-all → 500 |

---

## Target state

Every error response from the backend — whether from `@ExceptionHandler`, security filters, or Spring defaults — returns:

```json
{
  "type": "https://iced-latte.uk/errors/cart-not-found",
  "title": "Shopping cart not found",
  "status": 404,
  "detail": "Shopping cart not found.",
  "instance": "/api/v1/cart",
  "timestamp": "2026-05-04T20:00:00Z",
  "message": "Shopping cart not found.",
  "errors": [{"field": "email", "message": "must not be blank"}]
}
```

Content-Type: `application/problem+json` (from handlers) or `application/json` (from filters).

**Important:** The `@ExceptionHandler` methods must explicitly set `produces = "application/json"` (NOT `application/problem+json`) because the frontend proxy only parses `application/json`. Alternatively, the proxy must be updated first (see Phase 0). The recommended approach is to **keep `application/json`** content type and simply change the JSON body shape to match RFC 9457. This is a pragmatic deviation from the spec that avoids the proxy breakage.

### Backward compatibility

| Field | Source | Why |
|---|---|---|
| `message` | Extension property, duplicates `detail` | Frontend reads `data.message` — **must keep** |
| `error` | Extension property, duplicates `title` | Frontend reads `data.error` as fallback — **must keep** |
| `timestamp` | Extension property, ISO-8601 | Standardize format |
| `errors` | Extension property, `[{field, message}]` | Keep for future frontend use |

Fields dropped: `httpStatusCode` (use `status`), old timestamp format.

---

## Error type URI catalog

### Auth & Security

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `InvalidCredentialsException` | `invalid-credentials` | 401 | Invalid credentials |
| `AbsentBearerHeaderException` | `auth-required` | 401 | Authentication required |
| `JwtTokenException` | `auth-failed` | 401 | Authentication failed |
| `JwtTokenBlacklistedException` | `session-expired` | 401 | Session expired |
| `JwtTokenHasNoUserEmailException` | `auth-failed` | 401 | Authentication failed |
| `UserAccountLockedException` | `account-locked` | 401 | Account locked |
| `UserRegistrationException` | `registration-failed` | 400 | Registration failed |
| `SessionNotFoundException` | `session-not-found` | 404 | Session not found |
| `SessionOwnershipException` | `session-access-denied` | 403 | Access denied |

### User

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `UserNotFoundException` | `user-not-found` | 401 | User not found |
| `InvalidAvatarFileTypeException` | `invalid-avatar-type` | 400 | Invalid file type |

### Cart

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `ShoppingCartNotFoundException` | `cart-not-found` | 404 | Shopping cart not found |
| `ShoppingCartItemNotFoundException` | `cart-item-not-found` | 404 | Cart item not found |
| `InvalidShoppingCartIdException` | `cart-id-invalid` | 400 | Invalid cart ID |
| `InvalidItemProductQuantityException` | `cart-item-quantity-invalid` | 400 | Invalid item quantity |

### Product

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `ProductNotFoundException` | `product-not-found` | 404 | Product not found |

### Order

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `OrderNotFoundException` | `order-not-found` | 404 | Order not found |
| `OrderAccessDeniedException` | `order-access-denied` | 403 | Access denied |
| `InvalidOrderStateTransitionException` | `order-state-invalid` | 409 | Invalid order state |
| `OrderCancellationWindowExpiredException` | `order-cancellation-expired` | 409 | Cancellation window expired |

### Payment

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `StripeSessionCreationException` | `payment-session-failed` | 502 | Payment session failed |
| `PaymentEventProcessingException` | `payment-event-failed` | 400 | Payment event failed |

### File Storage

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `FileUploadException` | `file-upload-failed` | 500 | File upload failed |
| `FileReadException` | `file-read-failed` | 400 | File read failed |

### Email

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `InvalidTokenException` | `invalid-verification-code` | 400 | Invalid verification code |
| `TimeTokenException` | `verification-rate-limited` | 425 | Verification rate limited |

### Review

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `ReviewModerationException` | `review-moderation-failed` | 422 | Review rejected |

### Generic / Framework

| Exception | `type` slug | Status | `title` |
|---|---|---|---|
| `BadRequestException` (common) | `about:blank` | 400 | Bad request |
| `NotFoundException` (common) | `about:blank` | 404 | Not found |
| `UnauthorizedException` (common) | `about:blank` | 401 | Unauthorized |
| `InternalServerErrorException` (common) | `about:blank` | 500 | Internal server error |
| `MethodArgumentNotValidException` | `validation-failed` | 400 | Validation failed |
| `ConstraintViolationException` | `validation-failed` | 400 | Validation failed |
| `DataIntegrityViolationException` | `data-conflict` | 400 | Data conflict |
| `HttpMessageNotReadableException` | `malformed-request` | 400 | Malformed request |
| `MissingServletRequestParameterException` | `missing-parameter` | 400 | Missing parameter |
| `MethodArgumentTypeMismatchException` | `invalid-parameter` | 400 | Invalid parameter |
| `MaxUploadSizeExceededException` | `file-too-large` | 413 | File too large |
| `MultipartException` | `malformed-multipart` | 400 | Malformed request |
| `NoResourceFoundException` | `resource-not-found` | 404 | Resource not found |
| Catch-all (no `@ResponseStatus`) | `about:blank` | 500 | Internal server error |
| Catch-all (has `@ResponseStatus`) | `about:blank` | varies | varies |

### Filter-level (non-handler)

| Source | `type` slug | Status | `title` |
|---|---|---|---|
| JWT filter: `InvalidCredentialsException` | `invalid-credentials` | 401 | Authentication failed |
| JWT filter: `JwtTokenBlacklistedException` | `session-expired` | 401 | Authentication failed |
| JWT filter: `ExpiredJwtException` | `token-expired` | 401 | Authentication failed |
| JWT filter: `JwtTokenHasNoUserEmailException` | `auth-failed` | 401 | Authentication failed |
| JWT filter: `UsernameNotFoundException` | `auth-failed` | 401 | Authentication failed |
| JWT filter: default/internal error | `auth-internal-error` | 500 | Authentication error |
| Rate limiter | `rate-limited` | 429 | Too many requests |
| `authenticationEntryPoint` | `auth-required` | 401 | Authentication required |
| Missing `AccessDeniedHandler` | `access-denied` | 403 | Access denied |

---

## Implementation steps

### Phase 0: Pre-migration fixes (backend + frontend, separate PRs)

These fix pre-existing bugs and prepare the ground. Deploy before the ProblemDetail migration.

#### Step 0.1: Fix `OrderExceptionHandler` scoping bug

Either remove `basePackages` scope from `OrderExceptionHandler` (make it global), or add `@ResponseStatus(NOT_FOUND)` to `OrderNotFoundException` and `@ResponseStatus(FORBIDDEN)` to `OrderAccessDeniedException`. The latter is safer — it fixes the bug without changing handler routing.

#### Step 0.2: Fix frontend proxy content-type check

In `src/app/api/proxy/[...path]/route.ts` line 174, change:
```typescript
contentType.includes('application/json') && rawBody
```
to:
```typescript
(contentType.includes('application/json') || contentType.includes('application/problem+json')) && rawBody
```

This is backward-compatible — deploy it before the backend migration.

#### Step 0.3: Deploy frontend `apiError.ts` fix FIRST

The updated `handleAxiosError` (see Phase 3, Step 3.1) reads `data.detail || data.message` — this is backward-compatible with the current backend. Deploy it before the backend ProblemDetail migration to eliminate the window where the frontend can't parse new responses.

#### Step 0.4: Add `@ResponseStatus` to unhandled exceptions

Add `@ResponseStatus` to exceptions that currently hit the catch-all with wrong status:
- `OrderNotFoundException` → `@ResponseStatus(NOT_FOUND)` (if not done in 0.1)
- `OrderAccessDeniedException` → `@ResponseStatus(FORBIDDEN)` (if not done in 0.1)
- `ReviewModerationException` → `@ResponseStatus(UNPROCESSABLE_ENTITY)` (defensive, even though async-only)

#### Step 0.5: Add temporary `console.error` logging in frontend

Add `console.error('[API Error]', status, data)` in `handleAxiosError` during the migration window. Remove after confirming stable. This compensates for the lack of Sentry.

### Phase 1: Backend — unified ProblemDetail responses

#### Step 1.1: Create `ProblemDetailFactory`

New file: `common/exception/handler/ProblemDetailFactory.java`

Replaces `ApiErrorResponseCreator`. Methods:
- `build(String typeSlug, String title, HttpStatus status, String detail)` → `ProblemDetail`
- `build(String typeSlug, String title, HttpStatus status, String detail, List<FieldError> errors)` → `ProblemDetail`
- Auto-sets `timestamp` (ISO-8601), `message` (= `detail`), `error` (= `title`) as extension properties
- `instance` set from `RequestContextHolder` if available

Constant: `TYPE_BASE = "https://iced-latte.uk/errors/"`.

**Content-Type decision:** Use `produces = MediaType.APPLICATION_JSON_VALUE` on all handlers (not `application/problem+json`). This avoids the proxy content-type issue even if Step 0.2 hasn't been deployed yet. The JSON body follows RFC 9457 structure regardless of content type.

#### Step 1.2: Migrate all 7 `@RestControllerAdvice` classes

Change every `@ExceptionHandler` method:
- Return type: `ApiErrorResponse` → `ProblemDetail`
- Use `ProblemDetailFactory` instead of `ApiErrorResponseCreator`
- Set correct `type` slug per catalog above
- Keep all `@ResponseStatus` annotations and logging unchanged

Migration order (lowest risk first):
1. `UserExceptionHandler` (2 handlers)
2. `CommonExceptionHandler` / filestorage (2 handlers)
3. `PaymentExceptionHandler` (2 handlers)
4. `OrderExceptionHandler` (5 handlers)
5. `JwtTokenExceptionsHandler` (3 handlers)
6. `SignInExceptionHandler` (9 handlers)
7. `GlobalExceptionHandler` (13 handlers, catch-all)

#### Step 1.3: Migrate filter-level error responses

**`JwtAuthenticationFilter.handleAuthenticationException()`**: Replace manual `ObjectNode` JSON with ProblemDetail-shaped JSON. Keep `requestId` as extension property (no ProblemDetail equivalent; `instance` is the request path, not a correlation ID).

**`RateLimitResponseWriter.writeTooManyRequests()`**: Same — produce ProblemDetail-shaped JSON. Keep `retryAfter` as extension property (redundant with `Retry-After` header, but removing it is a breaking change). Keep `Retry-After` header.

**`SpringSecurityConfiguration` `authenticationEntryPoint`**: Replace `response.sendError()` lambda with a custom `AuthenticationEntryPoint` bean that writes ProblemDetail JSON. This fixes the missing `message` field in 401 responses.

**Add `AccessDeniedHandler`**: New bean in `SpringSecurityConfiguration` that writes ProblemDetail JSON with type `access-denied`, status 403. This fixes C3.

#### Step 1.4: Convert empty-body `ResponseEntity` returns

- `UserEndpoint.java:82` — return ProblemDetail body with type `about:blank`, detail "User has no avatar"
- `AuthEndpoint.java:72` — return ProblemDetail body with detail "Google authentication is not available"
- `GlobalExceptionHandler` 405/406 — return ProblemDetail body instead of `ResponseEntity<Void>`
- `AsyncRequestNotUsableException` — leave as void (client already disconnected)

#### Step 1.5: Add handlers for JDK exceptions that should not be 500

Add to `GlobalExceptionHandler`:
- `SecurityException` → 400
- `IllegalArgumentException` from known throw sites → 400

Or better: replace the throw sites in `SecurityPrincipalProvider`, `GoogleAuthCallbackHandler`, `GoogleTokenExchanger`, `AwsObjectStorage`, `ProductReviewDtoConverter` with custom exceptions that have `@ResponseStatus`.

#### Step 1.6: Delete old classes

- `common/exception/dto/ApiErrorResponse.java`
- `common/exception/handler/ApiErrorResponseCreator.java`

#### Step 1.7: Update OpenAPI `ErrorResponse` schema

Replace `common-schemas.yaml` `ErrorResponse` with RFC 9457 fields:

```yaml
ErrorResponse:
  type: object
  required: [type, title, status]
  properties:
    type:
      type: string
      format: uri
      example: "https://iced-latte.uk/errors/cart-not-found"
    title:
      type: string
      example: "Shopping cart not found"
    status:
      type: integer
      example: 404
    detail:
      type: string
      example: "Shopping cart not found."
    instance:
      type: string
      example: "/api/v1/cart"
    message:
      type: string
      description: "Backward-compatible alias for detail"
    error:
      type: string
      description: "Backward-compatible alias for title"
    timestamp:
      type: string
      format: date-time
    errors:
      type: array
      items:
        type: object
        properties:
          field:
            type: string
          message:
            type: string
```

### Phase 2: Backend tests

#### Step 2.1: Create `ProblemDetailFactoryTest`

Replaces `ApiErrorResponseCreatorTest`. Verify `type`, `title`, `status`, `detail`, extension properties.

#### Step 2.2: Update all 6 handler test files

For each test: assert on `ProblemDetail` fields instead of `ApiErrorResponse` fields. Verify `type` URI, `title`, `status`, `detail`.

Files:
- `GlobalExceptionHandlerTest.java` (37 refs)
- `SignInExceptionHandlerTest.java` (31 refs)
- `JwtTokenExceptionsHandlerTest.java` (13 refs)
- `UserExceptionHandlerTest.java` (13 refs)
- `CommonExceptionHandlerTest.java` (7 refs)
- `OrderExceptionHandlerTest.java` (6 refs)

#### Step 2.3: Update integration tests

Any REST Assured / MockMvc tests asserting on `message` / `httpStatusCode` → assert on `detail` / `status` (or `message` alias).

#### Step 2.4: Run full suite

`mvn test` — all 649+ tests pass.

### Phase 3: Frontend fixes (separate PR, can be done in parallel)

#### Step 3.1: Fix `apiError.ts` — the critical function

```typescript
export const handleAxiosError = (error: unknown): string => {
  if (!axios.isAxiosError(error) || !error.response) {
    return 'An unknown error occurred'
  }

  const { status, data } = error.response

  // Auth failures — redirect handled by AuthInterceptor,
  // this is the fallback message
  if (status === 401) {
    return data?.detail || data?.message || 'Please sign in to continue.'
  }

  // Authorization failures — NOT auth failures
  if (status === 403) {
    return data?.detail || data?.message || 'You do not have permission to perform this action.'
  }

  // Use backend message (detail for ProblemDetail, message for legacy)
  return data?.detail || data?.message || data?.title || data?.error || 'An unknown error occurred'
}
```

This fixes: C1 (403 message), C2 (422 hardcoding removed), and works with both old and new response shapes during rolling deployment.

#### Step 3.2: Fix components that call `handleError()` but never render `errorMessage`

Add `{errorMessage && <p className="text-negative text-sm">{errorMessage}</p>}` to:
- `ReviewsList.tsx`
- `ImageUpload.tsx`
- `UserBar.tsx`
- `ProductWithReviews.tsx`
- `ReviewsSection.tsx`

#### Step 3.3: Update `ErrorResponse` type

```typescript
export type ErrorResponse = {
  // RFC 9457 ProblemDetail fields
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  // Backward-compatible aliases
  message?: string
  error?: string
  // Extensions
  timestamp?: string
  errors?: Array<{ field: string; message: string }>
}
```

#### Step 3.4: (Optional) Read `errors[]` for validation

In form components (`LoginForm`, `RegistrationForm`, etc.), map server-side `errors[]` to field-level error display. This is a larger UX improvement — can be a follow-up.

---

## Files changed (estimated)

| Action | Files | Count |
|---|---|---|
| **Create** | `ProblemDetailFactory.java`, `ProblemDetailFactoryTest.java`, `AccessDeniedHandler` bean, `AuthenticationEntryPoint` bean | 4 |
| **Modify** | 7 exception handler classes | 7 |
| **Modify** | `JwtAuthenticationFilter.java`, `RateLimitResponseWriter.java`, `SpringSecurityConfiguration.java` | 3 |
| **Modify** | Exception classes needing `@ResponseStatus` (`OrderNotFoundException`, `OrderAccessDeniedException`, `ReviewModerationException`) | 3 |
| **Modify** | Empty-body `ResponseEntity` returns (`UserEndpoint`, `AuthEndpoint`, `GlobalExceptionHandler`) | 3 |
| **Modify** | 6 handler test files | 6 |
| **Modify** | Integration tests asserting on error shape | ~5–10 |
| **Modify** | `common-schemas.yaml` (OpenAPI) | 1 |
| **Delete** | `ApiErrorResponse.java`, `ApiErrorResponseCreator.java`, `ApiErrorResponseCreatorTest.java` | 3 |
| **Backend total** | | **~30–37 files** |
| **Modify** | `apiError.ts`, `apiError.test.ts`, `ErrorResponse.ts` | 3 |
| **Modify** | Proxy route (`route.ts` — content-type fix) | 1 |
| **Modify** | 5 components with unrendered `errorMessage` | 5 |
| **Frontend total** | | **~9 files** |

---

## Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **Proxy blocks `application/problem+json`** | Every error becomes "An unknown error occurred" | **Phase 0.2**: fix proxy content-type check BEFORE backend migration. Also: use `application/json` content type on handlers as defense-in-depth |
| Frontend reads `data.message` | Every error shows fallback if `message` missing | Include `message` as extension property (duplicating `detail`) — **P0** |
| Frontend reads `data.error` as fallback | Fallback breaks if `error` missing | Include `error` as extension property (duplicating `title`) — **P0** |
| No frontend error boundaries | Parsing failure during migration crashes most pages | Deploy frontend changes (Phase 0.3) BEFORE backend migration |
| No frontend error logging | Cannot detect silent failures in production | Add temporary `console.error` logging (Phase 0.5) |
| `OrderExceptionHandler` scoping bug | 500 instead of 404/403 for payment-thrown order exceptions | Fix in Phase 0.1 before migration |
| JDK exceptions → 500 | `IllegalStateException` etc. leak internal messages | Add handlers or replace with custom exceptions (Step 1.5) |
| Filter-level responses not migrated | 4 different JSON shapes persist | Migrate filters in same PR — Step 1.3 |
| Rolling deployment window | Some backend instances serve old format, some new | Frontend reads `data.detail \|\| data.message` — works with both |
| External QA repo integration tests | May assert on old field names | `message` alias ensures backward compat. Coordinate with QA. |
| `spring.mvc.problemdetails.enabled=true` | Changes actuator error responses, may break monitoring | Do NOT enable this flag. Handle ProblemDetail manually in handlers. |
| OpenAPI-generated clients | Currently broken anyway (field name mismatches) | Fix is net positive |

---

## Sequencing (deploy order matters)

1. **Frontend PR 0** (Phase 0.2, 0.3, 0.5): Fix proxy content-type check, update `apiError.ts` to read `detail || message`, add temp logging. **Deploy first** — backward-compatible with current backend.
2. **Backend PR 0** (Phase 0.1, 0.4): Fix `OrderExceptionHandler` scoping, add `@ResponseStatus` to unhandled exceptions. **Deploy second** — fixes pre-existing bugs.
3. **Backend PR 1** (Phase 1 + 2): Full ProblemDetail migration — handlers, filters, tests, OpenAPI. **Deploy third** — frontend already handles new shape.
4. **Frontend PR 1** (Phase 3.2, 3.3): Fix unrendered `errorMessage` components, update `ErrorResponse` type. **Deploy fourth** — polish.
5. **Frontend PR 2** (Phase 3.4, follow-up): Validation `errors[]` display, activate toast for non-form errors.
6. **Future**: Remove `message`/`error` backward-compat aliases once frontend is fully migrated.

---

## Out of scope

- Error type URI documentation pages (each `type` URI resolving to a docs page)
- i18n / localization
- Frontend `errors[]` field-level validation display (Phase 3.4 — follow-up)
- `retryAfter` countdown UI for 429
- Cart error rendering (`lastError` in store)
- Activating `react-toastify` for non-form errors (follow-up)
- Adding `global-error.tsx` / root `error.tsx` error boundaries (follow-up)
- Adding Sentry/error tracking to frontend (follow-up)
- Consolidating 7 handler classes into 2–3 (nice-to-have, reduces ordering fragility)
- Splitting `UserNotFoundException` into auth vs non-auth variants (nice-to-have)
- `spring.mvc.problemdetails.enabled=true` (too broad — affects actuator, `/error` endpoint)
