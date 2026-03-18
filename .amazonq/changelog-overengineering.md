# Overengineering / KISS / YAGNI Fixes

## 2026-03-02

### Deleted: unused audit infrastructure (YAGNI)
- Removed `AuditAspect`, `AuditService`, `AuditLog`, `AuditLogRepository`, `AuditOperation`, `@Auditable`
- `@Auditable` was never placed on any method; `AuditLogRepository` query methods were never called from any endpoint or service
- Kept `AuditableEntity` and `AuditConfig` — they serve JPA `@CreatedBy`/`@LastModifiedBy` for `Order`, `UserEntity`, `ProductInfo`

### Deleted: `ErrorDebugMessageCreator` (YAGNI)
- Called in 8 places across `CartExceptionHandler`, `UserExceptionHandler`, `ProductExceptionHandler`, `OrderExceptionHandler`
- Return value was discarded at every call site — the built string was never logged or returned
- Removed the bean, all injections, all call sites, and its test class
- Updated `CartExceptionHandlerTest`, `UserExceptionHandlerTest`, `ProductExceptionHandlerTest` to remove the mock and `verify` calls

### Fixed: `AbstractEmailSender.getMessage()` hardcoded type (OCP/LSP)
- Was filtering `messageBuilders` by `EmailConfirmMessage.class` regardless of generic `<T>`
- Now dispatches on `event.getClass()` so the method actually respects the type parameter
- Error message now reports the actual missing type instead of always blaming `EmailConfirmMessage`

### Replaced: `PatternReplacer` + `TokenGenerator` (KISS)
- `PatternReplacer` was a 40-line stateful index-tracking class used solely to generate a 9-digit numeric token
- Replaced with `String.format("%09d", random.nextInt(1_000_000_000))` in `TokenGenerator`
- Deleted `PatternReplacer.java` and `PatternReplacerTest.java`

## 2026-03-02 (round 2)

### Deleted: `CorrelationContext` (YAGNI)
- `ThreadLocal` state was set in `CorrelationFilter` but never read by any production code
- `CorrelationContext.get()` / `getOrGenerate()` had zero callers outside the filter itself
- Simplified `CorrelationFilter` to use MDC directly — same behaviour, no indirection
- Deleted `CorrelationContext.java` and `CorrelationContextTest.java`

### Removed: Gson from `UpdateUserOperationPerformer` / `PutUsersRequestValidator` (YAGNI)
- Project already uses Jackson; `Gson` was instantiated as a field solely to convert `AddressDto` → `JsonObject`
- `PutUsersRequestValidator` used Gson `JsonObject` + reflection to check for unknown fields and non-string values — both checks are redundant since Jackson deserialization already ignores unknown fields and `AddressDto` fields are typed `String`
- Changed validator signature to accept `AddressDto` directly; removed all Gson imports from both classes
- Updated `PutUsersRequestValidatorTest` to use `AddressDto` instead of `JsonParser`

### Dropped findings (false positives after re-check)
- Payment rate limit config in `RateLimitingFilter` — `PAYMENT_URL` still referenced in `SecurityConstants` and `SpringSecurityConfiguration`; payment module not fully removed
- `DeleteUserOperationPerformer` — consistent with module's decomposition style (`DeliveryAddressDeleter`, etc.)
- `SecurityEventListener` — `onAuthorizationDenied` provides unique 403 logging not covered elsewhere

## 2026-03-02 (round 3)

### Scanned: JWT, security, product, user avatar, AI review, delivery address modules
- No changes made — all remaining code is appropriately sized or in security-sensitive paths

### Dropped findings (false positives after full context analysis)
- `JwtTokenFromAuthHeaderExtractor.isValidTokenFormat()` — redundant pre-validation before JWT parser, but security-sensitive path; low reward, not worth the risk
- `UserAvatarLinkUpdater` — one-method wrapper, consistent with the module's established decomposition style (`DeliveryAddressDeleter`, `DeliveryAddressProvider`, etc.)
- `JwtBlacklistService.sha256()` as interface default method — borderline ISP violation, but both implementations use it and it is small; not worth splitting
- All delivery address classes, product providers, AI moderation module — appropriately sized and purposeful

