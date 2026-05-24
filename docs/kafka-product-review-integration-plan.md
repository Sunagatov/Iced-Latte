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
- Existing serializer trusted-package config appears to reference `com.zufar.icedlatte.review.kafka`, but current classes live under `com.zufar.icedlatte.review.service.kafka`; this must be verified and fixed during implementation.
- Existing direct Kafka publishing should be replaced, not extended. The final Kafka-enabled path must be `ReviewCreatedEvent -> outbox_events -> Kafka -> inbox_events -> inbox worker`.

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
- Review creation still publishes the in-process `ReviewCreatedEvent` after commit.
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
| `status` | VARCHAR not null | See statuses below |
| `attempt_count` | INT not null default 0 | Publish attempts |
| `max_attempts` | INT not null default 10 | Limit before permanent failure |
| `next_attempt_at` | TIMESTAMPTZ not null | Retry scheduling |
| `locked_by` | VARCHAR nullable | Worker instance id |
| `locked_at` | TIMESTAMPTZ nullable | Lease timestamp |
| `published_at` | TIMESTAMPTZ nullable | Set after Kafka ack |
| `last_error` | TEXT nullable | Safe error message |
| `created_at` | TIMESTAMPTZ not null | Audit |
| `updated_at` | TIMESTAMPTZ not null | Audit |

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
INDEX (status, next_attempt_at)
INDEX (aggregate_type, aggregate_id)
INDEX (topic)
```

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
| `consumer_name` | VARCHAR not null | `iced-latte-review-ai` |
| `payload` | JSONB not null | Original event envelope |
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
INDEX (status, next_attempt_at)
INDEX (event_type)
INDEX (consumer_name, status)
```

The unique `(event_id, consumer_name)` constraint is the consumer idempotency boundary. Duplicate Kafka delivery must not process the same event twice for the same consumer.

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

Stale `IN_PROGRESS` rows must be reclaimable. A worker should treat rows as retryable again when:

```text
status = IN_PROGRESS
and locked_at < now() - stale-lock-timeout
```

Do not rely on JVM memory to track in-progress rows. Database state is the source of truth.

Use small batches in v1, for example:

```text
kafka.outbox.batch-size=25
kafka.outbox.poll-interval=5s
kafka.outbox.max-attempts=10
kafka.outbox.stale-lock-timeout=5m
```

The worker should be disabled unless `kafka.enabled=true`.

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

---

## Inbox Flow

### Kafka Listener

The Kafka listener should be thin.

It should:

1. Receive `ReviewCreatedKafkaEvent`.
2. Insert one `inbox_events` row with status `RECEIVED`.
3. If `(event_id, consumer_name)` already exists, treat it as duplicate and skip.
4. Commit Kafka offset after durable inbox insert or duplicate detection.

The listener must not run AI moderation directly.

Offset acknowledgement rule:

- The listener must not acknowledge or commit the Kafka offset before the inbox insert transaction commits.
- With Spring Kafka `ack-mode=record`, this is acceptable only if the listener method is transactional and the database transaction commits before the method returns.
- Manual acknowledgement is also acceptable, but then `Acknowledgment.acknowledge()` must be called only after successful inbox insert or duplicate detection.
- If the inbox insert fails, the Kafka record must be retried by Kafka rather than lost.

This is the critical durability boundary for consumption:

```text
Kafka record -> committed inbox row -> Kafka offset commit
```

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

If the review no longer exists when the event is processed, mark the inbox row `IGNORED` or `PROCESSED` with a clear log. This is acceptable because duplicate or stale events must be safe.

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

Stale `IN_PROGRESS` inbox rows must be reclaimable using `locked_at` and `kafka.inbox.stale-lock-timeout`.

The inbox worker must run business processing in a transaction that includes the inbox status update. If business processing fails, the worker must persist the failure status in a separate transaction or carefully structure the transaction so the failure marker is not rolled back with the failed business work.

Recommended approach:

- Mark row `IN_PROGRESS` in a short transaction.
- Run business processing in a new transaction.
- Mark `PROCESSED` in a short transaction after success.
- On exception, mark `FAILED_RETRYABLE` or `FAILED_PERMANENT` in a separate transaction.

This mirrors the Stripe webhook event recorder pattern already used elsewhere in the project.

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

Do not add a `PENDING_MODERATION` review status in this first Kafka integration. That would be a separate product behavior change.

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

### Shared Event Infrastructure

Keep infrastructure app-local, not in Vault.

Suggested package:

```text
com.zufar.icedlatte.common.event
```

Suggested files:

```text
common/event/entity/OutboxEvent.java
common/event/entity/OutboxEventStatus.java
common/event/entity/InboxEvent.java
common/event/entity/InboxEventStatus.java
common/event/repository/OutboxEventRepository.java
common/event/repository/InboxEventRepository.java
common/event/service/OutboxEventWriter.java
common/event/service/KafkaOutboxPublisher.java
common/event/service/InboxEventRecorder.java
common/event/service/InboxEventWorker.java
common/event/config/EventProcessingProperties.java
```

If `common.event` violates the current architecture rules, use an app-owned infrastructure package such as:

```text
com.zufar.icedlatte.event
```

Do not put product-review business logic in this shared infrastructure package.

Spring Modulith note:

- The `review` module currently allows `common :: *`, so shared event infrastructure under `common` is the preferred direction.
- If new `common.event` subpackages are added, update the controlled named-interface snapshot test if Spring Modulith exposes a new named interface.
- `common.event` must not depend on `review`, `product`, `order`, `payment`, or any other feature module.
- Product-review-specific mapping belongs in `review.service.kafka`, not in `common.event`.

### Product Review Event Files

Suggested package:

```text
com.zufar.icedlatte.review.service.kafka
```

Suggested files:

```text
review/dto/ReviewCreatedEvent.java
review/service/kafka/ReviewCreatedKafkaEvent.java
review/service/kafka/ReviewCreatedOutboxWriter.java
review/service/kafka/ReviewCreatedOutboxEventListener.java
review/service/kafka/ReviewCreatedKafkaListener.java
review/service/kafka/ReviewCreatedInboxProcessor.java
```

Refactor existing direct classes:

- Replace `ReviewCreatedKafkaPublisher` with outbox writing and generic outbox publishing.
- Refactor `ReviewCreatedKafkaConsumer` so it records to `inbox_events` only.
- Keep `ReviewCreatedApplicationEventListener` only for `kafka.enabled=false`.
- Keep `AsyncReviewProcessingService` as the business service used by both local fallback and Kafka inbox processing.
- Remove `text` from `ReviewCreatedKafkaEvent.Payload`; keep text only in the internal `ReviewCreatedEvent` if the local fallback still needs it.

---

## Configuration

Suggested application properties:

```yaml
kafka:
  enabled: ${KAFKA_ENABLED:false}
  topics:
    review-created: ${KAFKA_TOPIC_REVIEW_CREATED:iced-latte.review.created.v1}
  consumer-groups:
    review-ai: ${KAFKA_CONSUMER_GROUP_REVIEW_AI:iced-latte-review-ai}
  outbox:
    enabled: ${KAFKA_OUTBOX_ENABLED:${KAFKA_ENABLED:false}}
    batch-size: ${KAFKA_OUTBOX_BATCH_SIZE:25}
    poll-interval: ${KAFKA_OUTBOX_POLL_INTERVAL:5s}
    max-attempts: ${KAFKA_OUTBOX_MAX_ATTEMPTS:10}
    stale-lock-timeout: ${KAFKA_OUTBOX_STALE_LOCK_TIMEOUT:5m}
    retention: ${KAFKA_OUTBOX_RETENTION:30d}
  inbox:
    enabled: ${KAFKA_INBOX_ENABLED:${KAFKA_ENABLED:false}}
    batch-size: ${KAFKA_INBOX_BATCH_SIZE:25}
    poll-interval: ${KAFKA_INBOX_POLL_INTERVAL:5s}
    max-attempts: ${KAFKA_INBOX_MAX_ATTEMPTS:10}
    stale-lock-timeout: ${KAFKA_INBOX_STALE_LOCK_TIMEOUT:5m}
    retention: ${KAFKA_INBOX_RETENTION:30d}
```

Kafka disabled mode must remain the default in:

```text
.env.example
src/main/resources/application-dev.yaml
```

Production-like deployments can set:

```text
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=kafka:19092
```

---

## Implementation Phases

### Phase 1: Event Contract And Configuration Cleanup

Goal: make the event contract and configuration explicit before schema work.

Tasks:

- Verify `ReviewCreatedKafkaEvent` package names match Spring Kafka JSON deserializer config.
- Update `docs/events/asyncapi.yaml` if needed.
- Update `review-created-event.schema.json` to remove `text` from payload.
- Update `ReviewCreatedKafkaEvent.Payload` to remove `text`.
- Ensure `.env.example` contains Kafka defaults.
- Ensure `application-dev.yaml` disables Kafka.
- Ensure `application-prod.yaml` can opt in via env vars.
- Keep local `docker-compose.yml` backend default at `KAFKA_ENABLED=false`; production/Vault deployment config owns enabling Kafka.
- Verify the topic exists in Vault `infra/kafka/topics.yml` and is created before enabling Kafka in Iced Latte.
- Decide whether listener offset commits use `ack-mode=record` with transactional listener method or explicit manual acknowledgements.

Acceptance criteria:

- App starts with Kafka disabled.
- Event schema contains only `reviewId` and `productId` in payload.
- Kafka topic and consumer group names are config-driven.
- Missing Kafka topic does not break review creation; it leaves outbox rows retryable.

### Phase 2: Outbox Schema And Writer

Goal: reliably record product-review events in PostgreSQL.

Tasks:

- Add Liquibase migration for `outbox_events`.
- Add `OutboxEvent` entity and repository.
- Add `OutboxEventWriter`.
- Add `ReviewCreatedOutboxWriter`.
- Add `ReviewCreatedOutboxEventListener` with `TransactionPhase.BEFORE_COMMIT`.
- Keep `ProductReviewManager.create(...)` publishing `ReviewCreatedEvent`; avoid Kafka-specific branching in the business service unless implementation constraints force it.
- Ensure Kafka-disabled mode still uses local `ReviewCreatedEvent` fallback.

Acceptance criteria:

- When Kafka is enabled, creating a review inserts one outbox row.
- If the review transaction rolls back, no outbox row remains.
- If the outbox insert fails, the review transaction rolls back.
- When Kafka is disabled, current local async behavior still works.
- Unit tests prove Kafka-enabled and Kafka-disabled listeners are mutually exclusive.

### Phase 3: Outbox Publisher Worker

Goal: publish outbox rows to Kafka reliably.

Tasks:

- Add `KafkaOutboxPublisher`.
- Poll eligible `PENDING` and `FAILED_RETRYABLE` rows.
- Use row locking to avoid two app instances publishing the same row concurrently.
- Use `FOR UPDATE SKIP LOCKED` or equivalent PostgreSQL row-locking behavior.
- Publish to Kafka with configured topic and partition key.
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
- Add `InboxEvent` entity and repository.
- Add `InboxEventRecorder`.
- Refactor `ReviewCreatedKafkaConsumer` into a thin listener.
- Listener records Kafka event into `inbox_events`.
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
- Call existing `AsyncReviewProcessingService` behavior.
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

Integration tests:

- Review creation writes outbox row when Kafka enabled.
- Review creation does not require Kafka broker availability.
- Outbox publisher publishes to Kafka and marks `PUBLISHED`.
- Kafka listener records inbox row.
- Inbox worker processes review-created event.
- Duplicate Kafka event is ignored by inbox uniqueness.
- Kafka disabled mode uses local listener and does not write outbox rows.
- Kafka-enabled mode does not run the local async listener.
- Kafka listener does not acknowledge a message when inbox insert fails.
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

Inbox processor should mark the row `IGNORED` or `PROCESSED` and log that the review no longer exists.

### Moderation Rejects Review

Delete the review, refresh product aggregates, schedule summary update, mark inbox row `PROCESSED`.

### Moderation Provider Is Temporarily Unavailable

Classify as retryable if the current moderation behavior can distinguish transient provider failures. Otherwise, preserve current fallback behavior and document it in the processor test cases.

### Poison Message Cannot Be Processed

After `max_attempts`, mark the inbox row `FAILED_PERMANENT`. Do not keep retrying forever. Do not add a Kafka DLQ topic in v1.

### Outbox Row Is Permanently Unpublishable

After `max_attempts`, mark the outbox row `FAILED_PERMANENT`. Review creation has already succeeded, so the operational response is inspection, manual correction, or future admin replay tooling.

---

## Open Implementation Decisions

These decisions should be confirmed during implementation:

1. Exact package for shared event infrastructure: `common.event` or top-level `event`.
2. Exact Liquibase version folder and migration filenames.
3. Whether stale `IN_PROGRESS` rows are reclaimed directly by polling query or by a separate maintenance method.
4. Whether `last_error` should store full exception class plus safe message or safe message only.
5. Whether `actorId` should be populated from `userId` in `review.created`; this is useful for traceability but should be treated as private data.
6. Whether `correlationId` should be read from the current MDC/request context.
7. Whether Kafka listener offset acknowledgement should remain `ack-mode=record` or move to manual ack for clearer control.
8. Whether `IGNORED` should be terminal success for missing reviews, or whether missing reviews should be treated as `PROCESSED`.
9. Whether outbox and inbox cleanup jobs belong in the first implementation or a follow-up PR.

Recommended defaults:

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
