# Avatar Upload AWS Lambda Plan

This plan describes a production-only AWS Lambda avatar pipeline while keeping
the current local/backend upload flow intact for contributors.

## Current Flow

The current avatar upload path is synchronous and backend-owned:

```text
Frontend ImageUpload
  -> POST /api/v1/users/avatar
  -> UserAvatarEndpoint
  -> UserAvatarUploader
  -> FileStorageService
  -> S3-compatible storage / MinIO
  -> file_metadata
```

The backend currently owns:

- authentication through the current user context
- Cloudflare Turnstile validation when avatar protection is enabled
- content-type normalization and magic-byte validation
- multipart upload limits and file-upload rate limiting
- object-storage write
- `file_metadata` replacement for the user's current avatar
- old-object deletion through the file deletion outbox

The frontend uploads the file through the generated user API and displays the
avatar through the same-origin `/api/user/avatar` route, which asks the backend
for the avatar link and streams the storage object.

Important current constraints to preserve:

- localhost and tests must not need AWS Lambda, SQS, CloudWatch, or real AWS S3
- the existing `avatarLink` field is already consumed by the frontend profile
  flow
- avatar reads are intentionally same-origin through the frontend route, so the
  browser does not need to understand storage-provider details
- avatar upload is classified as a file-upload rate-limit category

## Problem

The current flow is simple and good for local development, but it makes Spring
Boot carry file bytes and synchronous storage work during the user request. It
also stores the avatar as a single current file rather than a lifecycle with a
pending upload, processing result, and active processed avatar.

For production, AWS Lambda is a better fit for image normalization work:

- decode and re-encode the image
- strip EXIF/GPS metadata
- normalize orientation
- crop to square
- generate WebP variants
- reject broken or oversized images after decode
- emit processing metrics and failure events

## Design Goals

- Keep the existing backend upload path for localhost and tests.
- Keep Spring Boot as the source of truth for identity, authorization, and DB
  state.
- Keep Lambda as a worker, not a second public backend.
- Avoid writing AWS-specific orchestration into generic `filestorage` APIs.
- Avoid breaking the existing `avatarLink` contract.
- Use immutable object keys so CDN caching does not depend on frequent
  invalidation.
- Preserve the old active avatar until a new upload is fully processed.
- Keep avatar objects private by default; expose only short-lived upload/read
  permissions or same-origin application routes.
- Make every cross-system message idempotent and safe to replay.
- Treat upload intent creation as retryable, because browsers and proxies can
  retry requests after timeouts.

## Non-Goals

- Do not migrate the whole backend or avatar API to Lambda.
- Do not make Lambda write directly to the main production database in the first
  version.
- Do not require AWS Lambda, SQS, or CloudWatch for local development.
- Do not use S3 Object Lambda for this project.
- Do not remove the existing `POST /api/v1/users/avatar` endpoint.

## Upload Modes

Use a feature flag to select the avatar upload strategy.

```yaml
avatar:
  upload-mode: backend
```

Recommended values:

| Mode | Environment | Behavior |
| --- | --- | --- |
| `backend` | local, tests, fallback | Current `POST /api/v1/users/avatar` flow. |
| `presigned` | production AWS experiment | Upload intent, direct S3 upload, Lambda processing, SQS completion. |

Local `.env.example` should keep:

```text
AVATAR_UPLOAD_MODE=backend
AWS_ENDPOINT_URL=http://localhost:9000
AWS_PRODUCT_BUCKET=iced-latte-products
AWS_USER_BUCKET=iced-latte-users
```

Production values belong in Vault-managed `.env.prod` and AWS IaC, not in this
source repository.

Proposed production config surface:

```text
AVATAR_UPLOAD_MODE=backend|presigned
AVATAR_UPLOAD_PRESIGNED_URL_TTL=PT5M
AVATAR_UPLOAD_MAX_BYTES=5242880
AVATAR_UPLOAD_MAX_PIXELS=12000000
AVATAR_UPLOAD_PROCESSING_TIMEOUT=PT10M
AVATAR_UPLOAD_INCOMING_BUCKET=iced-latte-users
AVATAR_UPLOAD_PROCESSED_BUCKET=iced-latte-users
AVATAR_UPLOAD_COMPLETION_QUEUE_URL=https://sqs...
```

Keep these as avatar-owned properties instead of reusing generic file-storage
properties for lifecycle behavior. Generic object storage should still only
know how to read, write, delete, and sign storage objects.

`backend` mode is the compatibility contract. If AWS mode has an incident,
rolling back should mean changing config back to `backend`, redeploying, and
leaving the current endpoint behavior intact.

The first backend implementation should fail closed for `presigned` mode when
AWS resources are not configured. Do not silently fall back to partial direct
upload behavior after an upload intent has been created.

## Target Production Flow

```text
Frontend
  -> POST /api/v1/users/avatar/uploads
  -> Spring Boot validates auth, Turnstile, rate limit, and requested content
     type/size
  -> Spring Boot creates avatar upload intent
  -> Spring Boot returns a short-lived presigned S3 upload form or PUT URL
  -> Frontend uploads bytes directly to S3
  -> Frontend may notify backend that upload finished, or backend waits for
     Lambda/SQS completion as the first proof of upload
  -> S3 ObjectCreated event invokes Lambda
  -> Lambda processes the image and writes processed variants
  -> Lambda sends completion message to SQS
  -> Spring Boot consumes SQS completion
  -> Spring Boot idempotently activates the processed avatar
  -> Frontend refreshes profile/avatar
```

Spring Boot remains the state owner. Lambda does not decide which avatar is
active; it only reports a processing result.

Prefer presigned POST for the browser upload when possible because its policy
can constrain the target key, content type, and content-length range. Presigned
PUT is still acceptable for a first implementation, but then Lambda must enforce
size and type limits after upload and the S3 key must be generated only by the
backend.

Direct browser-to-S3 upload also requires Vault-owned S3 CORS rules for the
production frontend origin. Keep those rules narrow:

- allow only the frontend origins that need avatar upload
- allow only the required method (`POST` or `PUT`)
- allow only required headers
- expose only response headers the frontend needs, such as `ETag` or checksum
  headers if the UX uses them
- keep the bucket private
- do not allow arbitrary public reads

All AWS resources for the first version should live in one AWS Region to avoid
cross-region surprises. S3 bucket notifications, Lambda, SQS, and DLQ should be
configured together in Vault IaC.

## Object Keys

Avoid the current single-key shape for production AWS processing:

```text
user-avatar-{userId}.png
```

Use immutable, versioned keys:

```text
avatars/incoming/{userId}/{uploadId}/source
avatars/processed/{userId}/{uploadId}/avatar-96.webp
avatars/processed/{userId}/{uploadId}/avatar-192.webp
avatars/processed/{userId}/{uploadId}/avatar-384.webp
```

The backend, not the frontend, creates `userId`, `uploadId`, bucket, and key.
The frontend must not be able to choose arbitrary storage keys.

Store upload metadata on the incoming object when useful:

```text
x-amz-meta-upload-id: {uploadId}
x-amz-meta-user-id: {userId}
x-amz-meta-requested-content-type: image/png
x-amz-meta-avatar-upload-version: 1
```

Do not put personal data such as email, display name, or OAuth identifiers in
object keys or metadata. `userId` and `uploadId` are enough for correlation.

When feasible, include an upload checksum condition/header. This is mainly a
data-integrity guardrail, not the image-validity check; Lambda still has to
decode and validate the image itself.

Benefits:

- no overwrite races
- old avatar remains active while new avatar is processing
- failed processing does not break the profile photo
- CDN can cache processed avatar URLs aggressively
- cleanup can remove abandoned or old upload versions later

Retention policy:

- incoming originals should be short-lived and removed by S3 lifecycle or a
  cleanup job after processing or expiry
- processed active avatars should live while active
- superseded processed avatars can be retained briefly for rollback/debugging,
  then deleted by cleanup
- failed uploads should retain only safe metadata and a failure code after the
  cleanup window
- deletion should remove the current processed avatar and invalidate unfinished
  uploads for that user

## Backend Data Model

Do not overload `file_metadata` with processing state. That table is a generic
storage index used by multiple features.

Add a user-owned table for avatar upload lifecycle:

```text
user_avatar_upload
  id UUID primary key
  user_id UUID not null
  status VARCHAR not null
  original_bucket VARCHAR not null
  original_key VARCHAR not null
  processed_bucket VARCHAR
  processed_key VARCHAR
  content_type VARCHAR not null
  original_size_bytes BIGINT
  processed_size_bytes BIGINT
  image_width INT
  image_height INT
  sha256 VARCHAR
  client_idempotency_key VARCHAR
  failure_code VARCHAR
  failure_message VARCHAR
  active BOOLEAN not null default false
  created_at TIMESTAMPTZ not null
  uploaded_at TIMESTAMPTZ
  processed_at TIMESTAMPTZ
  activated_at TIMESTAMPTZ
  superseded_at TIMESTAMPTZ
  expires_at TIMESTAMPTZ not null
```

Recommended constraints and indexes:

```text
index user_avatar_upload_user_created_at_idx on (user_id, created_at desc)
unique user_avatar_upload_original_key_idx on (original_bucket, original_key)
unique user_avatar_upload_processed_key_idx on (processed_bucket, processed_key)
unique user_avatar_upload_user_active_idx on (user_id) where active = true
unique user_avatar_upload_user_idempotency_idx on (user_id, client_idempotency_key)
check expires_at > created_at
check status in (...)
```

Activation must be an explicit transaction in the user feature: mark the
selected upload `active=true`, set `activated_at`, and retire any older active
upload for the same user in the same transaction.

Recommended statuses:

```text
PENDING_UPLOAD
UPLOADED
PROCESSING
READY
FAILED
EXPIRED
SUPERSEDED
```

State transition rules:

```text
PENDING_UPLOAD -> UPLOADED      when the incoming object exists
UPLOADED       -> PROCESSING    when Lambda starts or claims the object
PENDING_UPLOAD -> READY         allowed when completion is the first backend
                                signal that upload and processing happened
PROCESSING     -> READY         when backend validates completion
READY          -> SUPERSEDED    when a newer avatar becomes active
PROCESSING     -> FAILED        when Lambda reports a terminal failure
PENDING_UPLOAD -> FAILED        allowed when failure is the first backend
                                signal from Lambda
PENDING_UPLOAD -> EXPIRED       when upload URL expires unused
UPLOADED       -> EXPIRED       when no processing result arrives before TTL
PROCESSING     -> EXPIRED       when processing is stuck beyond the retry window
```

Do not allow:

```text
FAILED -> READY
EXPIRED -> READY
SUPERSEDED -> active
older upload -> active when a newer ready upload exists
deleted avatar -> reactivated by a late Lambda completion
```

`UPLOADED` and `PROCESSING` are useful if the backend receives explicit
progress signals. If the first backend-visible event is the SQS completion from
Lambda, it is acceptable to move directly from `PENDING_UPLOAD` to `READY` or
`FAILED`.

The active avatar can stay represented through existing avatar resolution for
phase 1. Later, either add explicit active-avatar fields to the user-owned model
or update `file_metadata` only when a processed avatar becomes active.

If `file_metadata` remains the read path, activation should write only the
selected processed object into `file_metadata`. Incoming originals and non-active
processed variants should be tracked by `user_avatar_upload` and cleanup logic,
not by the generic current-file resolver.

## Public API Sketch

Keep the existing endpoint:

```text
POST /api/v1/users/avatar
GET /api/v1/users/avatar
DELETE /api/v1/users/avatar
```

Add upload-intent endpoints only for `presigned` mode:

```text
POST /api/v1/users/avatar/uploads
GET  /api/v1/users/avatar/uploads/{uploadId}
```

The new endpoints belong in `src/main/resources/api-specs/user-openapi.yaml`.
Backend Java interfaces are generated by the Maven OpenAPI generator, and the
frontend generated user client is produced by Orval in the frontend repo. Any
Phase 2 contract change should include both backend OpenAPI verification and the
frontend generated-client update.

Example create response:

```json
{
  "uploadId": "8c85a55b-5f8e-40ec-b66e-6e941d0d7d59",
  "status": "PENDING_UPLOAD",
  "upload": {
    "method": "POST",
    "url": "https://...",
    "fields": {
      "key": "avatars/incoming/...",
      "policy": "...",
      "x-amz-signature": "..."
    }
  },
  "expiresAt": "2026-06-25T12:00:00Z"
}
```

If using presigned PUT instead, return:

```json
{
  "uploadId": "8c85a55b-5f8e-40ec-b66e-6e941d0d7d59",
  "status": "PENDING_UPLOAD",
  "upload": {
    "method": "PUT",
    "url": "https://...",
    "headers": {
      "Content-Type": "image/png"
    }
  },
  "expiresAt": "2026-06-25T12:00:00Z"
}
```

Do not expose raw AWS credentials. The frontend should receive only the
single-object presigned upload data and required headers or form fields.

The status endpoint should return a stable shape:

```json
{
  "uploadId": "8c85a55b-5f8e-40ec-b66e-6e941d0d7d59",
  "status": "READY",
  "avatarLink": "/api/user/avatar?v=8c85a55b-5f8e-40ec-b66e-6e941d0d7d59",
  "failureCode": null,
  "expiresAt": "2026-06-25T12:00:00Z"
}
```

Keep new fields additive on profile responses. If `avatarStatus` is later added
to the user profile payload, make it optional so existing clients do not break.

HTTP behavior:

- upload intent create requires authentication
- upload intent create should accept an `Idempotency-Key` header so frontend
  retries do not create multiple competing upload intents
- upload intent create should reuse avatar Turnstile when avatar protection is
  enabled
- upload intent create should use the file-upload rate-limit category
- status reads must verify the upload belongs to the current user
- expired or unknown uploads should not reveal whether another user's upload ID
  exists
- repeated create calls with the same `Idempotency-Key` and user should return
  the same unexpired intent, or a clear retry-required response if the old
  intent can no longer be used

## Lambda Contract

Lambda receives an S3 object-created event for the incoming prefix. It should:

1. Validate the key shape and extract `userId` and `uploadId`.
2. Download the incoming object.
3. Verify object metadata matches the upload intent shape when available.
4. Verify checksum/ETag expectations when the chosen upload method supports it.
5. Decode the image and enforce max pixels/dimensions.
6. Strip metadata and normalize orientation.
7. Generate processed WebP variants.
8. Write variants to the processed prefix.
9. Send an SQS completion message.

Lambda must defend against decompression bombs and malformed images. The 5 MB
multipart limit in backend mode is not enough for direct upload mode because a
small compressed image can expand into huge pixel memory during decode.

Implementation note: package Lambda with an image-processing library that is
comfortable in Lambda. Sharp on Node.js or a small Java image library are both
reasonable. Avoid ImageMagick-style shells unless the deployment image, patching,
and CVE management are explicitly owned in Vault.

Example completion message:

```json
{
  "eventType": "AvatarProcessed",
  "version": 1,
  "userId": "3e5934d2-6e97-4e42-9a75-fc2adcc53f3a",
  "uploadId": "8c85a55b-5f8e-40ec-b66e-6e941d0d7d59",
  "status": "READY",
  "processedBucket": "iced-latte-users",
  "processedKey": "avatars/processed/3e5934d2-6e97-4e42-9a75-fc2adcc53f3a/8c85a55b-5f8e-40ec-b66e-6e941d0d7d59/avatar-384.webp",
  "contentType": "image/webp",
  "width": 384,
  "height": 384,
  "sha256": "..."
}
```

For failures, Lambda should send a failure message when possible and rely on DLQ
for unhandled failures.

Failure messages should use stable machine-readable codes, for example:

```text
INVALID_IMAGE
UNSUPPORTED_CONTENT_TYPE
IMAGE_TOO_LARGE
DECODE_FAILED
PROCESSING_TIMEOUT
```

## Queue Choice

Use SQS Standard first unless ordering problems become real. The backend must be
correct under duplicate and out-of-order messages anyway, because S3, Lambda,
and SQS integrations can retry. If ordering by user becomes valuable later,
consider SQS FIFO with `userId` as the message group ID, but do not depend on
FIFO as the only correctness mechanism.

The backend currently has AWS SDK usage for S3 and CloudFront. Phase 3 will need
an explicit SQS dependency/configuration if Spring Boot consumes completion
messages directly. Keep that dependency conditional so `backend` mode and local
startup do not need SQS configuration.

## Backend Completion Handling

Spring Boot should consume completion messages and activate avatars
idempotently:

- validate message schema
- verify `uploadId` exists and belongs to `userId`
- ignore duplicate completions
- ignore stale completions if a newer upload is already active or pending
- ignore completions for uploads deleted or expired after Lambda started
- mark upload `READY` or `FAILED`
- update active avatar metadata only after successful processing
- set exactly one active avatar per user
- enqueue cleanup for replaced processed objects
- record failure codes without exposing internal stack traces to the frontend

Out-of-order completion must be handled. If upload B is newer than upload A,
and Lambda finishes A after B, A must not replace B.

The completion consumer can be implemented either as:

- a backend SQS poller, if the production backend is allowed AWS SQS access; or
- a Vault-managed bridge that forwards validated completion events to a private
  backend endpoint.

Prefer the backend SQS poller first because it keeps DB activation inside the
application and avoids a public Lambda callback endpoint.

If a private backend callback is used later, it must be authenticated with a
machine credential owned in Vault and rate-limited separately. Do not expose a
public unauthenticated Lambda completion endpoint.

Late completion after delete is a required test case. If a user deletes their
avatar while Lambda is still processing a prior upload, the completion message
must not reactivate that avatar.

## Frontend Behavior

`backend` mode:

- keep the current `uploadImage(file)` behavior
- upload to `POST /api/v1/users/avatar`
- refresh user data after success

`presigned` mode:

- create upload intent
- upload directly to the returned presigned URL using a plain browser request,
  not the normal authenticated API proxy/mutator
- call S3 with `credentials: "omit"` so cookies are not sent accidentally
- show the local preview immediately
- poll upload status or refresh user data until ready
- keep the old avatar visible if processing fails
- handle upload URL expiry by requesting a new upload intent
- show a generic failure message for failed processing and let the user retry
- avoid sending application `Authorization`, refresh-token, or API proxy headers
  to S3

The existing local preview and avatar revision behavior can remain.

Serving processed avatars:

- keep using the same-origin `/api/user/avatar` route for the first production
  version
- let the backend return a short-lived read URL or public CDN URL internally to
  that route
- do not make the browser depend on raw S3 object URLs for private avatars
- only consider direct CloudFront avatar URLs later, after cache and privacy
  requirements are clear

Deletion behavior:

- deleting an avatar must deactivate the active upload and remove current avatar
  metadata
- deleting an avatar must also invalidate or mark obsolete any pending uploads
  for the same user, so late completions cannot reactivate the deleted avatar
- cleanup must delete the active processed object and any retained variants
- old incoming originals should be expired by lifecycle rules or cleanup jobs
- delete should stay idempotent when objects are already gone

## Vault / Production Ownership

Vault should own:

- AWS IaC for S3, Lambda, SQS, DLQ, IAM, CloudWatch alarms, and budget controls
- S3 CORS rules for direct browser upload
- S3 lifecycle rules for incoming and old processed objects
- least-privilege IAM roles for backend upload signing, Lambda object access,
  and SQS completion publishing/consuming
- production env variables
- Lambda deployment workflow
- operational runbooks and smoke checks
- AWS budget/anomaly controls for the learning stack

Backend source should own:

- upload mode config shape
- upload intent API and OpenAPI contract
- user-owned avatar lifecycle model
- SQS completion consumer
- local/backend upload strategy
- local fake/stub implementation for presigned mode tests when useful
- conditional AWS SQS configuration if the backend consumes completion messages

Frontend source should own:

- mode-aware upload client
- direct-to-S3 upload UX
- status polling or refresh behavior
- retry behavior for expired upload intents
- generated user API client changes after backend OpenAPI updates

Frontend config should default to the existing backend flow:

```text
NEXT_PUBLIC_AVATAR_UPLOAD_MODE=backend|presigned
```

For production, Vault/runtime config should set this together with backend
`AVATAR_UPLOAD_MODE`. If frontend and backend modes disagree, the frontend
should fail visibly and keep the existing avatar rather than attempting a mixed
upload flow.

## Observability

Track at least:

- upload intents created
- uploads expired
- direct S3 upload failures observed by the frontend
- Lambda processing success/failure
- processing duration
- original and processed byte sizes
- SQS completion lag
- DLQ message count
- avatar activation success/failure

Alert on:

- DLQ depth greater than zero
- high processing failure rate
- uploads stuck in `PROCESSING`
- sudden increase in rejected uploads
- AWS spend anomaly for the avatar pipeline

Every log/event across backend, Lambda, and SQS should carry `uploadId` and
`userId` where safe. User IDs are already application identifiers, but do not log
raw image bytes, presigned URLs, or secret material.

## Security Checklist

- Presigned upload expires quickly.
- Presigned upload is scoped to exactly one generated key.
- Presigned POST policy constrains content-length range when POST is used.
- Upload intent creation is idempotent per user and `Idempotency-Key`.
- Frontend cannot choose bucket or key.
- Frontend direct-to-S3 upload does not include app auth headers.
- Bucket remains private.
- S3 CORS is restricted to required origins, methods, and headers.
- Lambda validates decoded image type, dimensions, and pixel count.
- Lambda strips EXIF/GPS metadata.
- Object keys and metadata avoid personal data beyond `userId` and `uploadId`.
- Backend validates SQS message schema before using it.
- Backend validates `uploadId` belongs to `userId`.
- Backend ignores duplicate, stale, expired, or out-of-order completions.
- Backend ignores completions for uploads invalidated by avatar deletion.
- Frontend never receives AWS credentials.
- Presigned URLs are not logged.
- Machine-to-machine callbacks, if added, are authenticated and rate-limited.

## Local Testing Strategy

Local development should continue to use `backend` mode with MinIO. Tests for
the AWS pipeline should use boundaries instead of real AWS:

- unit-test upload intent creation and key generation
- unit-test state transitions
- unit-test duplicate and stale completion handling
- unit-test idempotent upload-intent creation
- unit-test delete followed by late completion
- integration-test existing `POST /api/v1/users/avatar` behavior unchanged
- contract-test OpenAPI generated frontend client shape
- run frontend `npm run api:generate` / `npm run api:check` when API specs
  change
- optionally use LocalStack only in a separate integration profile, not in the
  default local run path

## Phase 2 Acceptance Criteria

Phase 2 is ready to hand to Vault/AWS work only when:

- local backend startup needs no Lambda, SQS, CloudWatch, or real AWS S3
- existing `POST /api/v1/users/avatar` tests still pass
- upload-intent endpoints are disabled or fail closed unless `presigned` mode is
  explicitly configured
- upload intent creation is idempotent for the same user and `Idempotency-Key`
- state-transition tests cover duplicate, stale, expired, failed, and
  delete-after-upload cases
- OpenAPI generation is clean in the backend
- frontend Orval generation is updated and checked when the API contract changes
- no AWS credentials, presigned URLs, or secret values are logged

## Open Decisions

Recommended defaults before coding:

| Decision | Recommendation |
| --- | --- |
| Browser upload method | Presigned POST first, because it gives stronger policy controls. |
| Backend completion path | Backend SQS poller first, not public Lambda callback. |
| Lambda runtime | Node.js with Sharp unless Java packaging is simpler for the team. |
| Read path in phase 1 | Keep existing `avatarLink` / `/api/user/avatar` behavior. |
| Frontend upload-finished notify | Optional; do not depend on it for correctness. |
| Local AWS simulation | Keep default local path simple; add LocalStack only in a separate profile if needed. |

## Rollout Plan

### Phase 1: Design and Local Safety

- Add this plan.
- Keep `POST /api/v1/users/avatar` unchanged.
- Add config proposal for `AVATAR_UPLOAD_MODE=backend`.
- Do not add AWS runtime dependency to local development.

### Phase 2: Backend Contracts

- Add upload intent OpenAPI contract.
- Add `user_avatar_upload` migration and user-owned service classes.
- Add backend strategy selection for `backend` vs `presigned`.
- Keep SQS/Lambda integration out of the default local profile.
- Add key-generation and state-transition tests.
- Add tests for stale completion, duplicate completion, expired upload, and
  failed processing.
- Add tests for `Idempotency-Key` retry behavior and delete-vs-late-completion
  behavior.
- Keep the presigned implementation stubbed or disabled until Vault AWS
  resources exist.
- Regenerate backend OpenAPI code and frontend Orval client when the contract is
  ready.

### Phase 3: AWS Worker

- Add Vault-owned AWS IaC.
- Add private S3 bucket/prefixes and narrow CORS.
- Implement Lambda image processor.
- Add SQS completion queue and DLQ.
- Add backend SQS dependency/config only if using direct backend consumption.
- Add CloudWatch alarms.
- Add budget/anomaly guardrails.
- Keep production flag off until end-to-end smoke tests pass.

### Phase 4: Frontend Presigned Flow

- Add mode-aware avatar upload client.
- Add direct-to-S3 presigned POST first, or PUT if the backend implementation
  deliberately accepts the weaker upload policy controls.
- Add status polling or profile refresh.
- Keep current upload path available for local mode.

### Phase 5: Cleanup

- Add EventBridge/Lambda or backend scheduled cleanup for expired uploads.
- Add S3 lifecycle rules for abandoned incoming objects.
- Document rollback to `AVATAR_UPLOAD_MODE=backend`.

## Recommended Next Step

Implement phase 2 only after reviewing this plan. The first code change should
be the backend upload-intent contract and data model, behind a config flag, while
leaving the current avatar upload behavior untouched.
