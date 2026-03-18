# Stripe / Payment Changelog

## 2026-03-01 — Stripe integration restored

Restored the full `payment/` package from commit `b89101c9` (STRIPE_RESTORE_POINT).

### Files created
- `payment/api/PaymentProcessor.java`
- `payment/api/StripeSessionCreator.java`
- `payment/api/StripeSessionProvider.java`
- `payment/api/StripeShippingOptionsProvider.java`
- `payment/api/WebhookEventHandler.java`
- `payment/api/WebhookEventParser.java`
- `payment/api/WebhookEventProcessor.java`
- `payment/api/WebhookEventProvider.java`
- `payment/api/RedirectEventProcessor.java`
- `payment/api/scenario/SessionScenarioHandler.java`
- `payment/api/scenario/SessionCompletedScenarioHandler.java`
- `payment/api/scenario/SessionExpiredScenarioHandler.java`
- `payment/converter/StripeSessionLineItemListConverter.java`
- `payment/endpoint/PaymentEndpoint.java`
- `payment/entity/Payment.java`
- `payment/enums/StripeSessionConstants.java`
- `payment/enums/StripeSessionStatus.java`
- `payment/exception/PaymentEventParsingException.java`
- `payment/exception/PaymentEventProcessingException.java`
- `payment/exception/PaymentNotFoundException.java`
- `payment/exception/StripeSessionCreationException.java`
- `payment/exception/StripeSessionIsNotComplete.java`
- `payment/exception/StripeSessionRetrievalException.java`
- `payment/exception/handler/PaymentExceptionHandler.java`
- `email/sender/PaymentEmailConfirmation.java`
- `src/main/resources/api-specs/payment-openapi.yaml`

### Files modified
- `order/api/OrderCreator.java` — added `createOrderAndDeleteCart(Session)` alongside existing `create()` method
- `security/configuration/SecurityConstants.java` — restored `PAYMENT_URL` and `STRIPE_WEBHOOK_URL` constants
- `security/configuration/SpringSecurityConfiguration.java` — restored payment URL security rules (authenticated + webhook permitAll)
- `security/filter/RateLimitingFilter.java` — restored payment rate limit fields and branch
- `src/main/resources/application.yaml` — restored `spring.config.import: optional:stripe.yaml` and `stripe:` config block
- `src/test/resources/application-test.yaml` — restored `stripe.secret-key` and `stripe.webhook-secret` test placeholders
- `pom.xml` — restored `stripe.version` property, `stripe-java` dependency, and `generate-from-payment-openapi-spec` execution

### Test cards (use in Swagger UI or frontend)
| Card | Scenario |
|---|---|
| `4242 4242 4242 4242` | Payment succeeds |
| `4000 0000 0000 0002` | Card declined |
| `4000 0025 0000 3155` | 3D Secure required |
| `4000 0000 0000 9995` | Insufficient funds |

Expiry: any future date · CVC: any 3 digits · ZIP: any 5 digits

## 2026-03-01 — Stripe payment package simplified (SOLID/KISS/YAGNI)

### Deleted (12 files)
- `WebhookEventProvider`, `WebhookEventParser`, `WebhookEventHandler`, `WebhookEventProcessor` — 4-class pipeline replaced by `StripeWebhookService`
- `RedirectEventProcessor` — merged into `StripeWebhookService`
- `PaymentProcessor` — pass-through wrapper, endpoint now calls `StripeSessionCreator` directly
- `StripeSessionProvider` — one-liner wrapper, inlined into `StripeWebhookService`
- `StripeShippingOptionsProvider` — static data, moved to private method in `StripeSessionCreator`
- `Payment` entity — dead code, no repository, never persisted
- `PaymentEventParsingException` — never thrown
- `StripeSessionRetrievalException` — replaced by `StripeSessionCreationException` in retrieval path
- `StripeSessionStatus` enum — unused after entity removal
- `scenario/` package — `SessionScenarioHandler`, `SessionCompletedScenarioHandler`, `SessionExpiredScenarioHandler` replaced by switch in `StripeWebhookService`

### Result
24 files → 10 files. All behaviour preserved.
