# RFC 9457 ProblemDetail Migration Plan

## Goal

Replace the custom `ApiErrorResponse` with Spring Boot 4's built-in `ProblemDetail` (RFC 9457) so every error response — from `@ExceptionHandler` methods AND security filters — follows the internet standard. Align the OpenAPI specs and fix the frontend's broken error handling at the same time.

---

## Why RFC 9457 — the golden standard for BE/FE error contract (2026)

RFC 9457 "Problem Details for HTTP APIs" is the IETF internet standard for structured error responses. It is natively supported by Spring Boot 4's `ProblemDetail` class, adopted by Cloudflare (March 2026), and follows the same philosophy as Stripe's error design. This is the modern approach we are adopting for Iced Latte in 2026.

### The problem we are solving

Today, the Iced Latte backend and frontend have a **tightly coupled, fragile error contract**. The backend sends human-readable text in `message`, and the frontend displays it raw to the user. This creates five problems:

**Problem 1: The backend doesn't know the UX context.**

When `OrderNotFoundException` is thrown, the backend returns `"Order not found."` — but that message means different things on the order history page ("We couldn't find this order in your history") vs the checkout page ("Your order could not be processed"). The backend doesn't know which page the user is on. Only the frontend knows.

**Problem 2: The backend doesn't know the user's language.**

If Iced Latte adds Russian or German localization, the backend would need to know the user's locale to return the right message. That's the frontend's job — it already knows the browser language.

**Problem 3: Tight coupling between teams.**

Today, if a backend developer changes an exception message from `"Order not found."` to `"The requested order does not exist."`, the user-facing text changes with no frontend review. Backend and frontend teams can't work independently on error UX.

**Problem 4: Multiple clients need different wording.**

A mobile app, an admin panel, and the website all need different error messages for the same `OrderNotFoundException`. With the current approach, the backend would need to know which client is calling.

**Problem 5: The current contract is fragile and inconsistent.**

The backend currently returns **4 different JSON shapes** depending on where the error originates:

```
Shape A — @ExceptionHandler (36 handlers):
  {"message": "Order not found.", "httpStatusCode": 404, "timestamp": "2026-05-04 20:00:00"}

Shape B — JwtAuthenticationFilter:
  {"error": "Unauthorized", "message": "Authentication failed: token expired", "status": 401, "timestamp": "2026-05-04T20:00:00Z", "requestId": "abc-123"}

Shape C — RateLimitResponseWriter:
  {"error": "Rate limit exceeded", "message": "Too many requests. Please try again later.", "status": 429, "timestamp": "2026-05-04T20:00:00Z", "retryAfter": 60}

Shape D — SpringSecurityConfiguration:
  {"error": "Unauthorized", "message": "Authentication required.", "status": 401, "timestamp": "2026-05-04T20:00:00Z", "path": "/api/v1/cart"}
```

The frontend currently survives this by accident — all 4 shapes happen to include a `message` field. But the field names, timestamp formats, and available metadata are all different. One wrong refactor and the frontend shows "An unknown error occurred" for everything.

### The solution: RFC 9457 ProblemDetail

One standard shape. Every error. Every source.

```json
{
  "type": "https://iced-latte.uk/errors/order-not-found",
  "title": "Order not found",
  "status": 404,
  "detail": "Order not found.",
  "instance": "/api/v1/orders/123",
  "timestamp": "2026-05-04T20:00:00Z",
  "errors": []
}
```

### What each field means and who it's for

| Field | Audience | Purpose | Stability |
|---|---|---|---|
| `type` | **Machines** (frontend code) | Stable URI — the error code the frontend switches on. This is the primary identifier. | **Never changes** once published |
| `title` | Developers | Short human-readable label for logs and API docs. Not for end users. | Stable but not a contract |
| `status` | Both | HTTP status code repeated in the body for convenience. | Matches HTTP status |
| `detail` | Developers | Explanation of what went wrong. For logs, debugging, API consumers. **Not for end users.** | May change between releases |
| `instance` | Developers/support | The request path, for log correlation and support tickets. | Per-request |
| `timestamp` | Developers/support | ISO-8601, for log correlation. Extension property. | Per-request |
| `errors` | Both | Field-level validation errors `[{field, message}]`. Extension property. | Stable structure |

### The core principle

> The **backend** is the source of truth for **what went wrong** — it returns `type` + `status`.
> The **frontend** is the source of truth for **how to tell the user** — it maps `type` to user-facing copy.

### How this looks in practice — real Iced Latte example

**Backend** — `OrderExceptionHandler` (Phase 1 target):

```java
@ExceptionHandler(OrderNotFoundException.class)
public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
    return problemDetailFactory.build(
        "order-not-found",           // type slug → https://iced-latte.uk/errors/order-not-found
        "Order not found",           // title (for developers)
        HttpStatus.NOT_FOUND,        // status
        "Order not found."           // detail (for developers/logs)
    );
}
```

Returns:
```json
{
  "type": "https://iced-latte.uk/errors/order-not-found",
  "title": "Order not found",
  "status": 404,
  "detail": "Order not found.",
  "instance": "/api/v1/orders",
  "timestamp": "2026-05-04T20:00:00Z",
  "message": "Order not found.",
  "error": "Order not found"
}
```

(`message` and `error` are temporary backward-compat aliases — removed once frontend fully migrates.)

**Frontend** — `OrderCard.tsx` (Phase 3 target):

```typescript
// BEFORE (current — tightly coupled, hardcoded, loses backend context):
} catch {
  setActionError('Could not cancel order.')  // backend says WHY, but we throw it away
}

// AFTER (Phase 3 — frontend owns user-facing copy, switches on type):
} catch (err) {
  setActionError(getUserMessage(err))
}
```

**Frontend** — the `getUserMessage` function (Phase 3 target):

```typescript
const ERROR_MESSAGES: Record<string, string> = {
  // Auth & Security
  'https://iced-latte.uk/errors/invalid-credentials':    'The email or password you entered is incorrect.',
  'https://iced-latte.uk/errors/auth-required':          'Please sign in to continue.',
  'https://iced-latte.uk/errors/session-expired':        'Your session expired. Please sign in again.',
  'https://iced-latte.uk/errors/account-locked':         'Your account has been locked. Please contact support.',
  'https://iced-latte.uk/errors/access-denied':          'You do not have permission to perform this action.',
  'https://iced-latte.uk/errors/registration-failed':    'This email is already registered. Please sign in.',
  'https://iced-latte.uk/errors/user-not-found':         'User not found.',
  'https://iced-latte.uk/errors/session-not-found':      'Your session could not be found. Please sign in again.',
  'https://iced-latte.uk/errors/session-access-denied':  'You do not have access to this session.',

  // Orders
  'https://iced-latte.uk/errors/order-not-found':        'We couldn\'t find this order.',
  'https://iced-latte.uk/errors/order-access-denied':    'You do not have permission to access this order.',
  'https://iced-latte.uk/errors/order-state-invalid':    'This order can no longer be modified.',
  'https://iced-latte.uk/errors/order-cancellation-expired': 'The cancellation window for this order has passed.',

  // Cart
  'https://iced-latte.uk/errors/cart-not-found':         'Your shopping cart could not be found.',
  'https://iced-latte.uk/errors/cart-item-not-found':    'This item is no longer in your cart.',

  // Products & Reviews
  'https://iced-latte.uk/errors/product-not-found':      'This product is no longer available.',
  'https://iced-latte.uk/errors/review-moderation-failed': 'Your review could not be published — it may contain inappropriate content.',

  // Validation & Generic
  'https://iced-latte.uk/errors/validation-failed':      'Please check the form for errors.',
  'https://iced-latte.uk/errors/file-too-large':         'The file you selected is too large.',
  'https://iced-latte.uk/errors/rate-limited':           'Too many requests. Please wait a moment and try again.',

  // Payment
  'https://iced-latte.uk/errors/payment-session-failed': 'We couldn\'t process your payment. Please try again.',

  // Remaining catalog slugs (bad-request, not-found, unauthorized, malformed-request,
  // missing-parameter, invalid-parameter, data-conflict, file-upload-failed, file-read-failed,
  // invalid-avatar-type, invalid-verification-code, verification-rate-limited, etc.)
  // are handled by the fallback chain: data.detail || data.message || 'Something went wrong.'
}

export function getUserMessage(error: unknown): string {
  if (!axios.isAxiosError(error) || !error.response) {
    return 'Network error. Please check your connection.'
  }
  const data = error.response.data as ErrorResponse

  // If we have a mapped type, use our user-facing copy
  if (data?.type && ERROR_MESSAGES[data.type]) {
    return ERROR_MESSAGES[data.type]
  }

  // Fallback chain for unmapped types or during migration
  return data?.detail || data?.message || data?.error || 'Something went wrong. Please try again.'
}
```

### Why this is better than what we have today

| Aspect | Current (2024 pattern) | RFC 9457 (2026 standard) |
|---|---|---|
| Error identity | HTTP status code only | `type` URI — stable, machine-readable, unique per error |
| User-facing text | Backend `message` displayed raw | Frontend maps `type` → localized, context-aware copy |
| Localization | Impossible without backend changes | Frontend-only — add a new locale file |
| Multiple clients | All clients show same text | Each client has its own `ERROR_MESSAGES` map |
| Field-level validation | `errors[]` exists but frontend ignores it | Frontend maps `errors[]` to inline field messages |
| Coupling | Backend text change = UI change | Backend and frontend teams work independently |
| Consistency | 4 different JSON shapes | One shape everywhere — handlers, filters, security |
| Standard compliance | Custom `ApiErrorResponse` record | IETF RFC 9457, Spring Boot 4 native `ProblemDetail` |

### Migration rollout (how we get there safely)

| Phase | What changes | Backward compatible? |
|---|---|---|
| **Phase 0** ✅ Done | Frontend reads `detail → message → error` fallback chain. Proxy accepts `application/problem+json`. | Yes — works with current backend |
| **Phase 1** (next) | Backend returns `ProblemDetail` with `type` URIs. Includes temporary `message`/`error` aliases. | Yes — frontend already reads both shapes |
| **Phase 3** (follow-up) | Frontend introduces `ERROR_MESSAGES` map keyed by `type` URI. Falls back to `detail`/`message` for unmapped types. | Yes — fallback handles any unmapped type |
| **Future** | Remove `message`/`error` backward-compat aliases. Frontend fully owns user-facing copy. | Breaking — only after all types are mapped |

---

## Current state audit

### Backend: 4 distinct error response shapes

| Shape | Source | Content-Type | Fields | Files |
|---|---|---|---|---|
| **A** — `ApiErrorResponse` | 7 `@RestControllerAdvice` classes (36 handlers) | `application/json` | `message`, `httpStatusCode`, `timestamp`, `errors[]` | `GlobalExceptionHandler`, `SignInExceptionHandler`, `JwtTokenExceptionsHandler`, `OrderExceptionHandler`, `PaymentExceptionHandler`, `CommonExceptionHandler`, `UserExceptionHandler` |
| **B** — `JwtAuthenticationFilter` JSON | Filter-level, bypasses `@ExceptionHandler` | `application/json` | `error`, `message`, `status`, `timestamp` (ISO), `requestId` | `JwtAuthenticationFilter.java:84-130` |
| **C** — `RateLimitResponseWriter` JSON | Filter-level, bypasses `@ExceptionHandler` | `application/json` | `error`, `message`, `status`, `timestamp` (ISO), `retryAfter` | `RateLimitResponseWriter.java:29-45` |
| **D** — Spring Security custom JSON | `authenticationEntryPoint` / `accessDeniedHandler` in `SpringSecurityConfiguration` | `application/json` | `error`, `message`, `status`, `timestamp`, `path` | `SpringSecurityConfiguration.java` |

**✅ FIXED (Phase 0):** `AccessDeniedHandler` now configured. Both entry point and access denied handler write proper JSON with `message` field.

Additionally, 4 locations return `ResponseEntity` with **no body at all**:
- `UserEndpoint.java:82` — 404 (no avatar)
- `GlobalExceptionHandler.java:166` — 406 (Not Acceptable)
- `GlobalExceptionHandler.java:174` — 405 (Method Not Allowed)
- `GlobalExceptionHandler` — `AsyncRequestNotUsableException` → 503 (void return, client disconnected)

**✅ FIXED (Phase 0):** `AuthEndpoint.java:72` — 503 (Google auth disabled) now returns `{message, status}` body.

### Backend: 29 custom exception classes + common base exceptions

All extend `RuntimeException`. Spread across: `cart` (4), `order` (4), `security` (8), `payment` (2), `filestorage` (2), `user` (2), `product` (1), `email` (2), `review` (1), `common` (4).

The 4 common base exceptions have `@ResponseStatus` and are used heavily (26 throw sites):
- `BadRequestException` (`@ResponseStatus(400)`) — 19 throw sites across `OrderCreator`, `CartEndpoint`, `CheckoutPaymentService`, `ProductReviewValidator`, `EmailVerificationService`, `PutUsersRequestValidator`, `GetProductsRequestValidator`, `GetReviewsRequestValidator`, `ProductsEndpoint`, `ProductReviewLikesUpdater`
- `NotFoundException` (`@ResponseStatus(404)`) — 1 throw site in `ProductReviewValidator`
- `UnauthorizedException` (`@ResponseStatus(401)`) — 1 throw site in `UserProfileService`
- `InternalServerErrorException` (`@ResponseStatus(500)`) — 0 throw sites found

### Backend: unhandled JDK exceptions (hit catch-all → 500)

14 `throw new IllegalStateException/IllegalArgumentException/SecurityException` sites have **no dedicated handler and no `@ResponseStatus`**, so they hit the catch-all and return 500:
- ~~`SecurityPrincipalProvider.java:29` — `"No authenticated UserEntity in security context"` (should be 401)~~ **✅ FIXED → throws `UnauthorizedException`**
- ~~`GoogleAuthCallbackHandler.java:39` — `"Google account has no email"` (should be 400)~~ **✅ FIXED → throws `BadRequestException`**
- ~~`GoogleTokenExchanger.java:48` — `"Google ID token verification failed"` (should be 401)~~ **✅ FIXED → throws `UnauthorizedException`**
- ~~`AwsObjectStorage.java:84` — `SecurityException("Invalid directory path")` (should be 400)~~ **✅ FIXED → throws `BadRequestException`**
- ~~`ProductReviewDtoConverter.java:61` — `"Unexpected product's rating value: X"` (should be 400)~~ **✅ FIXED → throws `BadRequestException`**
- `EmailVerificationService.java:104,112` — serialization failures (true 500, leave as-is)
- `StripeWebhookBusinessProcessor.java:229` — `"No orderId in Stripe session metadata"` (true 500, leave as-is)
- `SpringSecurityConfiguration.java:127,136,139` — startup config errors (not runtime, leave as-is)
- `RateLimitingFilter.java:63,67` — startup config errors (not runtime, leave as-is)
- `JwtBlacklistStore.java:62` — `"SHA-256 not available"` (true 500, leave as-is)
- `ShoppingCartService.java:120` — `"Cart not found after uniqueness conflict for userId=" + userId` — **⚠️ leaks userId, true 500 but message must be sanitized in Phase 1**

### Backend: remaining message sanitization risks

The catch-all handler uses `exception.getMessage()` for the response body (see above). Several exceptions still include internal identifiers in their messages:

| Exception | Leaks | Current status |
|---|---|---|
| `OrderNotFoundException(Object orderId)` | orderId UUID | All 6 call sites use this constructor |
| `UserNotFoundException(UUID userId)` | userId UUID | Used in non-auth contexts |
| `UserNotFoundException(String email)` | email address | Used in `SignInExceptionHandler` → 401 |
| `ShoppingCartNotFoundException(UUID userId)` | userId UUID | `@ResponseStatus(404)` but message leaks |
| `ShoppingCartService` `IllegalStateException` | userId UUID | Hits catch-all → 500 |

**Phase 1 must sanitize all of these.** For `ProblemDetailFactory`: never use `exception.getMessage()` for 5xx responses. For 4xx, use the exception message only if it has been explicitly sanitized (or use the `title` from the type URI catalog instead).

### Backend: `OrderExceptionHandler` scoping bug — ✅ FIXED

`OrderExceptionHandler` is scoped to `basePackages = {"com.zufar.icedlatte.order.endpoint"}`. But `PaymentStatusService.java` (in the `payment` package) throws `OrderNotFoundException` and `OrderAccessDeniedException`. These bypass `OrderExceptionHandler` and hit the catch-all.

**Fix applied:** Added `@ResponseStatus(NOT_FOUND)` to `OrderNotFoundException` and `@ResponseStatus(FORBIDDEN)` to `OrderAccessDeniedException`. The `GlobalExceptionHandler` catch-all resolves `@ResponseStatus` via `AnnotatedElementUtils.findMergedAnnotation()`, so these now return correct status codes regardless of which package throws them.

### Backend: `ReviewModerationException` never reaches a controller — ✅ DEFENSIVE FIX APPLIED

`ReviewModerationException` is only thrown in the async `@TransactionalEventListener` path (`AsyncReviewProcessingService`), where it's caught and handled (review deleted). It **never propagates to a controller**. The frontend's hardcoded 422 message was dead code — no 422 is ever returned for review moderation.

**Fix applied:** Added `@ResponseStatus(UNPROCESSABLE_ENTITY)` defensively. If this exception ever reaches a controller in the future, it will return 422 instead of 500.

### Backend: `UserNotFoundException` dual-status ambiguity

`UserNotFoundException` has `@ResponseStatus(NOT_FOUND)` (404) but is handled by `SignInExceptionHandler` as 401 for auth flows. `SignInExceptionHandler` passes `exception.getMessage()` through, which leaks email/UUID (e.g., "User with email = foo@bar.com is not found.").

**Note:** `UserExceptionHandler` handles `UsernameNotFoundException` (Spring Security's class), NOT `UserNotFoundException` (Iced Latte's class). The plan's error type URI catalog lists `UserNotFoundException` under `UserExceptionHandler` — this is wrong. It is only handled explicitly by `SignInExceptionHandler` (as 401). Outside auth flows, it falls to the catch-all which resolves `@ResponseStatus(NOT_FOUND)` → 404.

**Phase 1 must:**
1. Sanitize the message — remove email/UUID from `UserNotFoundException` constructors
2. Decide: keep one exception with context-dependent status, or split into `AuthUserNotFoundException` (401) vs `UserNotFoundException` (404)

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

### Frontend: error handling (`apiError.ts`) — ✅ FIXED

```typescript
// src/shared/utils/apiError.ts — THE critical function (CURRENT STATE after Phase 0 fixes)
export const handleAxiosError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ErrorResponse>
    if (axiosError.response) {
      const { status, data } = axiosError.response
      if (status === 401) return data?.detail || data?.message || 'Please sign in to continue.'
      if (status === 403) return data?.detail || data?.message || 'You do not have permission to perform this action.'
      return data?.detail || data?.message || data?.error || 'An unknown error occurred'
    }
    return 'Network error. Please check your connection.'
  }
  return 'An unknown error occurred'
}
```

**Fields read at runtime:** `response.status`, `data.detail` (RFC 9457 ready), `data.message`, `data.error` (fallback).
**Fields declared in `ErrorResponse` type but never read:** `type`, `title`, `instance`, `httpStatusCode`, `timestamp`, `errors[]`.

### Frontend: Next.js proxy layer — ✅ FIXED

The proxy at `src/app/api/proxy/[...path]/route.ts` (line 175) now parses both content types:
```typescript
(contentType.includes('application/json') || contentType.includes('application/problem+json')) && rawBody
  ? JSON.parse(rawBody) : rawBody
```

This is backward-compatible and ready for ProblemDetail responses.

The proxy also emits synthetic errors with shape `{ error: 'API unavailable' }` (503) and `{ error: 'Invalid path' }` (400) for proxy-level failures — these are not ProblemDetail and will continue to work (frontend reads `data.error` as fallback).

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
| `handleError()` → `errorMessage` state → rendered | `ImageUpload` | ✅ FIXED — now renders error below avatar |
| `handleError()` → `errorMessage` state → **never rendered** | `ReviewsList`, `UserBar`, `ProductWithReviews` | ❌ Error swallowed — user sees nothing (fix in Phase 3.2) |
| Hardcoded error strings | `OrderCard.tsx` (`'Could not cancel order.'`, etc.) | Backend message discarded (UX choice, not a bug) |
| `setCartError()` → `lastError` in store | `cart.mutations.ts`, `cart.sync.ts` | ✅ FIXED — now reads backend `data.message` via `handleAxiosError` |
| `error` state in address store | `addresses/store.ts` | ✅ FIXED — all mutations now have try/catch with `handleAxiosError` |
| `.catch(() => {})` — silently swallowed | `useSearchBar.ts`, `ProfileScreen.tsx`, `session.ts`, `favorites.mutations.ts`, `favorites.sync.ts`, `useLogout.ts` | ⚠️ Intentional for most (optimistic rollback, logout, session bootstrap) |
| `setError(getCheckoutUnavailableMessage())` | `useCheckoutForm.ts` | Hardcoded checkout error (intentional — don't expose Stripe internals) |
| `AuthInterceptor` → refresh → redirect | 401 handling | ✅ Functional — only checks status code, never reads body |

### Frontend: `ErrorResponse` TypeScript type — ✅ UPDATED

```typescript
// src/shared/types/ErrorResponse.ts (CURRENT STATE)
export type ErrorResponse = {
  // RFC 9457 ProblemDetail fields
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  // Current backend fields
  message?: string
  error?: string
  // Extensions
  timestamp?: string
  errors?: Array<{ field: string; message: string }>
}
```

All fields optional — safe for both current `ApiErrorResponse` and future `ProblemDetail` shapes.

### Frontend: cart error handling — ✅ FIXED

```typescript
// src/features/cart/utils/cartStoreHelpers.ts (CURRENT STATE)
export function setCartError(set: StoreSet, fallbackMessage: string, err: unknown): void {
  const message = handleAxiosError(err)
  set({
    lastError: message === 'An unknown error occurred' ? fallbackMessage : message,
    status: 'error',
  })
}
```

Previously read `err.message` (Axios's "Request failed with status code 400"). Now extracts the backend's actual message via `handleAxiosError`.

**Note:** `lastError` is set in the cart store but **never rendered in any component**. This is a remaining gap (G6 in the audit) — the cart store has error state but no UI displays it. Fix in Phase 3 or via toast notifications.

---

## Inconsistencies and gaps found

### Critical (will break things or already broken)

| # | Issue | Backend | Frontend | Status |
|---|---|---|---|---|
| **C1** | ~~403 shows "Incorrect email or password"~~ | `OrderAccessDeniedException` → 403, `SessionOwnershipException` → 403 | `apiError.ts` now returns `data?.message` for 403 | **✅ FIXED** |
| **C2** | ~~422 hardcoded to review rejection~~ | `ReviewModerationException` is async-only — 422 is **never returned** to the client | Hardcoded 422 message removed | **✅ FIXED** |
| **C3** | ~~No `AccessDeniedHandler`~~ | Added `accessDeniedHandler` in `SpringSecurityConfiguration` | Frontend reads `data?.message` | **✅ FIXED** |
| **C4** | OpenAPI `ErrorResponse` ≠ actual `ApiErrorResponse` | `httpStatusCode` vs `status`, `errors[{field,message}]` vs `details[string]`, timestamp format mismatch, `path` never sent | Any generated client would break | ⏳ Fix in Phase 1.7 |
| **C5** | ~~Proxy blocks `application/problem+json`~~ | ProblemDetail responses use `application/problem+json` content type | Proxy now matches both `application/json` and `application/problem+json` | **✅ FIXED** |
| **C6** | ~~`OrderExceptionHandler` scoping bug~~ | Added `@ResponseStatus` to both exceptions | Frontend now sees correct 404/403 | **✅ FIXED** |

### High (information leakage or lost functionality)

| # | Issue | Details | Status |
|---|---|---|---|
| **H1** | `errors[]` validation array never read by frontend | Backend sends `[{field: "email", message: "must not be blank"}]` — user sees only "Validation failed" with no field details | ⏳ Phase 3.4 |
| **H2** | `retryAfter` from rate limiter ignored | No countdown/backoff UI | ⏳ Out of scope |
| **H3** | `requestId` from JWT filter ignored | No log correlation for support | ⏳ Out of scope |
| **H4** | ~~5 components call `handleError()` but never render `errorMessage`~~ | `ImageUpload` and `ReviewsSection` fixed. Remaining: `ReviewsList`, `UserBar`, `ProductWithReviews` | **2/5 FIXED**, rest ⏳ Phase 3.2 |
| **H5** | ~~Cart errors silently swallowed~~ | `setCartError` now extracts backend message via `handleAxiosError` | **✅ FIXED** (but `lastError` still not rendered in UI) |
| **H6** | ~~14 JDK exception throw sites → 500~~ | 5 runtime throw sites fixed (replaced with proper exceptions). 9 remaining are startup-only or true 500s. | **✅ FIXED** |
| **H7** | ~~Empty-body error response (AuthEndpoint 503)~~ | Now returns `{message, status}` body | **✅ FIXED** |
| **H8** | No frontend error boundaries | Only `/product/[id]` has an error boundary — parsing failures during migration crash pages with no recovery | ⏳ Out of scope |
| **H9** | No frontend error logging | No Sentry/DataDog — cannot detect if new response shape causes silent failures in production | ⏳ Out of scope |
| **H10** | `UserNotFoundException` dual-status | Handled as 401 everywhere, but semantically wrong for non-auth contexts (admin lookup → should be 404) | ⏳ Phase 1 decision |

### Medium (inconsistency, not broken)

| # | Issue | Details | Status |
|---|---|---|---|
| **M1** | 4 different JSON shapes from backend | Handlers (shape A), JWT filter (shape B), rate limiter (shape C), Spring Security (shape D) — all include `message` field now | ⏳ Unify in Phase 1 |
| **M2** | Timestamp format inconsistency | Handlers: `"yyyy-MM-dd HH:mm:ss"`, Filters: ISO-8601, OpenAPI: `date-time` | ⏳ Standardize in Phase 1 |
| **M3** | Status field name inconsistency | Handlers: `httpStatusCode`, Filters: `status`, OpenAPI: `status` | ⏳ Standardize in Phase 1 |
| **M4** | `OrderCard.tsx` discards backend error messages | Uses hardcoded strings like "Could not cancel order." — loses context like "Cancellation window expired" | ⏳ UX decision (Phase 3) |
| **M5** | Handler ordering fragility | `CommonExceptionHandler` and `GlobalExceptionHandler` both have no `@Order` — ambiguous resolution | ⏳ Phase 1 |
| **M6** | `react-toastify` installed but never used | `<ToastContainer />` mounted in `layout.tsx`, `toast()` never called — dead code, missed opportunity for non-form errors | ⏳ Out of scope |
| **M7** | ~~`ReviewModerationException` has no `@ResponseStatus`~~ | Added `@ResponseStatus(UNPROCESSABLE_ENTITY)` defensively | **✅ FIXED** |
| **M8** | `FileUploadException` handler HTTP status/body mismatch | `@ResponseStatus(500)` but 503 branch in body — HTTP status and body `httpStatusCode` disagreed | **✅ FIXED** — now uses `ResponseEntity` |

---

## End-to-end error flow reference (current state after Phase 0)

This section documents the exact path each error takes from backend to user, for implementation reference.

### Error pipeline architecture

```
Backend Exception
  → @ExceptionHandler (or filter-level writer)
    → HTTP Response {status, Content-Type: application/json, body: JSON}
      → Next.js Proxy (src/app/api/proxy/[...path]/route.ts)
        → Preserves status code, parses JSON body, forwards to client
          → Axios response interceptor (AuthInterceptor.tsx)
            → If 401: attempt refresh, redirect on failure
            → Otherwise: pass through to component
              → Component catch block
                → handleAxiosError(err) extracts message
                  → Displayed to user (or swallowed)
```

### Backend handler → response body mapping

| Handler class | Exception | HTTP Status | Body `message` field value |
|---|---|---|---|
| `SignInExceptionHandler` | `BadCredentialsException` | 401 | "The login credentials are invalid." |
| `SignInExceptionHandler` | `UserNotFoundException` | 401 | "The user with email '{email}' is not found." |
| `SignInExceptionHandler` | `UserRegistrationException` | 400 | "User with email '{email}' already exists." |
| `SignInExceptionHandler` | `UserAccountLockedException` | 401 | "User account is locked." |
| `UserExceptionHandler` | `UsernameNotFoundException` | 401 | "User not found" |
| `SignInExceptionHandler` | `UserNotFoundException` (auth flows) | 401 | `exception.getMessage()` — **⚠️ leaks UUID/email until Phase 1 sanitization** |
| `UserExceptionHandler` | `InvalidAvatarFileTypeException` | 400 | "Invalid avatar file type: {type}. Allowed: {types}" |
| `OrderExceptionHandler` | `OrderNotFoundException` | 404 | "Order not found." (safe) or "Order not found: {orderId}" — **⚠️ `Object orderId` constructor still leaks orderId in all 6 call sites; remove or sanitize before Phase 1** |
| `OrderExceptionHandler` | `OrderAccessDeniedException` | 403 | "Access denied." |
| `OrderExceptionHandler` | `InvalidOrderStateTransitionException` | 409 | "Cannot transition order from {from} to {to}." |
| `OrderExceptionHandler` | `OrderCancellationWindowExpiredException` | 409 | "Order cannot be cancelled: cancellation window has expired." |
| `PaymentExceptionHandler` | `StripeSessionCreationException` | 502 | "Failed to create Stripe checkout session." |
| `PaymentExceptionHandler` | `PaymentEventProcessingException` | 400 | varies |
| `CommonExceptionHandler` | `FileReadException` | 400 | "Failed to read file: {name}" |
| `CommonExceptionHandler` | `FileUploadException` | 500/503 | "Failed to upload file: {name}" / "File storage is not available" |
| `GlobalExceptionHandler` | `MethodArgumentNotValidException` | 400 | "Validation failed" + `errors[]` array |
| `GlobalExceptionHandler` | `ConstraintViolationException` | 400 | "Validation failed" + `errors[]` array |
| `GlobalExceptionHandler` | `MaxUploadSizeExceededException` | 413 | "Maximum upload size exceeded" |
| `GlobalExceptionHandler` | `NoResourceFoundException` | 404 | "No resource found for {method} {path}" |
| `GlobalExceptionHandler` | Catch-all (with `@ResponseStatus`) | varies | `exception.getMessage()` — **⚠️ LEAKS internal details** |
| `GlobalExceptionHandler` | Catch-all (without `@ResponseStatus`) | 500 | `exception.getMessage()` — **⚠️ LEAKS internal details** |

**⚠️ CRITICAL for Phase 1:** The catch-all handler calls `apiErrorResponseCreator.buildResponse(exception, status)` which uses `exception.getMessage()` as the response body. This means ANY unhandled exception's raw message is exposed to the client. `ProblemDetailFactory` MUST sanitize 5xx responses: use `"An internal server error occurred."` for all 500s without `@ResponseStatus`, and never pass `exception.getMessage()` through for server errors. Log the real exception server-side only.

### Filter-level → response body mapping

| Source | HTTP Status | Body shape | Key fields |
|---|---|---|---|
| `JwtAuthenticationFilter` | 401/500 | `{error, message, status, timestamp, requestId}` | `message`: "Authentication failed: {reason}" |
| `RateLimitResponseWriter` | 429 | `{error, message, status, timestamp, retryAfter}` | `message`: "Too many requests. Please try again later." |
| `SpringSecurityConfiguration` entryPoint | 401 | `{error, message, status, timestamp, path}` | `message`: "Unauthorized" |
| `SpringSecurityConfiguration` accessDenied | 403 | `{error, message, status, timestamp, path}` | `message`: "Access denied" |

### Frontend component → error display mapping

| Component | Error source | Uses `handleAxiosError`? | Displays to user? | What user sees |
|---|---|---|---|---|
| `LoginForm` | `/auth/authenticate` 401 | Yes | Yes (inline) | "The login credentials are invalid." |
| `RegistrationForm` | `/auth/register` 400/409 | Yes | Yes (inline) | Backend message |
| `FormProfile` | `/users` 400 | Yes | Yes (inline) | "Validation failed" (no field details) |
| `ReviewForm` | `/reviews` 400 | Yes | Yes (inline) | Backend message |
| `OrderCard` | `/orders/*` 403/404/409 | **No** | Yes (hardcoded) | "Could not cancel order." etc. |
| `ImageUpload` | `/users/avatar` 400/413/500 | Yes | **Yes (fixed)** | Backend message |
| `CartPage` (via store) | `/cart/*` 400/404 | Yes (via `setCartError`) | **No** (`lastError` not rendered) | Nothing visible |
| `AddressForm` (via store) | `/users/addresses` 400 | Yes | Yes (via store `error`) | Backend message |
| `CheckoutForm` | `/payment/checkout` 502 | **No** | Yes (hardcoded) | Config message |
| `ProductPage` | `/products/{id}` 404 | **No** (checks status) | Yes (Next.js 404) | Not Found page |
| `AuthInterceptor` | Any 401 | **No** (only checks status) | Redirect to `/signin` | Sign-in page |

### Key implementation notes for Phase 1

1. **`message` field MUST be present during the compatibility window** — it's the primary field the frontend reads. Set it equal to `detail`. Once the frontend maps `type` URIs to user-facing copy (Phase 3 follow-up), this alias can be removed.
2. **`error` field SHOULD be present during the compatibility window** — it's the fallback for Spring Boot's `BasicErrorController` responses and proxy-level errors. Set it equal to `title`. Remove alongside `message`.
3. **Content-Type should be `application/problem+json`** for controller-level `ProblemDetail` responses (proper RFC 9457). The proxy supports it. Filter-level responses may initially keep `application/json`.
4. **`errors[]` array must keep shape `[{field, message}]`** — even though frontend doesn't render it yet, the type is declared and Phase 3.4 will use it.
5. **Filter-level responses** (JWT, rate limiter, security config) should adopt the same ProblemDetail shape but can keep `application/json` content type since they write directly to `HttpServletResponse`.
6. **The `httpStatusCode` field can be dropped** — no frontend code reads it (verified by grep). Replace with `status` (ProblemDetail standard).
7. **Timestamp format should standardize to ISO-8601** — filters already use it, handlers use `"yyyy-MM-dd HH:mm:ss"`. No frontend code parses timestamps.
8. **⚠️ 5xx responses MUST NOT use `exception.getMessage()`** — the current catch-all leaks internal details. `ProblemDetailFactory` must return `"An internal server error occurred."` for all unhandled 500s. Log the real exception server-side only.
9. **4xx exception messages must be sanitized** — several exceptions still include UUIDs/emails in their messages (see "remaining message sanitization risks" section). Either sanitize the constructors or have `ProblemDetailFactory` use the `title` from the type URI catalog instead of `exception.getMessage()`.
10. **Frontend `type` → user message mapping** is the real final goal. During Phase 1, the frontend continues to read `detail`/`message`. A follow-up Phase 3 step should introduce `ERROR_MESSAGE_BY_TYPE` mapping so the frontend owns user-facing copy and the backend owns machine-readable error codes.

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

**Important:** The frontend proxy now supports `application/problem+json` (Phase 0.2 fix). Controller-level `@ExceptionHandler` methods should return `ProblemDetail` with `application/problem+json` content type (proper RFC 9457). Filter-level writers may use `application/json` initially. During the compatibility window, include `message` and `error` as extension properties so the frontend works with both old and new shapes.

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
| `UserNotFoundException` | `user-not-found` | 404 (via `@ResponseStatus`) or 401 (via `SignInExceptionHandler`) — **decision needed: split or unify** | User not found |
| `InvalidAvatarFileTypeException` | `invalid-avatar-type` | 400 | Invalid file type |

**Note:** `UserExceptionHandler` handles `UsernameNotFoundException` (Spring Security), not `UserNotFoundException`. `UserNotFoundException` is only explicitly handled by `SignInExceptionHandler` (as 401). Outside auth flows, it falls to the catch-all which resolves `@ResponseStatus(NOT_FOUND)` → 404.

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
| `BadRequestException` (common) | `bad-request` | 400 | Bad request |
| `NotFoundException` (common) | `not-found` | 404 | Not found |
| `UnauthorizedException` (common) | `unauthorized` | 401 | Unauthorized |
| `InternalServerErrorException` (common) | `internal-error` | 500 | Internal server error |
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

### Phase 0: Pre-migration fixes (backend + frontend, separate PRs) — ✅ COMPLETED

All Phase 0 fixes have been implemented and verified (backend: 650 tests pass, frontend: 160 tests pass).

#### Step 0.1: Fix `OrderExceptionHandler` scoping bug — ✅ DONE

Added `@ResponseStatus(NOT_FOUND)` to `OrderNotFoundException` and `@ResponseStatus(FORBIDDEN)` to `OrderAccessDeniedException`. Also changed exception messages to not leak UUIDs.

#### Step 0.2: Fix frontend proxy content-type check — ✅ DONE

In `src/app/api/proxy/[...path]/route.ts` line 175:
```typescript
(contentType.includes('application/json') || contentType.includes('application/problem+json')) && rawBody
```

Test added to verify `application/problem+json` parsing.

#### Step 0.3: Deploy frontend `apiError.ts` fix — ✅ DONE

The updated `handleAxiosError` reads `data?.detail || data?.message` — backward-compatible with current backend, forward-compatible with ProblemDetail.

Additional fixes applied:
- **Network error message:** Axios errors without response now return "Network error. Please check your connection." instead of "An unknown error occurred"
- **Cart error helper:** `setCartError` now uses `handleAxiosError` to extract backend message instead of Axios's generic `Error.message`
- **Address store:** Added try/catch to `add/update/remove/setDefault` — errors extracted via `handleAxiosError` and stored in `error` state
- **ImageUpload:** Now renders `errorMessage` so upload failures are visible

#### Step 0.4: Add `@ResponseStatus` to unhandled exceptions — ✅ DONE

- `OrderNotFoundException` → `@ResponseStatus(NOT_FOUND)`
- `OrderAccessDeniedException` → `@ResponseStatus(FORBIDDEN)`
- `ReviewModerationException` → `@ResponseStatus(UNPROCESSABLE_ENTITY)` (defensive)

#### Step 0.5: Fix JDK exception throw sites — ✅ DONE

Replaced 5 runtime `IllegalStateException`/`IllegalArgumentException`/`SecurityException` throws with proper typed exceptions:
- `SecurityPrincipalProvider` → `UnauthorizedException("Authentication required.")`
- `GoogleAuthCallbackHandler` → `BadRequestException("Google account has no email.")`
- `GoogleTokenExchanger` → `UnauthorizedException("Google authentication failed.")`
- `AwsObjectStorage` → `BadRequestException("Invalid directory path.")`
- `ProductReviewDtoConverter` → `BadRequestException("Invalid product rating value.")`

#### Step 0.6: Fix Spring Security error responses — ✅ DONE

- `authenticationEntryPoint` now writes proper JSON `{error, message, status, timestamp, path}` (was `sendError` with no body)
- Added `accessDeniedHandler` writing same shape for 403

#### Step 0.7: Fix empty-body 503 response — ✅ DONE

`AuthEndpoint.initiateGoogleAuth()` now returns `{message: "Google authentication is not available.", status: 503}` instead of empty body.

#### Step 0.8: Fix `FileUploadException` handler status mismatch — ✅ DONE

Changed from `@ResponseStatus(INTERNAL_SERVER_ERROR)` with conditional 503 body to `ResponseEntity` return type so each branch sets its own correct HTTP status (500 for generic failure, 503 for storage unavailable).

#### Step 0.9: Add temporary `console.error` logging in frontend — ⏳ TODO (optional)

Add `console.error('[API Error]', status, data)` in `handleAxiosError` during the migration window. Remove after confirming stable.

### Phase 1: Backend — unified ProblemDetail responses

#### Step 1.1: Create `ProblemDetailFactory`

New file: `common/exception/handler/ProblemDetailFactory.java`

Replaces `ApiErrorResponseCreator`. Methods:
- `build(String typeSlug, String title, HttpStatus status, String detail)` → `ProblemDetail`
- `build(String typeSlug, String title, HttpStatus status, String detail, List<FieldError> errors)` → `ProblemDetail`
- Auto-sets `timestamp` (ISO-8601), `message` (= `detail`), `error` (= `title`) as extension properties
- `instance` set from `RequestContextHolder` if available
- **For 5xx status codes:** `detail`/`message` MUST be a safe generic string (e.g., "An internal server error occurred."), never `exception.getMessage()`. The real exception is logged server-side only.

Constant: `TYPE_BASE = "https://iced-latte.uk/errors/"`.

**Content-Type decision:** The frontend proxy now supports both `application/json` and `application/problem+json` (Phase 0.2). Prefer `application/problem+json` for controller-level `ProblemDetail` responses (proper RFC 9457 compliance). Filter-level responses may initially keep `application/json` if direct servlet writing makes migration simpler, but should use the same body shape. During the compatibility window, include `message` (= `detail`) and `error` (= `title`) as extension properties; remove these aliases once the frontend maps `type` URIs to user-facing copy.

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

**`SpringSecurityConfiguration` `authenticationEntryPoint` and `accessDeniedHandler`**: Already write proper JSON with `{error, message, status, timestamp, path}` (Phase 0.6). During Phase 1, update the shape to include ProblemDetail fields (`type`, `title`, `detail`, `instance`). The `message` field is already present — just add the new fields alongside it.

#### Step 1.4: Convert empty-body `ResponseEntity` returns

- `UserEndpoint.java:82` — return ProblemDetail body with type `about:blank`, detail "User has no avatar"
- ~~`AuthEndpoint.java:72`~~ — **✅ ALREADY FIXED** (Phase 0.7) — returns `{message, status}`. During Phase 1, upgrade to full ProblemDetail shape.
- `GlobalExceptionHandler` 405/406 — return ProblemDetail body instead of `ResponseEntity<Void>`
- `AsyncRequestNotUsableException` — leave as void (client already disconnected)

#### Step 1.5: ~~Add handlers for JDK exceptions that should not be 500~~ — ✅ ALREADY DONE (Phase 0.5)

The 5 runtime throw sites have been replaced with proper typed exceptions (`UnauthorizedException`, `BadRequestException`). No further action needed.

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
- `CommonExceptionHandlerTest.java` (11 refs)
- `OrderExceptionHandlerTest.java` (6 refs)

#### Step 2.3: Update integration tests

Any REST Assured / MockMvc tests asserting on `message` / `httpStatusCode` → assert on `detail` / `status` (or `message` alias).

#### Step 2.4: Run full suite

`mvn test` — all 649+ tests pass.

### Phase 3: Frontend fixes (separate PR, can be done in parallel)

#### Step 3.1: Fix `apiError.ts` — ✅ ALREADY DONE (Phase 0.3)

Current state:
```typescript
export const handleAxiosError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ErrorResponse>
    if (axiosError.response) {
      const { status, data } = axiosError.response
      if (status === 401) return data?.detail || data?.message || 'Please sign in to continue.'
      if (status === 403) return data?.detail || data?.message || 'You do not have permission to perform this action.'
      return data?.detail || data?.message || data?.error || 'An unknown error occurred'
    }
    return 'Network error. Please check your connection.'
  }
  return 'An unknown error occurred'
}
```

This reads `detail` first (ProblemDetail), falls back to `message` (current format), then `error` (Spring defaults). Works with both old and new response shapes during rolling deployment.

#### Step 3.2: Fix components that call `handleError()` but never render `errorMessage`

Add `{errorMessage && <p className="text-negative text-sm" role="alert">{errorMessage}</p>}` to:
- ~~`ImageUpload.tsx`~~ **✅ DONE**
- ~~`ReviewsSection.tsx`~~ **✅ Already renders errorMessage**
- `ReviewsList.tsx`
- `UserBar.tsx`
- `ProductWithReviews.tsx`

#### Step 3.3: Update `ErrorResponse` type — ✅ ALREADY DONE (Phase 0.3)

Current state includes all RFC 9457 fields plus backward-compatible aliases, all optional.

#### Step 3.4: (Optional) Read `errors[]` for validation

In form components (`LoginForm`, `RegistrationForm`, `FormProfile`), map server-side `errors[]` to field-level error display. This is a larger UX improvement — can be a follow-up.

#### Step 3.5: (Optional) Render cart `lastError` in UI

The cart store's `lastError` is now correctly populated with backend messages, but no component renders it. Options:
- Add inline error display in cart page
- Use `react-toastify` `toast.error(lastError)` when it changes
- Both

#### Step 3.6: (Optional) Activate `react-toastify` for non-form errors

`<ToastContainer />` is already mounted in `layout.tsx`. Components that handle errors in background (favorites sync, cart sync, address operations) could call `toast.error(message)` instead of/in addition to setting state.

---

## Files changed (estimated)

| Action | Files | Count |
|---|---|---|
| **Create** | `ProblemDetailFactory.java`, `ProblemDetailFactoryTest.java` | 2 |
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

| Risk | Impact | Mitigation | Status |
|---|---|---|---|
| **Proxy blocks `application/problem+json`** | Every error becomes "An unknown error occurred" | **Phase 0.2**: fix proxy content-type check BEFORE backend migration. Now supports both content types. | **✅ MITIGATED** |
| Frontend reads `data.message` | Every error shows fallback if `message` missing | Include `message` as extension property (duplicating `detail`) — **P0** | Design decision for Phase 1 |
| Frontend reads `data.error` as fallback | Fallback breaks if `error` missing | Include `error` as extension property (duplicating `title`) — **P0** | Design decision for Phase 1 |
| No frontend error boundaries | Parsing failure during migration crashes most pages | Deploy frontend changes (Phase 0.3) BEFORE backend migration | **✅ MITIGATED** (frontend already handles both shapes) |
| No frontend error logging | Cannot detect silent failures in production | Add temporary `console.error` logging (Phase 0.9) | ⏳ Optional |
| `OrderExceptionHandler` scoping bug | 500 instead of 404/403 for payment-thrown order exceptions | Fix in Phase 0.1 before migration | **✅ MITIGATED** |
| JDK exceptions → 500 | `IllegalStateException` etc. leak internal messages | Add handlers or replace with custom exceptions (Step 0.5) | **✅ MITIGATED** |
| Filter-level responses not migrated | 4 different JSON shapes persist | Migrate filters in same PR — Step 1.3 | ⏳ Phase 1 |
| Rolling deployment window | Some backend instances serve old format, some new | Frontend reads `data.detail \|\| data.message` — works with both | **✅ MITIGATED** |
| External QA repo integration tests | May assert on old field names | `message` alias ensures backward compat. Coordinate with QA. | ⏳ Phase 1 |
| `spring.mvc.problemdetails.enabled=true` | Changes actuator error responses, may break monitoring | Do NOT enable this flag. Handle ProblemDetail manually in handlers. | Design decision |
| OpenAPI-generated clients | Currently broken anyway (field name mismatches) | Fix is net positive | ⏳ Phase 1.7 |
| Cart `lastError` never rendered | Users get no feedback on cart operation failures | `setCartError` now extracts correct message; rendering is Phase 3.5 | **Partially mitigated** |
| Address store unhandled rejections | Crash risk, no user feedback | Added try/catch with `handleAxiosError` | **✅ MITIGATED** |

---

## Sequencing (deploy order matters)

1. ~~**Frontend PR 0** (Phase 0.2, 0.3): Fix proxy content-type check, update `apiError.ts` to read `detail || message`.~~ **✅ DONE & VERIFIED**
2. ~~**Backend PR 0** (Phase 0.1, 0.4, 0.5, 0.6, 0.7, 0.8): Fix `OrderExceptionHandler` scoping, add `@ResponseStatus`, fix JDK exceptions, fix security handlers, fix empty-body 503, fix FileUploadException status mismatch.~~ **✅ DONE & VERIFIED**
3. **Backend PR 1** (Phase 1 + 2): Full ProblemDetail migration — handlers, filters, tests, OpenAPI. **Deploy next** — frontend already handles new shape.
4. **Frontend PR 1** (Phase 3.2, 3.5): Fix unrendered `errorMessage` components, render cart `lastError`. **Deploy after backend PR 1.**
5. **Frontend PR 2** (Phase 3.4, 3.6, follow-up): Validation `errors[]` display, activate toast for non-form errors.
6. **Frontend PR 3** (Phase 3 follow-up): Introduce `ERROR_MESSAGES` map — frontend maps `type` URIs to user-facing copy. This is the real BE/FE contract.
7. **Future**: Remove `message`/`error` backward-compat aliases once frontend fully maps all `type` URIs.

---

## Phase 0 — files modified (for reference)

### Backend (`/Users/zufar/IdeaProjects/Iced-Latte`)

| File | Change |
|---|---|
| `src/main/java/.../order/exception/OrderNotFoundException.java` | Added `@ResponseStatus(NOT_FOUND)`; no-arg message is safe; `Object orderId` constructor still leaks and must be sanitized in Phase 1 |
| `src/main/java/.../order/exception/OrderAccessDeniedException.java` | Added `@ResponseStatus(FORBIDDEN)`, safe message |
| `src/main/java/.../security/configuration/SpringSecurityConfiguration.java` | Rewrote `authenticationEntryPoint` + added `accessDeniedHandler` with proper JSON |
| `src/main/java/.../security/api/SecurityPrincipalProvider.java` | `IllegalStateException` → `UnauthorizedException` |
| `src/main/java/.../auth/api/GoogleAuthCallbackHandler.java` | `IllegalStateException` → `BadRequestException` |
| `src/main/java/.../auth/api/GoogleTokenExchanger.java` | `IllegalStateException` → `UnauthorizedException` |
| `src/main/java/.../filestorage/aws/AwsObjectStorage.java` | `SecurityException` → `BadRequestException` |
| `src/main/java/.../review/converter/ProductReviewDtoConverter.java` | `IllegalArgumentException` → `BadRequestException` |
| `src/main/java/.../auth/endpoint/AuthEndpoint.java` | 503 now returns `{message, status}` body |
| `src/main/java/.../review/ai/ReviewModerationException.java` | Added `@ResponseStatus(UNPROCESSABLE_ENTITY)` |
| `src/main/java/.../filestorage/exception/CommonExceptionHandler.java` | Changed to `ResponseEntity` return for correct status per branch |
| `src/test/java/.../security/api/SecurityPrincipalProviderTest.java` | Updated assertions |
| `src/test/java/.../auth/api/GoogleAuthCallbackHandlerTest.java` | Updated assertions |
| `src/test/java/.../auth/api/GoogleTokenExchangerTest.java` | Updated assertions |
| `src/test/java/.../filestorage/exception/CommonExceptionHandlerTest.java` | Updated for `ResponseEntity` + added 503 test |

### Frontend (`/Users/zufar/IdeaProjects/Iced-Latte-Frontend`)

| File | Change |
|---|---|
| `src/app/api/proxy/[...path]/route.ts` | Added `application/problem+json` to content-type check |
| `src/app/api/proxy/[...path]/route.test.ts` | Added test for `application/problem+json` parsing |
| `src/shared/utils/apiError.ts` | Complete rewrite: reads `detail` first, proper 401/403 messages, network error message |
| `src/shared/utils/apiError.test.ts` | 9 tests covering all branches |
| `src/shared/types/ErrorResponse.ts` | All fields optional, added RFC 9457 fields |
| `src/features/addresses/store.ts` | Added try/catch to all mutations with `handleAxiosError` |
| `src/features/user/components/ImageUpload.tsx` | Now renders `errorMessage` |
| `src/features/cart/utils/cartStoreHelpers.ts` | `setCartError` uses `handleAxiosError` instead of `err.message` |

### Test results after Phase 0

- **Backend:** 650 tests, 0 failures, 0 errors
- **Frontend:** 32 suites, 160 tests, all passing

---

## Out of scope

- Error type URI documentation pages (each `type` URI resolving to a docs page)
- i18n / localization
- Frontend `errors[]` field-level validation display (Phase 3.4 — follow-up)
- `retryAfter` countdown UI for 429 (G1)
- Cart `lastError` rendering in UI (Phase 3.5 — follow-up)
- Activating `react-toastify` for non-form errors (Phase 3.6 — follow-up)
- Adding `global-error.tsx` / root `error.tsx` error boundaries (follow-up)
- Adding Sentry/error tracking to frontend (follow-up)
- Consolidating 7 handler classes into 2–3 (nice-to-have, reduces ordering fragility)
- Splitting `UserNotFoundException` into auth vs non-auth variants (nice-to-have)
- `spring.mvc.problemdetails.enabled=true` (too broad — affects actuator, `/error` endpoint)
- `OrderCard.tsx` using backend messages instead of hardcoded strings (UX decision)
- Google OAuth callback error display (G4 — `src/app/auth/google/callback/page.tsx`)
- Rendering errors in `ReviewsList`, `UserBar`, `ProductWithReviews` (Phase 3.2)
- Frontend `type` URI → user-facing message mapping (Phase 3 follow-up — the real BE/FE contract)
- HTTP 409 Conflict specific UX (G2)
- HTTP 502 Bad Gateway specific UX (G3)
