# Kafka Product Review Integration Plan

**Date:** 2026-05-24
**Scope:** Backend product-review module, Apache Kafka, transactional outbox, durable inbox
**Status:** Draft implementation plan

> This document is the intended implementation guide for integrating Apache Kafka into the Iced Latte product-review flow. Before implementation, verify the current `review/` module, Liquibase migrations, Kafka configuration, tests, and Vault Kafka runtime docs.

---

## Executive Summary

Iced Latte will use Apache Kafka for asynchronous product-review processing. The first Kafka event will be `review.created`, published to:

```text
iced-latte.review.created.v1
```

The integration must be reliable, optional, and safe for local contributors:

- Product-review creation must not fail just because Kafka is down.
- Local contributor environments must keep Kafka disabled by default.
- Production-like deployments may enable Kafka with configuration.
- The Kafka-enabled path must use a transactional outbox for reliable publishing.
- The Kafka consumer must use a durable inbox for idempotent processing and retry control.
- The existing non-Kafka async review processing must remain only as the `kafka.enabled=false` fallback.
- `ProductReviewManager` should stay focused on product-review business work. Kafka-specific routing should live in event adapters/listeners.

The target architecture is:

```text
User creates product review
-> ProductReviewManager saves ProductReview
-> ProductReviewManager publishes in-process ReviewCreatedEvent
-> Kafka-enabled transactional listener inserts outbox_events row before commit
-> Outbox worker locks eligible outbox rows
-> Outbox worker publishes to Kafka and waits for broker ack
-> Outbox worker marks row PUBLISHED only after ack
-> Kafka listener records message into inbox_events and commits Kafka offset
-> Inbox worker locks eligible inbox rows
-> Inbox worker runs review async processing
-> rejected review is deleted, aggregates are refreshed, summary update is scheduled
-> Inbox worker marks row PROCESSED
```

This gives the project a production-style outbox/inbox event flow without making Kafka mandatory for every developer.

---

## Goals

- Integrate Kafka into the product-review module first.
- Replace the Kafka-enabled async review flow with a durable Kafka path.
- Preserve non-Kafka local behavior behind `kafka.enabled=false`.
- Use one shared `outbox_events` table for all future Iced Latte modules.
- Use one shared `inbox_events` table for durable, idempotent consumers.
- Store only stable identifiers in Kafka payloads, not review text.
- Keep the design concrete enough that future implementation agents do not invent conflicting patterns.
- Build a credible senior/lead-level engineering story: at-least-once delivery, durable handoff, idempotent consumers, retries, failure status, and operational visibility.

## Non-Goals

- Do not add Kafka to payment, order, cart, or user modules in the first implementation.
- Do not introduce Kafka Connect or Debezium in the first implementation.
- Do not add Schema Registry in the first implementation.
- Do not add Kafka DLQ topics in the first implementation.
- Do not expose Kafka publicly.
- Do not publish full JPA entities or raw review text to Kafka.
- Do not make review creation synchronously depend on Kafka availability.

---

## Current State

The product-review module already has a partial event flow:

- `ProductReviewManager` saves a review and publishes `ReviewCreatedEvent`.
- `ReviewCreatedApplicationEventListener` handles `ReviewCreatedEvent` locally when Kafka is disabled.
- `ReviewCreatedKafkaPublisher` directly sends `ReviewCreatedEvent` to Kafka when Kafka is enabled.
- `ReviewCreatedKafkaConsumer` consumes Kafka events and calls `AsyncReviewProcessingService`.
- `AsyncReviewProcessingService` runs moderation logic. If moderation rejects the review, it deletes the review, refreshes product aggregates, and schedules summary update.
- Kafka-related configuration already exists in `application.yaml`, `application-dev.yaml`, `.env.example`, and `docker-compose.yml`.

Problems with the current partial Kafka path:

- Direct Kafka publishing from `@TransactionalEventListener` is not a reliable outbox pattern.
- If Kafka publish fails after the review transaction commits, there is no durable retry record.
- There is no durable consumer inbox.
- Duplicate Kafka delivery is not explicitly controlled by a consumer-side idempotency table.
- Existing serializer trusted-package config appears to reference `com.zufar.icedlatte.review.kafka`, but current classes live under `com.zufar.icedlatte.review.messaging.kafka.*`; this must be verified and fixed during implementation.
- Existing direct Kafka publishing should be replaced, not extended. The final Kafka-enabled path must be `ReviewCreatedEvent -> outbox_events -> Kafka -> inbox_events -> inbox worker`.
- Current Kafka code serializes/deserializes a Java-specific `ReviewCreatedKafkaEvent` type directly. The outbox design should prefer storing and publishing the JSON envelope from `outbox_events.payload`, then mapping that JSON to the review event contract at the listener boundary. This reduces coupling to Java package names and Spring JSON type headers.
- `ReviewCreatedEvent` currently does not carry `userId`; therefore `actorId` cannot be populated without changing the internal event or passing request context separately.
- AI is optional through `ai.enabled`; when AI is disabled, moderation and summary services are no-op implementations. Kafka integration must preserve that behavior.
- `docs/events/schemas/review-created-event.schema.json` currently still requires `payload.text`; implementation must update that schema before Kafka-enabled mode is considered contract-correct.

Repo-specific facts to keep in mind during implementation:

- `IcedLatteApplication` already has `@EnableScheduling`, `@EnableAsync`, `@EnableRetry`, and `@ConfigurationPropertiesScan`.
- Liquibase root config is `src/main/resources/db/changelog-master.yaml`; version 2 migrations are included from `src/main/resources/db/changelog/version-2.0/changelog-master-version-2.0.yaml`.
- Spring Modulith verifies module dependencies and controls exposed `@NamedInterface` packages in `ModularityTests`.
- `spring-boot-starter-kafka-test`, Testcontainers PostgreSQL, and JSON Schema validator dependencies already exist in `pom.xml`, so the proposed contract and Kafka tests should not need a new test stack.

---

## Target Event

### Event Name

```text
review.created
```

Meaning: a product review has been successfully committed in Iced Latte and asynchronous product-review work may react to it.

### Topic

```text
iced-latte.review.created.v1
```

### Partition Key

Use:

```text
productId
```

Reason: product-review summary work is product-scoped. Using `productId` preserves event order per product.

Avoid composite keys such as `productId:reviewId` for v1. They add complexity and weaken the main ordering property because every review has a different `reviewId`.

Ordering caveat: the Kafka key only preserves order after records are accepted by Kafka into the same partition. The application can still publish records out of order if multiple outbox workers concurrently publish events for the same `productId`. V1 should keep outbox and inbox worker concurrency at `1` unless key-level locking is implemented. If Iced Latte later runs multiple backend instances with Kafka enabled, either run only one active outbox/inbox worker instance or add per-key/advisory locking for `partition_key`.

### Topic Provisioning

Iced Latte should not rely on Kafka auto topic creation. Vault Kafka currently treats `infra/kafka/topics.yml` as the canonical planned topic inventory, and `iced-latte.review.created.v1` is already listed there with:

```text
partitions: 3
replication_factor: 1
retention_ms: 259200000
owner: iced-latte
```

Implementation assumptions:

- Local contributor Compose does not run Kafka and keeps `KAFKA_ENABLED=false`.
- Production-like Vault deployments must create the topic before enabling Iced Latte Kafka.
- If the topic is missing when Kafka is enabled, outbox publishing should fail retryably and leave rows inspectable; review creation must still succeed because the outbox row was already committed.
- Do not enable broker auto topic creation as a shortcut.

### Payload

Do not publish review text to Kafka. Publish identifiers only.

```json
{
  "eventId": "3c4c0c8f-7643-412c-bd89-489b40fb5f24",
  "eventType": "review.created",
  "eventVersion": 1,
  "sourceApp": "iced-latte",
  "occurredAt": "2026-05-24T12:00:00Z",
  "correlationId": "optional-request-or-trace-id",
  "actorId": "optional-user-id",
  "payload": {
    "reviewId": "945ad22e-c9ad-4074-9e4c-78e49afc3859",
    "productId": "06bd245b-ef29-4b7f-b748-0da01d7c16c9"
  }
}
```

The consumer must load the `ProductReview` from PostgreSQL by `reviewId` before moderation.

Mapping rule:

- Internal `ReviewCreatedEvent` may keep `text` for the non-Kafka local fallback.
- Kafka-facing `ReviewCreatedKafkaEvent` and the `outbox_events.payload` must not include `text`.
- `ReviewCreatedOutboxWriter` is responsible for mapping internal domain event data to the Kafka envelope with only `reviewId` and `productId`.
- Use the application `ObjectMapper` for JSON serialization so Java time handling and future JSON config stay consistent.
- Treat serialization failure while writing the outbox row as an application bug and roll back the review transaction in Kafka-enabled mode.
- Do not put secrets, auth headers, cookies, raw tokens, or review text into payload or headers.

Header policy:

- Headers are optional in v1.
- Safe headers may include `eventId`, `eventType`, `eventVersion`, `sourceApp`, `correlationId`, and `contentType`.
- Do not copy inbound HTTP headers wholesale into Kafka or database event headers.
- Do not store `Authorization`, `Cookie`, CSRF tokens, session ids, refresh/access tokens, API keys, or provider credentials in event headers.
- If `actorId` is populated later, treat it as private data and keep it out of logs unless explicitly needed and reviewed.

Contract validation rule:

- Keep `docs/events/asyncapi.yaml`, `docs/events/schemas/review-created-event.schema.json`, `ReviewCreatedKafkaEvent`, and the outbox writer mapping in sync in the same PR.
- Add at least one test that serializes the Java Kafka event/envelope and validates the resulting JSON against `review-created-event.schema.json`, or an equivalent strict contract test if JSON Schema validation is not added.
- The schema must require `reviewId` and `productId`, and must not allow `payload.text`.
- Prefer a strict JSON Schema for the event envelope: require top-level metadata fields, require `payload.reviewId` and `payload.productId`, and reject unknown payload fields unless there is a deliberate compatibility reason.
- Treat event version changes as a contract change. If the payload changes incompatibly, create a new topic suffix such as `.v2` instead of silently changing `.v1`.

---

## Kafka Disabled Mode

Kafka must be disabled by default for local contributors.

Expected configuration:

```yaml
kafka:
  enabled: false
```

Expected env values:

```text
KAFKA_ENABLED=false
KAFKA_BOOTSTRAP_SERVERS=kafka:19092
KAFKA_CLIENT_ID=iced-latte
KAFKA_TOPIC_REVIEW_CREATED=iced-latte.review.created.v1
KAFKA_CONSUMER_GROUP_REVIEW_AI=iced-latte-review-ai
```

When `kafka.enabled=false`:

- Do not start Kafka outbox publisher workers.
- Do not start Kafka listeners.
- Do not start inbox workers for Kafka messages.
- Keep local `ReviewCreatedApplicationEventListener` behavior.
- `ProductReviewManager` still publishes the in-process `ReviewCreatedEvent`; the local listener handles it after commit.
- `AsyncReviewProcessingService` still handles moderation as it does today.

When `kafka.enabled=true`:

- Disable the local `ReviewCreatedApplicationEventListener`.
- Enable a transactional outbox listener for `ReviewCreatedEvent`.
- Write `review.created` to `outbox_events` before the review transaction commits.
- Publish from outbox to Kafka.
- Record consumed Kafka messages into `inbox_events`.
- Process review moderation from inbox worker.

There must never be a mode where both local async processing and Kafka inbox processing handle the same review-created event.

---

## Database Design

### Outbox Table

Use one shared table for all Iced Latte modules:

```text
outbox_events
```

Rationale:

- Iced Latte is one application.
- Future modules can reuse the same publisher, retry logic, cleanup, and monitoring.
- One table avoids duplicated polling infrastructure per event type.
- Event type, aggregate type, topic, and payload distinguish events.

Suggested columns:

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | Row identifier |
| `event_id` | UUID unique not null | Stable event identifier used by consumers |
| `aggregate_type` | VARCHAR not null | `PRODUCT_REVIEW`, later `ORDER`, `PAYMENT`, etc. |
| `aggregate_id` | UUID not null | `reviewId` for `review.created` |
| `event_type` | VARCHAR not null | `review.created` |
| `event_version` | INT not null | `1` |
| `topic` | VARCHAR not null | `iced-latte.review.created.v1` |
| `partition_key` | VARCHAR not null | `productId` |
| `payload` | JSONB not null | Event envelope JSON |
| `headers` | JSONB nullable | Optional safe Kafka headers |
| `status` | VARCHAR not null | See statuses below |
| `attempt_count` | INT not null default 0 | Publish attempts |
| `max_attempts` | INT not null default 10 | Limit before permanent failure |
| `next_attempt_at` | TIMESTAMPTZ not null | Retry scheduling |
| `locked_by` | VARCHAR nullable | Worker instance id |
| `locked_at` | TIMESTAMPTZ nullable | Lease timestamp |
| `published_at` | TIMESTAMPTZ nullable | Set after Kafka ack |
| `kafka_partition` | INT nullable | Partition returned by Kafka ack |
| `kafka_offset` | BIGINT nullable | Offset returned by Kafka ack |
| `last_error` | TEXT nullable | Safe error message |
| `created_at` | TIMESTAMPTZ not null | Audit |
| `updated_at` | TIMESTAMPTZ not null | Audit |

JPA mapping guidance:

- Store `status` as `@Enumerated(EnumType.STRING)`.
- Map `payload` and `headers` either as `String` containing canonical JSON or as a Jackson type supported by Hibernate 6 JSON mapping, for example `@JdbcTypeCode(SqlTypes.JSON)` with `columnDefinition = "jsonb"`.
- Do not use Java serialization or database-specific object blobs for event payloads.
- Keep persisted enum names stable; changing enum names later becomes a data migration.
- Do not add a foreign key from `aggregate_id` to `product_reviews.id`. Outbox rows must survive even if the review is later deleted by moderation or user action.

Suggested statuses:

```text
PENDING
IN_PROGRESS
PUBLISHED
FAILED_RETRYABLE
FAILED_PERMANENT
CANCELLED
```

Required indexes:

```text
UNIQUE (event_id)
UNIQUE (aggregate_type, aggregate_id, event_type, event_version)
INDEX (status, next_attempt_at)
INDEX (aggregate_type, aggregate_id)
INDEX (topic)
INDEX (partition_key, created_at)
```

The `(aggregate_type, aggregate_id, event_type, event_version)` uniqueness rule prevents accidentally creating two `review.created.v1` outbox rows for the same review. If a future event type legitimately needs multiple events with the same aggregate and event type, add a separate sequence or business discriminator before reusing this constraint.

Consider a partial index for polling efficiency if the table grows:

```text
INDEX ON outbox_events (next_attempt_at, created_at)
WHERE status IN ('PENDING', 'FAILED_RETRYABLE')
```

Recommended constraints:

```text
CHECK (attempt_count >= 0)
CHECK (max_attempts > 0)
CHECK (event_version > 0)
CHECK (status IN ('PENDING', 'IN_PROGRESS', 'PUBLISHED', 'FAILED_RETRYABLE', 'FAILED_PERMANENT', 'CANCELLED'))
```

Column default rules:

- New rows should use `status = PENDING`, `attempt_count = 0`, `max_attempts = kafka.outbox.max-attempts`, `next_attempt_at = now()`, `created_at = now()`, and `updated_at = now()`.
- Terminal rows should clear `locked_by` and `locked_at`.
- Retryable failure rows should clear `locked_by` and `locked_at`, increment `attempt_count`, set `last_error`, and set the next retry time.
- `last_error` must be sanitized and bounded before storing. Do not store payloads, secrets, stack traces, or unbounded provider responses.
- Every status transition must update `updated_at`.
- Status updates after publishing should guard by `id`, expected status, and `locked_by` so an old worker cannot overwrite a row that was reclaimed by another worker after a stale-lock timeout.

Recommended retention:

- Keep `PUBLISHED` rows for a short operational window, for example 7-30 days.
- Keep `FAILED_PERMANENT` rows until manually inspected or explicitly cleaned.
- Do not delete unpublished rows automatically.
- Add cleanup as a later scheduled job, not as part of the first publisher loop.

### Inbox Table

Use one shared durable inbox table:

```text
inbox_events
```

The Kafka listener must write to this table quickly and then commit the Kafka offset. Business work happens in a separate inbox worker.

Suggested columns:

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | Row identifier |
| `event_id` | UUID not null | Original event id |
| `event_type` | VARCHAR not null | `review.created` |
| `event_version` | INT not null | `1` |
| `topic` | VARCHAR not null | Original Kafka topic |
| `partition_key` | VARCHAR not null | Original Kafka key |
| `kafka_partition` | INT not null | Source Kafka partition |
| `kafka_offset` | BIGINT not null | Source Kafka offset |
| `consumer_name` | VARCHAR not null | `iced-latte-review-ai` |
| `payload` | JSONB not null | Original event envelope |
| `headers` | JSONB nullable | Optional safe Kafka headers |
| `status` | VARCHAR not null | See statuses below |
| `attempt_count` | INT not null default 0 | Processing attempts |
| `max_attempts` | INT not null default 10 | Limit before permanent failure |
| `next_attempt_at` | TIMESTAMPTZ not null | Retry scheduling |
| `locked_by` | VARCHAR nullable | Worker instance id |
| `locked_at` | TIMESTAMPTZ nullable | Lease timestamp |
| `processed_at` | TIMESTAMPTZ nullable | Set after business processing succeeds |
| `last_error` | TEXT nullable | Safe error message |
| `created_at` | TIMESTAMPTZ not null | Audit |
| `updated_at` | TIMESTAMPTZ not null | Audit |

JPA mapping guidance:

- Store `status` as `@Enumerated(EnumType.STRING)`.
- Use the same JSONB mapping strategy as `outbox_events` for `payload` and `headers`.
- Avoid module-specific entity dependencies in shared inbox infrastructure; event-specific code should parse the JSON envelope at the processor boundary.
- Do not add a foreign key from payload `reviewId` or `aggregate_id` to `product_reviews.id`. Inbox rows must remain auditable after review deletion.

Suggested statuses:

```text
RECEIVED
IN_PROGRESS
PROCESSED
FAILED_RETRYABLE
FAILED_PERMANENT
IGNORED
```

Required indexes:

```text
UNIQUE (event_id, consumer_name)
UNIQUE (topic, kafka_partition, kafka_offset, consumer_name)
INDEX (status, next_attempt_at)
INDEX (event_type)
INDEX (consumer_name, status)
INDEX (partition_key, created_at)
```

Consider a partial index for polling efficiency if the table grows:

```text
INDEX ON inbox_events (consumer_name, next_attempt_at, created_at)
WHERE status IN ('RECEIVED', 'FAILED_RETRYABLE')
```

Recommended constraints:

```text
CHECK (attempt_count >= 0)
CHECK (max_attempts > 0)
CHECK (event_version > 0)
CHECK (status IN ('RECEIVED', 'IN_PROGRESS', 'PROCESSED', 'FAILED_RETRYABLE', 'FAILED_PERMANENT', 'IGNORED'))
```

The unique `(event_id, consumer_name)` constraint is the consumer idempotency boundary. Duplicate Kafka delivery must not process the same event twice for the same consumer.

Column default rules:

- New rows should use `status = RECEIVED`, `attempt_count = 0`, `max_attempts = kafka.inbox.max-attempts`, `next_attempt_at = now()`, `created_at = now()`, and `updated_at = now()`.
- `kafka_partition` and `kafka_offset` should be non-null for Kafka-created inbox rows so the offset uniqueness constraint is meaningful.
- Terminal rows should clear `locked_by` and `locked_at`.
- Retryable failure rows should clear `locked_by` and `locked_at`, increment `attempt_count`, set `last_error`, and set the next retry time.
- `consumer_name` is the durable processor identity, not just the Kafka group id. For this flow it should match the configured review AI consumer group unless there is a deliberate reason to separate those names.
- Every status transition must update `updated_at`.
- Status updates after processing should guard by `id`, expected status, and `locked_by` so a stale worker cannot overwrite a row that another worker reclaimed.

Recommended retention:

- Keep `PROCESSED` and `IGNORED` rows for a short operational window, for example 7-30 days.
- Keep `FAILED_PERMANENT` rows until manually inspected or explicitly cleaned.
- Do not delete retryable rows automatically.

### DLQ Decision

Do not add Kafka DLQ topics in the first implementation.

The durable inbox already provides database-backed DLQ semantics through:

```text
inbox_events.status = FAILED_PERMANENT
```

This is enough for v1 because failed events remain queryable with payload, status, attempts, timestamps, and error message.

Future Kafka DLQ topics may be added if failure triage needs to happen through Kafka tooling or another service becomes responsible for failed-event handling. A possible future topic is:

```text
iced-latte.review.created.dlq.v1
```

---

## Outbox Flow

### Write Path

`ProductReviewManager.create(...)` should save the review and publish the in-process domain event. Delivery adapters decide what happens with that event.

```text
begin transaction
  validate product/review/user rules
  save ProductReview
  refresh product review aggregates
  publish ReviewCreatedEvent
  if kafka.enabled=true:
      ReviewCreatedOutboxEventListener writes outbox_events row before commit
  else:
      ReviewCreatedApplicationEventListener runs local async processing after commit
commit transaction
```

Implementation must ensure the outbox row cannot exist without the review, and the review cannot commit without the outbox row when Kafka is enabled.

Recommended Spring boundary:

- `ProductReviewManager` continues to call `ApplicationEventPublisher.publishEvent(...)`.
- `ReviewCreatedOutboxEventListener` is conditional on `kafka.enabled=true`.
- `ReviewCreatedOutboxEventListener` uses `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` so the outbox insert participates in the same transaction as the review insert.
- `ReviewCreatedOutboxEventListener` should not use `fallbackExecution=true`; if there is no active review transaction, writing an outbox row is a bug.
- The outbox row `event_id` must come from the internal `ReviewCreatedEvent.eventId`, not from a new UUID generated by the outbox writer.
- `ReviewCreatedApplicationEventListener` remains conditional on `kafka.enabled=false` and uses `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with `@Async`.
- If the outbox insert fails in Kafka-enabled mode, the review transaction must roll back.

### Publish Worker

The outbox worker should:

1. Poll rows where:

```text
status in (PENDING, FAILED_RETRYABLE)
and next_attempt_at <= now()
```

2. Lock a limited batch using database row locking.
3. Mark rows `IN_PROGRESS` with `locked_by` and `locked_at`.
4. Publish each event to Kafka.
5. Wait for Kafka broker acknowledgement.
6. Mark the row `PUBLISHED` only after successful ack.
7. On transient failure, mark `FAILED_RETRYABLE`, increment `attempt_count`, store `last_error`, and set `next_attempt_at` using backoff.
8. If max attempts is exceeded, mark `FAILED_PERMANENT`.

Attempt-count semantics:

- Increment `attempt_count` only after a publish attempt fails.
- The first failed publish sets `attempt_count = 1`.
- If the next failure would make `attempt_count >= max_attempts`, mark the row `FAILED_PERMANENT` instead of scheduling another retry.
- Successful publishes do not need to increment `attempt_count`; `published_at`, `kafka_partition`, and `kafka_offset` are the success audit fields.

Use PostgreSQL row locking semantics:

```sql
SELECT *
FROM outbox_events
WHERE status IN ('PENDING', 'FAILED_RETRYABLE')
  AND next_attempt_at <= now()
ORDER BY created_at
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```

The implementation may express this through a native Spring Data query. `SKIP LOCKED` is important because it allows multiple app instances to run the worker without blocking each other on the same rows.

Recommended claim shape:

```sql
WITH candidate AS (
  SELECT id
  FROM outbox_events
  WHERE status IN ('PENDING', 'FAILED_RETRYABLE')
    AND next_attempt_at <= now()
  ORDER BY created_at
  LIMIT :batchSize
  FOR UPDATE SKIP LOCKED
)
UPDATE outbox_events o
SET status = 'IN_PROGRESS',
    locked_by = :workerId,
    locked_at = now(),
    updated_at = now()
FROM candidate
WHERE o.id = candidate.id
RETURNING o.*;
```

Stale `IN_PROGRESS` rows must be reclaimable. A worker should treat rows as retryable again when:

```text
status = IN_PROGRESS
and locked_at < now() - stale-lock-timeout
```

Do not rely on JVM memory to track in-progress rows. Database state is the source of truth.

Worker identity and clock:

- Generate a `workerId` at application startup, for example `${spring.application.name}:${hostname}:${randomUUID}`.
- Store that value in `locked_by` when claiming rows.
- Prefer database time (`now()` / `CURRENT_TIMESTAMP`) for `next_attempt_at`, `locked_at`, `published_at`, and `processed_at` comparisons so multiple app instances do not depend on perfectly synchronized JVM clocks.

Transaction shape:

- Claim rows in a short transaction: select eligible rows with `FOR UPDATE SKIP LOCKED`, set `IN_PROGRESS`, `locked_by`, and `locked_at`, then commit.
- Publish claimed rows outside the database transaction. Do not hold database row locks while waiting for Kafka broker acknowledgements.
- Mark each row `PUBLISHED`, `FAILED_RETRYABLE`, or `FAILED_PERMANENT` in a separate short transaction.
- Store Kafka `partition` and `offset` from the send result when marking `PUBLISHED`.
- When marking success or failure, update only rows that still match `id = :id`, `status = 'IN_PROGRESS'`, and `locked_by = :workerId`; if no row is updated, log it and do not retry in memory because another worker may have reclaimed the row.

Use small batches in v1, for example:

```text
kafka.outbox.batch-size=25
kafka.outbox.poll-interval=5s
kafka.outbox.max-attempts=10
kafka.outbox.stale-lock-timeout=5m
kafka.outbox.worker-enabled=true
kafka.outbox.worker-concurrency=1
```

The worker should be disabled unless `kafka.enabled=true`.

Scheduling guidance:

- Implement the polling loop with `@Scheduled(fixedDelayString = "${kafka.outbox.poll-interval:5s}")` or an equivalent scheduler bean.
- `IcedLatteApplication` already enables scheduling; do not add another application-level scheduling annotation.
- The scheduled method should immediately return when `kafka.enabled=false`, `kafka.outbox.enabled=false`, or `kafka.outbox.worker-enabled=false`.
- Keep the scheduled method thin; delegate claiming, publishing, and status transitions to testable services.

Ordering policy:

- V1 should use one outbox worker thread per application process.
- Production-like Vault deployment should run only one active Iced Latte backend instance with `kafka.outbox.worker-enabled=true` unless key-level locking is added.
- If multiple app instances are needed, keep Kafka listeners enabled on all instances if desired, but enable the outbox worker on only one instance or add a DB/advisory lock around each `partition_key`.
- Do not claim strict per-product ordering if multiple outbox workers can publish the same `partition_key` concurrently.

Backoff should be explicit and bounded. Recommended v1:

```text
next_attempt_at = now + min(2^attempt_count seconds, 5 minutes)
```

The exact formula can be implemented in Java for readability. Tests must prove that retry delay increases and does not grow without bound.

### Kafka Producer Acks

The outbox publisher must wait for the Kafka send result before marking the event `PUBLISHED`.

Producer config should prefer reliability:

```text
acks=all
enable.idempotence=true
retries > 0
max.in.flight.requests.per.connection <= 5
```

Because the Vault broker is single-node, replication factor is currently `1`, so `acks=all` means acknowledgement from the single in-sync broker. This is still the correct producer semantic and remains valid if the broker topology grows later.

Serialization recommendation:

- Prefer `KafkaTemplate<String, String>` or `KafkaTemplate<String, JsonNode>` for the generic outbox publisher so it publishes the JSON envelope stored in `outbox_events.payload`.
- Do not require the generic outbox publisher to know about `ReviewCreatedKafkaEvent`.
- If Spring's `JsonSerializer` / `JsonDeserializer` is kept, disable reliance on type headers and explicitly fix trusted packages/default type config. A bad package name must fail loudly during Kafka-enabled tests.
- Configure producer delivery timeout/request timeout intentionally so a stuck broker does not block a worker thread forever.

---

## Inbox Flow

### Kafka Listener

The Kafka listener should be thin.

It should:

1. Receive the Kafka record value and map it to the `review.created` event contract.
2. Insert one `inbox_events` row with status `RECEIVED`, including Kafka topic, partition, offset, key, and safe headers where available.
3. If `(event_id, consumer_name)` already exists, treat it as duplicate and skip.
4. Commit Kafka offset after durable inbox insert or duplicate detection.

The listener must not run AI moderation directly.

Offset acknowledgement rule:

- The listener must not acknowledge or commit the Kafka offset before the inbox insert transaction commits.
- Recommended v1: use manual acknowledgement for this listener.
- `Acknowledgment.acknowledge()` must be called only after successful inbox insert or duplicate detection.
- Current `ack-mode=record` should be changed globally or replaced with a review-specific listener container factory using manual ack.
- If the inbox insert fails, the Kafka record must be retried by Kafka rather than lost.
- Do not annotate the Kafka listener itself with `@Async`. Kafka listener concurrency should be controlled through Spring Kafka container configuration, not through application async execution.
- The listener must use an explicit stable Kafka group id from `kafka.consumer-groups.review-ai`; do not rely on a generated or environment-specific group id.
- Duplicate detection should acknowledge the Kafka record when an existing `inbox_events` row is found for `(event_id, consumer_name)`, regardless of whether that row is currently `RECEIVED`, `IN_PROGRESS`, `PROCESSED`, `FAILED_RETRYABLE`, `FAILED_PERMANENT`, or `IGNORED`.

This is the critical durability boundary for consumption:

```text
Kafka record -> committed inbox row -> Kafka offset commit
```

Deserialization and schema failures:

- If Spring cannot deserialize the Kafka record into `ReviewCreatedKafkaEvent`, the normal listener method may never run and therefore cannot write `inbox_events`.
- Prefer receiving raw JSON (`String` or `JsonNode`) and validating/mapping inside the listener transaction. This makes malformed records recordable/loggable with topic, partition, and offset.
- If typed Spring deserialization is kept, use Spring Kafka `ErrorHandlingDeserializer` and configure an error handler before enabling Kafka in production-like environments.
- For v1, deserialization failures may be treated as operational failures visible in logs/metrics because Iced Latte is the only producer of this topic.
- Do not silently skip malformed records. A malformed record must either be retried, logged with topic/partition/offset, or moved to a future Kafka DLQ when that is introduced.
- If malformed records are retried without a Kafka DLQ, they can block that partition. This is acceptable for v1 only because Iced Latte is the sole producer; production rollout should monitor this explicitly.

### Inbox Worker

The inbox worker should:

1. Poll rows where:

```text
status in (RECEIVED, FAILED_RETRYABLE)
and next_attempt_at <= now()
and consumer_name = 'iced-latte-review-ai'
```

2. Lock a limited batch.
3. Mark rows `IN_PROGRESS`.
4. Route by `event_type`.
5. For `review.created`, load the review by `reviewId`.
6. Call the same review moderation behavior currently used by `AsyncReviewProcessingService`.
7. If moderation rejects the review, delete it, refresh product aggregates, and schedule summary update.
8. Mark the inbox row `PROCESSED`.
9. On transient failure, mark `FAILED_RETRYABLE`.
10. On permanent failure or max attempts exceeded, mark `FAILED_PERMANENT`.

If the review no longer exists when the event is processed, mark the inbox row `IGNORED` with a clear log. This is acceptable because duplicate or stale events must be safe.

Attempt-count semantics:

- Increment `attempt_count` only after business processing fails.
- The first failed processing attempt sets `attempt_count = 1`.
- If the next failure would make `attempt_count >= max_attempts`, mark the row `FAILED_PERMANENT`.
- Do not increment `attempt_count` when a row is claimed and then processed successfully.

Use the same row-locking pattern as the outbox worker:

```sql
SELECT *
FROM inbox_events
WHERE status IN ('RECEIVED', 'FAILED_RETRYABLE')
  AND next_attempt_at <= now()
  AND consumer_name = :consumerName
ORDER BY created_at
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```

Use the same `WITH candidate ... UPDATE ... RETURNING` claim pattern as the outbox worker, setting `status = 'IN_PROGRESS'`, `locked_by`, `locked_at`, and `updated_at` in the claim transaction.

Stale `IN_PROGRESS` inbox rows must be reclaimable using `locked_at` and `kafka.inbox.stale-lock-timeout`.

The inbox worker must persist success or failure status outside the business transaction. If business processing fails, the failure marker must still commit and must not be rolled back with the failed business work.

Recommended approach:

- Mark row `IN_PROGRESS` in a short transaction.
- Run business processing in a new transaction.
- Mark `PROCESSED` in a short transaction after success.
- On exception, mark `FAILED_RETRYABLE` or `FAILED_PERMANENT` in a separate transaction.
- When marking success or failure, update only rows that still match `id = :id`, `status = 'IN_PROGRESS'`, and `locked_by = :workerId`.
- Implement the polling loop with `@Scheduled(fixedDelayString = "${kafka.inbox.poll-interval:5s}")` or an equivalent scheduler bean.
- The scheduled method should immediately return when `kafka.enabled=false`, `kafka.inbox.enabled=false`, or `kafka.inbox.worker-enabled=false`.

This mirrors the Stripe webhook event recorder pattern already used elsewhere in the project.

Inbox ordering policy:

- V1 should use one inbox worker thread per application process for `iced-latte-review-ai`.
- If multiple backend instances process inbox rows, add key-level locking or accept that database-side processing may not preserve per-product order even though Kafka partition order does.
- Review moderation is mostly per-review, but summary refresh is product-scoped; keep concurrency conservative until summary behavior is proven safe under out-of-order processing.

---

## Moderation Behavior

Kafka-enabled mode must preserve current behavior:

```text
review.created consumed
-> load ProductReview
-> run moderation against review text from PostgreSQL
-> if moderation rejects:
     delete ProductReview
     refresh product review aggregates
     schedule product summary update
-> if moderation passes:
     leave ProductReview as-is
```

This keeps behavior consistent with the existing non-Kafka implementation.

Tradeoff: moderation is eventually consistent. A rejected review may exist briefly before Kafka processing deletes it. This is already conceptually similar to the current async-after-commit behavior.

AI-disabled behavior:

- When `ai.enabled=false`, the current project wires no-op moderation and summary services.
- Kafka-enabled mode must still run the inbox processor, but the moderation call should pass through the same no-op service and mark the inbox row `PROCESSED`.
- Do not make Kafka enablement imply `ai.enabled=true`; they are separate feature flags.

Do not add a `PENDING_MODERATION` review status in this first Kafka integration. That would be a separate product behavior change.

Important refactor:

- The current local path can pass review text through `ReviewCreatedEvent`.
- The Kafka inbox path must not reconstruct a text-bearing domain event from Kafka payload.
- Add a review-processing entry point that accepts `reviewId` or loads `ProductReview` before moderation, for example `AsyncReviewProcessingService.processByReviewId(reviewId)`.
- The Kafka inbox processor should load the current review row, read text from PostgreSQL, and then reuse the same moderation/delete/aggregate/summary behavior.
- If the review is missing, treat it as `IGNORED` in v1.

Idempotent side-effect expectations:

- Deleting an already-deleted review must be safe and should result in `IGNORED`.
- `refreshReviewAggregates(productId)` recalculates aggregate state and is safe to call more than once.
- `summaryDebouncer.schedule(productId)` is product-scoped and debounce-based, so duplicate scheduling should not create incorrect product state.
- Any future side effect added to review-created processing must be idempotent before it is called from the inbox worker.

---

## Proposed Classes And Files

### Documentation

```text
docs/kafka-product-review-integration-plan.md
docs/events/asyncapi.yaml
docs/events/schemas/review-created-event.schema.json
```

`docs/events/` already exists and should remain the app-local event contract location.

### Database

```text
src/main/resources/db/changelog/version-2.0/DD.MM.2026.partN.create-outbox-events-table.sql
src/main/resources/db/changelog/version-2.0/DD.MM.2026.partN.create-inbox-events-table.sql
src/main/resources/db/changelog/version-2.0/changelog-master-version-2.0.yaml
```

Migration registration:

- Add both SQL files to `src/main/resources/db/changelog/version-2.0/changelog-master-version-2.0.yaml`.
- Follow the existing version-2.0 include style and set `errorIfMissing: true` for new Kafka migration includes.
- Keep DDL idempotence consistent with existing project migrations; do not rely on Hibernate schema generation because JPA uses `ddl-auto: validate`.
- Add `jsonb` columns directly in SQL migrations; the entity mapping must match the exact column types.

### Shared Event Infrastructure

Keep infrastructure app-local, not in Vault.

Do not extract a shared Kafka/event package in the first product-review implementation.
Spring Modulith should keep the first integration cohesive inside the `review` module.
The shared database tables are cross-module, but the Java code should stay
module-owned until a second module proves the common shape.

First implementation package:

```text
com.zufar.icedlatte.review.messaging.kafka
```

First implementation files:

```text
review/messaging/kafka/config/KafkaIntegrationProperties.java
review/messaging/kafka/event/ReviewCreatedKafkaEvent.java
review/messaging/kafka/outbox/OutboxEventRepository.java
review/messaging/kafka/outbox/ReviewCreatedOutboxEventListener.java
review/messaging/kafka/outbox/ReviewCreatedKafkaPublisher.java
review/messaging/kafka/inbox/InboxEventRepository.java
review/messaging/kafka/inbox/ReviewCreatedKafkaConsumer.java
review/messaging/kafka/inbox/ReviewCreatedInboxProcessor.java
```

Future extraction rule:

Extract only after another module needs Kafka. At that point, move only proven
generic mechanics into a cohesive infrastructure package, such as
`common.event.outbox` and `common.event.inbox`, while leaving event contracts,
mappers, listeners, and processors inside the owning feature module.

Spring Modulith note:

- The `review` module owns the first Kafka integration end to end.
- Avoid a technology-only top-level package just because Kafka is used.
- Do not put product-review business logic in `common`.
- When a second module integrates Kafka, extract only duplication that is
  already concrete: row claiming, retry/backoff, stale lock reclaiming, and
  terminal status transitions.
- Any future `common.event` package must not depend on `review`, `product`,
  `order`, `payment`, or any other feature module.

### Product Review Event Files

Suggested package:

```text
com.zufar.icedlatte.review.messaging.kafka
```

Suggested files:

```text
review/dto/ReviewCreatedEvent.java
review/messaging/kafka/event/ReviewCreatedKafkaEvent.java
review/messaging/kafka/outbox/ReviewCreatedOutboxWriter.java
review/messaging/kafka/outbox/ReviewCreatedOutboxEventListener.java
review/messaging/kafka/inbox/ReviewCreatedKafkaListener.java
review/messaging/kafka/inbox/ReviewCreatedKafkaEventMapper.java
review/messaging/kafka/inbox/ReviewCreatedInboxProcessor.java
```

Refactor existing direct classes:

- Replace `ReviewCreatedKafkaPublisher` with outbox writing and generic outbox publishing.
- Refactor `ReviewCreatedKafkaConsumer` so it records to `inbox_events` only.
- Keep `ReviewCreatedApplicationEventListener` only for `kafka.enabled=false`.
- Keep `AsyncReviewProcessingService` as the business service used by both local fallback and Kafka inbox processing.
- Remove `text` from `ReviewCreatedKafkaEvent.Payload`; keep text only in the internal `ReviewCreatedEvent` if the local fallback still needs it.
- Remove `ReviewCreatedKafkaEvent.toDomainEvent()` or stop using it for the Kafka path, because it currently implies text comes from Kafka.

---

## Configuration

Suggested application properties:

```yaml
spring:
  kafka:
    listener:
      ack-mode: manual
    producer:
      properties:
        acks: all
        enable.idempotence: true
    consumer:
      enable-auto-commit: false

kafka:
  enabled: ${KAFKA_ENABLED:false}
  topics:
    review-created: ${KAFKA_TOPIC_REVIEW_CREATED:iced-latte.review.created.v1}
  consumer-groups:
    review-ai: ${KAFKA_CONSUMER_GROUP_REVIEW_AI:iced-latte-review-ai}
  outbox:
    enabled: ${KAFKA_OUTBOX_ENABLED:${KAFKA_ENABLED:false}}
    worker-enabled: ${KAFKA_OUTBOX_WORKER_ENABLED:${KAFKA_ENABLED:false}}
    worker-concurrency: ${KAFKA_OUTBOX_WORKER_CONCURRENCY:1}
    batch-size: ${KAFKA_OUTBOX_BATCH_SIZE:25}
    poll-interval: ${KAFKA_OUTBOX_POLL_INTERVAL:5s}
    max-attempts: ${KAFKA_OUTBOX_MAX_ATTEMPTS:10}
    stale-lock-timeout: ${KAFKA_OUTBOX_STALE_LOCK_TIMEOUT:5m}
    retention: ${KAFKA_OUTBOX_RETENTION:30d}
  inbox:
    enabled: ${KAFKA_INBOX_ENABLED:${KAFKA_ENABLED:false}}
    worker-enabled: ${KAFKA_INBOX_WORKER_ENABLED:${KAFKA_ENABLED:false}}
    worker-concurrency: ${KAFKA_INBOX_WORKER_CONCURRENCY:1}
    batch-size: ${KAFKA_INBOX_BATCH_SIZE:25}
    poll-interval: ${KAFKA_INBOX_POLL_INTERVAL:5s}
    max-attempts: ${KAFKA_INBOX_MAX_ATTEMPTS:10}
    stale-lock-timeout: ${KAFKA_INBOX_STALE_LOCK_TIMEOUT:5m}
    retention: ${KAFKA_INBOX_RETENTION:30d}

ai:
  enabled: ${AI_ENABLED:false}
```

Kafka disabled mode must remain the default in:

```text
.env.example
src/main/resources/application-dev.yaml
src/main/resources/application-config.schema.json
```

Production-like deployments can set:

```text
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=kafka:19092
```

Configuration invariants:

- If `kafka.enabled=false`, local fallback is active and Kafka outbox/inbox components must not run.
- If `kafka.enabled=true`, local fallback is inactive and outbox writing for `review.created` must be active.
- If `kafka.enabled=true`, `kafka.outbox.enabled` and `kafka.inbox.enabled` must also be true for the product-review flow. `worker-enabled` may be false to pause publishing or processing, but event recording must not be disabled because that would drop work.
- Kafka listeners must set their group id from `kafka.consumer-groups.review-ai`; `spring.kafka.consumer.group-id` is optional only if every listener declares its group explicitly.
- `kafka.enabled` and `ai.enabled` are independent. Kafka can be enabled while AI remains disabled; in that case messages still flow through outbox/inbox and the no-op moderation/summary behavior is preserved.
- `kafka.outbox.worker-enabled=false` may be used on secondary app instances, but at least one production-like instance must run the outbox worker or events will remain pending.
- `kafka.inbox.worker-enabled=false` may be used to pause business processing intentionally, but then `inbox_events` will accumulate.
- Fail application startup when `kafka.enabled=true` and required topic/group/bootstrap properties are blank.
- Fail application startup when `kafka.enabled=true` but outbox or inbox recording is disabled for the review-created flow.
- Fail application startup when worker settings are invalid, for example non-positive batch size, non-positive max attempts, negative/zero poll interval, or negative/zero stale-lock timeout.
- Validate `kafka.outbox.worker-concurrency` and `kafka.inbox.worker-concurrency` are `1` in v1 unless key-level locking is implemented in the same PR.
- Prefer failing startup over silently dropping product-review events because of inconsistent Kafka configuration.
- Update `.env.example` with any new `KAFKA_OUTBOX_*` and `KAFKA_INBOX_*` variables introduced by the implementation.
- Update `application-config.schema.json` if the project uses it to document or validate configuration keys.

---

## Implementation Phases

### Phase 1: Event Contract And Configuration Cleanup

Goal: make the event contract and configuration explicit before schema work.

Tasks:

- Verify `ReviewCreatedKafkaEvent` package names match Spring Kafka JSON deserializer config.
- Verify `docs/events/asyncapi.yaml` still points to `iced-latte.review.created.v1` and the review-created schema.
- Update `review-created-event.schema.json` to remove `text` from payload.
- Make the schema strict enough to reject unexpected `payload.text`.
- Add or update a contract test proving the serialized event matches the schema.
- Update `ReviewCreatedKafkaEvent.Payload` to remove `text`.
- Ensure `.env.example` contains Kafka defaults.
- Ensure `application-dev.yaml` disables Kafka.
- Ensure `application-prod.yaml` can opt in via env vars.
- Keep local `docker-compose.yml` backend default at `KAFKA_ENABLED=false`; production/Vault deployment config owns enabling Kafka.
- Verify the topic exists in Vault `infra/kafka/topics.yml` and is created before enabling Kafka in Iced Latte.
- Switch the review-created listener to manual acknowledgement, or isolate it in a listener container factory with manual ack if other listeners need different behavior.
- Add configuration-properties validation so Kafka-enabled startup fails when required Kafka topic/group/bootstrap settings are blank.
- Add configuration validation for positive batch sizes, positive max attempts, positive durations, and v1 worker concurrency limits.

Acceptance criteria:

- App starts with Kafka disabled.
- Event schema contains only `reviewId` and `productId` in payload.
- Kafka topic and consumer group names are config-driven.
- `kafka.enabled=true` does not require `ai.enabled=true`.
- Invalid worker configuration fails startup.
- `kafka.enabled=true` fails startup if review-created outbox/inbox recording is disabled.
- Missing Kafka topic does not break review creation; it leaves outbox rows retryable.
- Invalid Kafka-enabled configuration fails startup instead of silently dropping events.

### Phase 2: Outbox Schema And Writer

Goal: reliably record product-review events in PostgreSQL.

Tasks:

- Add Liquibase migration for `outbox_events`.
- Register the migration in `changelog-master-version-2.0.yaml` with `errorIfMissing: true`.
- Add `OutboxEvent` entity and repository.
- Add `OutboxEventWriter`.
- Add `ReviewCreatedOutboxWriter`.
- Add `ReviewCreatedOutboxEventListener` with `TransactionPhase.BEFORE_COMMIT`.
- Ensure the outbox listener does not run without an active transaction.
- Store the serialized Kafka envelope in `outbox_events.payload` during the same database transaction as review creation.
- Reuse `ReviewCreatedEvent.eventId` as `outbox_events.event_id`.
- Keep `ProductReviewManager.create(...)` publishing `ReviewCreatedEvent`; avoid Kafka-specific branching in the business service unless implementation constraints force it.
- Ensure Kafka-disabled mode still uses local `ReviewCreatedEvent` fallback.

Acceptance criteria:

- When Kafka is enabled, creating a review inserts one outbox row.
- If the review transaction rolls back, no outbox row remains.
- If the outbox insert fails, the review transaction rolls back.
- When Kafka is disabled, current local async behavior still works.
- Unit tests prove Kafka-enabled and Kafka-disabled listeners are mutually exclusive.
- Unit tests prove the outbox writer preserves the original event id.
- Duplicate `review.created` outbox writes for the same review are rejected by the database uniqueness boundary.

### Phase 3: Outbox Publisher Worker

Goal: publish outbox rows to Kafka reliably.

Tasks:

- Add `KafkaOutboxPublisher`.
- Poll eligible `PENDING` and `FAILED_RETRYABLE` rows.
- Use row locking to avoid two app instances publishing the same row concurrently.
- Use `FOR UPDATE SKIP LOCKED` or equivalent PostgreSQL row-locking behavior.
- Publish to Kafka with configured topic and partition key.
- Publish the stored JSON envelope from `outbox_events.payload`; do not remap from a JPA entity to product-review-specific DTO inside the generic publisher.
- Wait for Kafka send acknowledgement.
- Mark `PUBLISHED` only after ack.
- Mark retryable failures with backoff.
- Mark permanent failures after max attempts.
- Reclaim stale `IN_PROGRESS` rows after `stale-lock-timeout`.

Acceptance criteria:

- Kafka unavailable does not break review creation.
- Kafka unavailable leaves outbox rows retryable.
- Successful broker ack marks row `PUBLISHED`.
- Duplicate publication is minimized by DB locking.
- Unit tests cover success, retryable failure, and max attempts.
- Unit tests cover stale lock recovery and bounded backoff.

### Phase 4: Inbox Schema And Listener

Goal: make Kafka consumption durable and idempotent.

Tasks:

- Add Liquibase migration for `inbox_events`.
- Register the migration in `changelog-master-version-2.0.yaml` with `errorIfMissing: true`.
- Add `InboxEvent` entity and repository.
- Add `InboxEventRecorder`.
- Refactor `ReviewCreatedKafkaConsumer` into a thin listener.
- Listener records Kafka event into `inbox_events`.
- Listener captures topic, partition, offset, key, and safe headers.
- Listener handles duplicate `(event_id, consumer_name)` as a safe skip.
- Listener does not call `AsyncReviewProcessingService` directly.
- Listener commits Kafka offset only after inbox insert or duplicate detection.

Acceptance criteria:

- Consuming a Kafka message inserts one inbox row.
- Consuming the same Kafka message twice does not create duplicate inbox rows.
- Kafka offset is committed only after durable inbox insert or duplicate detection.
- Failed inbox insert causes Kafka redelivery rather than message loss.

### Phase 5: Inbox Worker And Review Processing

Goal: process product-review events from inbox rows.

Tasks:

- Add `InboxEventWorker`.
- Add `ReviewCreatedInboxProcessor`.
- Route `review.created` rows to review processing.
- Load `ProductReview` by `reviewId`.
- Call existing `AsyncReviewProcessingService` behavior through a method that loads review text from PostgreSQL, not from Kafka payload.
- Preserve current delete-on-moderation-rejection behavior.
- Mark rows `PROCESSED`, `FAILED_RETRYABLE`, `FAILED_PERMANENT`, or `IGNORED`.
- Reclaim stale `IN_PROGRESS` rows after `stale-lock-timeout`.
- Persist failure status even when business processing throws.

Acceptance criteria:

- Kafka-enabled flow deletes rejected reviews like the current local async implementation.
- Duplicate inbox events do not duplicate side effects.
- Missing review rows are handled safely.
- Failed processing is visible in `inbox_events`.
- Tests prove retryable failures do not lose the inbox row.
- Tests prove failure status is committed even when review processing throws.
- Tests prove `ai.enabled=false` still results in a `PROCESSED` inbox row without calling real AI.

### Phase 6: Tests

Goal: prove reliability boundaries and fallback behavior.

Unit tests:

- `ReviewCreatedKafkaEventTest`
- `ReviewCreatedOutboxWriterTest`
- `KafkaOutboxPublisherTest`
- `InboxEventRecorderTest`
- `ReviewCreatedInboxProcessorTest`
- `InboxEventWorkerTest`
- `ReviewCreatedApplicationEventListenerTest`
- `ReviewCreatedEventContractTest`
- `EventProcessingPropertiesValidationTest`

Integration tests:

- Review creation writes outbox row when Kafka enabled.
- Review creation does not require Kafka broker availability.
- Outbox publisher publishes to Kafka and marks `PUBLISHED`.
- Kafka listener records inbox row.
- Kafka listener records topic, partition, offset, key, and event id.
- Inbox worker processes review-created event.
- Duplicate Kafka event is ignored by inbox uniqueness.
- Kafka disabled mode uses local listener and does not write outbox rows.
- Kafka-enabled mode does not run the local async listener.
- Kafka listener does not acknowledge a message when inbox insert fails.
- Kafka payload does not contain review text.
- Kafka payload and headers do not contain credentials, authorization headers, cookies, or review text.
- Kafka-enabled flow works with `AI_ENABLED=false`.
- Stale outbox and inbox locks are reclaimed.

Use Testcontainers Kafka for Kafka integration tests.

### Phase 7: Operational Visibility

Goal: make event flow debuggable.

Logging:

- `review.outbox.created`
- `kafka.outbox.publish.started`
- `kafka.outbox.publish.succeeded`
- `kafka.outbox.publish.failed`
- `kafka.inbox.recorded`
- `kafka.inbox.duplicate`
- `kafka.inbox.processing.started`
- `kafka.inbox.processing.succeeded`
- `kafka.inbox.processing.failed`

Useful future metrics:

- outbox pending count
- outbox failed permanent count
- inbox received count
- inbox failed permanent count
- publish latency
- processing latency
- consumer lag
- oldest pending outbox age
- oldest retryable inbox age

Do not add admin replay endpoints in the first PR. Manual database inspection is enough for v1.

### Manual Operations V1

Because v1 intentionally has no admin replay endpoints, operational recovery should be explicit and conservative.

Safe manual actions:

- Inspect `FAILED_PERMANENT` rows by `event_id`, `event_type`, `aggregate_id`, `attempt_count`, `last_error`, `created_at`, and `updated_at`.
- If the root cause is fixed and replay is safe, manually move a failed row back to a retryable state by setting `status = FAILED_RETRYABLE`, clearing `locked_by` and `locked_at`, clearing or preserving `last_error` according to the incident note, and setting `next_attempt_at = now()`.
- If an outbox row should never be published, set `status = CANCELLED`, clear `locked_by` and `locked_at`, and keep the row for audit.
- If an inbox row should never be processed, keep it as `FAILED_PERMANENT` in v1; do not delete it as a way to skip processing.

Manual replay cautions:

- Do not manually insert Kafka messages to bypass `outbox_events`; that bypasses the event id and audit trail.
- Do not reset `attempt_count` unless the operator deliberately wants to give the row a full retry budget again.
- Do not update `payload` by hand except to correct an incident with a reviewed SQL change.
- Do not delete failed rows until retention/cleanup tooling exists.

### Rollout And Rollback

Recommended rollout:

1. Merge schema, code, and migrations with `KAFKA_ENABLED=false`.
2. Deploy and verify the app still uses local review async processing.
3. Provision `iced-latte.review.created.v1` in Kafka through Vault-managed topic configuration.
4. Enable Kafka in one production-like environment with one active outbox worker and one inbox worker.
5. Create a review and verify `outbox_events -> Kafka -> inbox_events -> PROCESSED`.

Rollback guidance:

- If Kafka publishing or consumption is unhealthy, set `KAFKA_ENABLED=false` and restart the app. New reviews return to the local async fallback.
- Existing `outbox_events` and `inbox_events` rows should remain in PostgreSQL for later inspection or replay.
- Do not drop the outbox/inbox tables as a rollback step.
- If only publishing should pause, set `KAFKA_OUTBOX_WORKER_ENABLED=false`; review creation will continue and outbox rows will accumulate.
- If only business processing should pause, set `KAFKA_INBOX_WORKER_ENABLED=false`; Kafka messages can still be recorded into the inbox and processed later.

---

## Failure Semantics

### Review Transaction Fails

No review and no outbox row should commit.

### Kafka Is Down

Review creation still succeeds. Outbox row remains retryable.

### App Crashes After Outbox Row Is Locked

The stale-lock timeout must allow future workers to reclaim rows stuck in `IN_PROGRESS`.

### App Crashes After Inbox Row Is Locked

The stale-lock timeout must allow future workers to reclaim inbox rows stuck in `IN_PROGRESS`.

### Kafka Publish Succeeds But App Crashes Before Marking Published

The event may be published again later. This is acceptable because Kafka and the outbox worker provide at-least-once delivery. The inbox table prevents duplicate business processing.

### Kafka Delivers Duplicate Message

`inbox_events` unique `(event_id, consumer_name)` prevents duplicate processing for the same consumer.

### Review Was Already Deleted

Inbox processor should mark the row `IGNORED` and log that the review no longer exists.

### Moderation Rejects Review

Delete the review, refresh product aggregates, schedule summary update, mark inbox row `PROCESSED`.

### AI Is Disabled

The inbox processor should still process the row through the configured no-op moderation/summary services and mark the row `PROCESSED`. Kafka enablement must not require AI provider credentials.

### Moderation Provider Is Temporarily Unavailable

Classify as retryable if the current moderation behavior can distinguish transient provider failures. Otherwise, preserve current fallback behavior and document it in the processor test cases.

### Poison Message Cannot Be Processed

After `max_attempts`, mark the inbox row `FAILED_PERMANENT`. Do not keep retrying forever. Do not add a Kafka DLQ topic in v1.

### Outbox Row Is Permanently Unpublishable

After `max_attempts`, mark the outbox row `FAILED_PERMANENT`. Review creation has already succeeded, so the operational response is inspection, manual correction, or future admin replay tooling.

---

## Implementation Details To Confirm

These details should be confirmed during implementation, but the recommended defaults below are the intended v1 behavior unless code constraints force a change:

1. Exact package for shared event infrastructure: `common.event` or top-level `event`.
2. Exact Liquibase version folder and migration filenames.
3. Whether stale `IN_PROGRESS` rows are reclaimed directly by polling query or by a separate maintenance method.
4. Whether `last_error` should store full exception class plus safe message or safe message only.
5. Whether `actorId` should be populated from `userId` in `review.created`; this is useful for traceability but should be treated as private data.
6. Whether `correlationId` should be read from the current MDC/request context.
7. Whether manual ack is configured globally or through a review-specific listener container factory.
8. Whether outbox and inbox cleanup jobs belong in the first implementation or a follow-up PR.

Recommended defaults:

- Use manual Kafka acknowledgement for v1.
- Use top-level `event` package if architecture tests reject `common.event`.
- Store `error_class` separately only if needed; otherwise keep `last_error`.
- Leave `actorId` and `correlationId` nullable in v1.
- Treat `IGNORED` as terminal success for missing reviews in v1.
- Defer cleanup jobs to a follow-up PR, but create retention fields/config now.

---

## Future Work

- Add Kafka DLQ topics only if database-backed `FAILED_PERMANENT` rows are not enough.
- Add admin tooling for retrying or cancelling failed outbox/inbox rows.
- Add metrics and Grafana dashboards for outbox/inbox health.
- Add transactional outbox support for order/payment events.
- Consider Debezium later to stream `outbox_events` from PostgreSQL WAL to Kafka.
- Consider Schema Registry after multiple consumers or schema compatibility problems appear.
- Add Kafka ACL/SASL/TLS when multiple apps need separate topic permissions.

---

## Interview-Level Summary

The intended architecture is:

> Iced Latte uses a transactional outbox to reliably publish product-review events to Kafka after the product-review transaction commits. Kafka is optional for local contributors. In Kafka-enabled deployments, a polling outbox worker publishes `review.created` events and waits for broker acknowledgement before marking rows as published. Consumers use a durable inbox table, so Kafka listeners only record messages and commit offsets after durable handoff. Business processing runs from the inbox worker, making retries, duplicate handling, and permanent failures visible in PostgreSQL. The system is intentionally at-least-once, and idempotency is enforced through the inbox uniqueness boundary.
