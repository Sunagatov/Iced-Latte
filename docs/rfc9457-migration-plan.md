# RFC 9457 ProblemDetail Migration Plan

## Goal

Replace the custom `ApiErrorResponse` with Spring Boot 4's built-in `ProblemDetail` (RFC 9457) so every error response follows the internet standard. This gives the frontend a stable `type` URI to switch on instead of parsing human-readable `message` strings, and makes the API self-documenting for any consumer.

## What RFC 9457 looks like

Before (current):
```json
{
  "message": "Shopping cart not found.",
  "httpStatusCode": 404,
  "timestamp": "2026-05-04 20:00:00"
}
```

After:
```json
{
  "type": "https://iced-latte.uk/errors/cart-not-found",
  "title": "Shopping cart not found",
  "status": 404,
  "detail": "Shopping cart not found.",
  "instance": "/api/v1/cart",
  "timestamp": "2026-05-04T20:00:00Z",
  "errors": [...]
}
```

The `type` field is the machine-readable error code (a stable URI). The `title` is a short human-readable label. The `detail` is the developer-facing explanation. The `timestamp` and `errors` are custom extension properties.

## Current state

| Component | Count | Notes |
|---|---|---|
| `ApiErrorResponse` record | 1 | `common/exception/dto/ApiErrorResponse.java` — has `message`, `httpStatusCode`, `timestamp`, `errors` |
| `ApiErrorResponseCreator` service | 1 | Factory that builds `ApiErrorResponse` from exception + status |
| Exception handler classes | 7 | `GlobalExceptionHandler`, `SignInExceptionHandler`, `JwtTokenExceptionsHandler`, `OrderExceptionHandler`, `PaymentExceptionHandler`, `CommonExceptionHandler` (filestorage), `UserExceptionHandler` |
| `@ExceptionHandler` methods | 36 | Across all 7 handler classes |
| Custom exception classes | 29 | Across all feature packages |
| Test files referencing `ApiErrorResponse` | 7 | 113 total references |
| Spring Boot version | 4.0.5 | `ProblemDetail` fully supported natively |

## Design decisions

### 1. Error type URI scheme

Format: `https://iced-latte.uk/errors/{error-slug}`

Each custom exception maps to one slug. Examples:

| Exception | `type` URI |
|---|---|
| `ShoppingCartNotFoundException` | `https://iced-latte.uk/errors/cart-not-found` |
| `ProductNotFoundException` | `https://iced-latte.uk/errors/product-not-found` |
| `OrderNotFoundException` | `https://iced-latte.uk/errors/order-not-found` |
| `OrderAccessDeniedException` | `https://iced-latte.uk/errors/order-access-denied` |
| `OrderCancellationWindowExpiredException` | `https://iced-latte.uk/errors/order-cancellation-expired` |
| `InvalidOrderStateTransitionException` | `https://iced-latte.uk/errors/order-state-invalid` |
| `UserNotFoundException` | `https://iced-latte.uk/errors/user-not-found` |
| `InvalidCredentialsException` | `https://iced-latte.uk/errors/invalid-credentials` |
| `UserAccountLockedException` | `https://iced-latte.uk/errors/account-locked` |
| `AbsentBearerHeaderException` | `https://iced-latte.uk/errors/auth-required` |
| `JwtTokenBlacklistedException` | `https://iced-latte.uk/errors/session-expired` |
| `JwtTokenException` | `https://iced-latte.uk/errors/auth-failed` |
| `JwtTokenHasNoUserEmailException` | `https://iced-latte.uk/errors/auth-failed` |
| `UserRegistrationException` | `https://iced-latte.uk/errors/registration-failed` |
| `StripeSessionCreationException` | `https://iced-latte.uk/errors/payment-session-failed` |
| `PaymentEventProcessingException` | `https://iced-latte.uk/errors/payment-event-failed` |
| `FileUploadException` | `https://iced-latte.uk/errors/file-upload-failed` |
| `FileReadException` | `https://iced-latte.uk/errors/file-read-failed` |
| `InvalidAvatarFileTypeException` | `https://iced-latte.uk/errors/invalid-avatar-type` |
| `InvalidTokenException` | `https://iced-latte.uk/errors/invalid-verification-code` |
| `TimeTokenException` | `https://iced-latte.uk/errors/verification-rate-limited` |
| `ShoppingCartItemNotFoundException` | `https://iced-latte.uk/errors/cart-item-not-found` |
| `InvalidShoppingCartIdException` | `https://iced-latte.uk/errors/cart-id-invalid` |
| `InvalidItemProductQuantityException` | `https://iced-latte.uk/errors/cart-item-quantity-invalid` |
| `ReviewModerationException` | `https://iced-latte.uk/errors/review-moderation-failed` |
| `SessionNotFoundException` | `https://iced-latte.uk/errors/session-not-found` |
| `SessionOwnershipException` | `https://iced-latte.uk/errors/session-access-denied` |
| Validation errors (`MethodArgumentNotValidException`, `ConstraintViolationException`) | `https://iced-latte.uk/errors/validation-failed` |
| Catch-all / unhandled | `about:blank` (RFC 9457 default) |

### 2. Extension properties

RFC 9457 allows custom fields via `ProblemDetail.setProperty()`. We keep two:

- `timestamp` (ISO 8601) — for log correlation
- `errors` (array of `{field, message}`) — for validation errors, same as current `FieldError`

### 3. No `ApiErrorResponse` — use `ProblemDetail` directly

Spring Boot 4's `ProblemDetail` class already has `type`, `title`, `status`, `detail`, `instance` plus a `properties` map for extensions. No custom DTO needed.

### 4. Content-Type becomes `application/problem+json`

RFC 9457 mandates this media type. Spring Boot handles it automatically when returning `ProblemDetail`.

## Implementation steps

### Step 1: Create `ProblemDetailFactory` (replaces `ApiErrorResponseCreator`)

New file: `common/exception/handler/ProblemDetailFactory.java`

A `@Service` with methods:
- `build(String type, String title, HttpStatus status, String detail)` → `ProblemDetail`
- `build(String type, String title, HttpStatus status, String detail, List<FieldError> errors)` → `ProblemDetail` with `errors` extension
- All methods auto-set `timestamp`

The `type` base URI (`https://iced-latte.uk/errors/`) is a constant.

### Step 2: Migrate all 7 exception handler classes

For each `@ExceptionHandler` method:
1. Change return type from `ApiErrorResponse` to `ProblemDetail`
2. Replace `apiErrorResponseCreator.buildResponse(...)` with `problemDetailFactory.build(...)`
3. Set the correct `type` slug per the table above
4. Keep all existing `@ResponseStatus` annotations and logging unchanged

Order of migration (by risk, lowest first):
1. `UserExceptionHandler` (2 handlers, simple)
2. `CommonExceptionHandler` / filestorage (2 handlers)
3. `PaymentExceptionHandler` (2 handlers)
4. `OrderExceptionHandler` (5 handlers)
5. `JwtTokenExceptionsHandler` (3 handlers)
6. `SignInExceptionHandler` (9 handlers)
7. `GlobalExceptionHandler` (13 handlers, catch-all)

### Step 3: Delete `ApiErrorResponse` and `ApiErrorResponseCreator`

Once all handlers are migrated, remove:
- `common/exception/dto/ApiErrorResponse.java`
- `common/exception/handler/ApiErrorResponseCreator.java`

### Step 4: Update all tests (7 test files, ~113 references)

For each test:
1. Change assertions from `ApiErrorResponse` fields to `ProblemDetail` fields
2. Assert `type` URI, `title`, `status`, `detail`
3. Assert extension properties (`timestamp`, `errors`) where applicable

Test files to update:
- `GlobalExceptionHandlerTest.java` (37 refs)
- `SignInExceptionHandlerTest.java` (31 refs)
- `JwtTokenExceptionsHandlerTest.java` (13 refs)
- `UserExceptionHandlerTest.java` (13 refs)
- `CommonExceptionHandlerTest.java` (7 refs)
- `OrderExceptionHandlerTest.java` (6 refs)
- `ApiErrorResponseCreatorTest.java` (6 refs → delete entirely, replace with `ProblemDetailFactoryTest`)

### Step 5: Update integration tests

Any REST Assured or MockMvc tests that assert on the JSON response body shape need to switch from `message`/`httpStatusCode` to `type`/`title`/`status`/`detail`.

### Step 6: Verify

- `mvn test` — all 649+ tests pass
- Manual smoke test: hit a bad endpoint, confirm `application/problem+json` content type and correct body shape

## Files changed (estimated)

| Action | Files | Count |
|---|---|---|
| Create | `ProblemDetailFactory.java`, `ProblemDetailFactoryTest.java` | 2 |
| Modify | 7 exception handler classes | 7 |
| Modify | 6 test files (handler tests) | 6 |
| Delete | `ApiErrorResponse.java`, `ApiErrorResponseCreator.java`, `ApiErrorResponseCreatorTest.java` | 3 |
| Modify | Integration tests asserting on error response shape | ~5–10 |
| **Total** | | **~20–25 files** |

## Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Frontend currently reads `data.message` and `data.httpStatusCode` | Frontend breaks on error display | `ProblemDetail` has `detail` (≈ old `message`) and `status` (≈ old `httpStatusCode`). Frontend needs a one-line change: `data.detail` instead of `data.message`. Alternatively, we can add `message` and `httpStatusCode` as extension properties for backward compatibility during transition. |
| `application/problem+json` content type | Frontend Axios might not parse it as JSON | Axios parses any `application/...+json` as JSON by default. No change needed. |
| Integration tests in external QA repo | May break if they assert on old field names | Coordinate with QA repo. The field mapping is straightforward. |

## Backward compatibility option

If we want zero frontend breakage during transition, we can add the old field names as extension properties:

```java
problemDetail.setProperty("message", detail);      // alias for "detail"
problemDetail.setProperty("httpStatusCode", status); // alias for "status"
```

This lets the frontend work with both old and new field names. Remove the aliases once the frontend is updated.

## Out of scope

- Frontend error code mapping (separate task — the frontend should eventually map `type` URIs to user-facing strings)
- Error type URI documentation page (nice-to-have — each `type` URI could resolve to a docs page explaining the error)
- i18n / localization
