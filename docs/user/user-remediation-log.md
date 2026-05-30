# Backend Remediation Log

This document explains the security, user, address, avatar, and file-cleanup
issues found and fixed during this chat.

Security-specific issues from the same work are also documented in
`docs/security/security-remediation-log.md`. This file repeats those findings in
shorter form so this log is self-contained for the full chat history.

The goal is not only to say "what changed", but to make each bug understandable
for someone who is still learning Spring, JPA, transactions, and backend
consistency.

Each issue uses this structure:

- What this feature is for
- How the old system behaved
- Why that behavior was a bug
- A simple example
- What the fix changed

## 1. Setting an Already-Default Delivery Address Could Remove the Default

### What This Feature Is For

Each user can have multiple delivery addresses, but only one should be the
default address.

The default address is the address the system should automatically choose when
the user starts an order.

### How the Old System Behaved

The old `setDefault(...)` method loaded the selected address, cleared every
default address for the user, then set the selected entity to default again:

```text
var entity = addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(...);
addressRepository.clearDefaultForUser(userId);
entity.setDefault(true);
return converter.toDto(addressRepository.save(entity));
```

The repository method used a bulk JPQL update:

```text
@Modifying
@Query("UPDATE DeliveryAddressEntity a SET a.isDefault = false WHERE a.user.id = :userId")
void clearDefaultForUser(UUID userId);
```

### Why That Was a Bug

JPQL bulk updates go directly to the database. They do not update the entity
object that is already loaded in the JPA persistence context.

That means Java could still think the selected address has
`isDefault = true`, even after the database row was changed to
`is_default = false`.

If the user clicked "make default" on an address that was already default, Java
saw no real change when `entity.setDefault(true)` ran. Because JPA thought the
entity was already true, it might not send a second update to the database.

### Simple Example

```text
Database before click:
  Home.is_default = true

Java loads Home:
  Home.isDefault = true

Bulk update runs:
  Database Home.is_default = false
  Java Home.isDefault is still true

Code runs:
  Home.setDefault(true)

JPA sees:
  old Java value = true
  new Java value = true
  no dirty change
```

Before the fix, the database could end with no default address even though the
response object said the selected address was default.

### What the Fix Changed

The service now returns immediately when the selected address is already
default:

```text
if (entity.isDefault()) {
    return converter.toDto(entity);
}
addressRepository.clearDefaultForUser(userId);
entity.setDefault(true);
return converter.toDto(addressRepository.save(entity));
```

Now the dangerous bulk update is not run when it is not needed.

If the address is already default, the method simply returns it. If the address
is not default, the method clears the old default and saves the new one.

## 2. Concurrent First Delivery Address Creation Had an Unsafe Retry

### What This Feature Is For

When a user adds their first delivery address, that address should automatically
become the default address.

If two requests create the first address at almost the same time, only one of
them should become default.

### How the Old System Behaved

The old code checked whether the user had any addresses, then decided whether
the new address should be default:

```text
boolean shouldBecomeDefault = !addressRepository.existsByUserId(userId);
entity.setDefault(shouldBecomeDefault);
return converter.toDto(saveAddress(entity));
```

It tried to catch a uniqueness violation inside `saveAddress(...)`:

```text
try {
    return addressRepository.save(entity);
} catch (DataIntegrityViolationException ex) {
    entity.setDefault(false);
    return addressRepository.save(entity);
}
```

### Why That Was a Bug

With real JPA, `save(...)` often does not immediately write SQL to the database.
The SQL may run later, during flush or transaction commit.

That means the unique-index error may happen after the `try/catch` block has
already finished. In that case, the retry code never runs.

Also, once a database flush fails, the transaction is commonly marked as
rollback-only. Retrying inside the same transaction is usually not reliable.

### Simple Example

```text
Request A checks addresses: none found
Request B checks addresses: none found

Both decide:
  "I am creating the first address, so I should be default."

Request A commits first:
  Address A is default.

Request B flushes later:
  Database rejects second default address.
```

Before the fix, the code expected the exception to happen inside `save(...)`,
but JPA could raise it later.

### What the Fix Changed

The service now locks the user row before creating a delivery address:

```text
var user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
boolean shouldBecomeDefault = !addressRepository.existsByUserId(userId);
```

`findByIdForUpdate(...)` uses a pessimistic write lock:

```text
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM UserEntity u WHERE u.id = :userId")
Optional<UserEntity> findByIdForUpdate(UUID userId);
```

In beginner terms: only one address-creation transaction for that user can pass
through this section at a time.

The first request creates the default address. The second request waits, then
sees that an address already exists, so it creates a non-default address.

## 3. Deleting the Default Delivery Address Left No Default Address

### What This Feature Is For

A user's address book should keep a simple invariant:

```text
If the user has at least one delivery address, one of them should be default.
```

### How the Old System Behaved

The old delete method removed the selected address and stopped:

```text
var entity = addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(...);
addressRepository.delete(entity);
```

### Why That Was a Bug

If the deleted address was the default address, the remaining addresses were all
left with `isDefault = false`.

The database row was removed correctly, but the address book was left in an
incomplete state.

### Simple Example

```text
Before delete:
  Home = default
  Office = not default

User deletes Home.

After old delete:
  Office = not default
```

Now the user still has an address, but no default address.

### What the Fix Changed

The service now checks whether the address being deleted is default. If it is,
the service selects another address for the same user and promotes it after the
delete:

```text
var replacement = entity.isDefault()
        ? addressRepository.findFirstByUserIdAndIdNotOrderByIdAsc(userId, addressId)
        : Optional.<DeliveryAddressEntity>empty();

addressRepository.delete(entity);

replacement.ifPresent(address -> {
    addressRepository.flush();
    address.setDefault(true);
    addressRepository.save(address);
});
```

In beginner terms: when the default address is removed, the app chooses another
address to become default, so the address book stays valid.

## 4. Profile Address Fields Allowed Values the Database Could Not Store

### What This Feature Is For

The profile address API lets a user store:

```text
country
city
line
postcode
```

The OpenAPI contract said these fields could be up to 128 characters.

### How the Old System Behaved

The OpenAPI contract allowed 128 characters:

```text
AddressDto:
  properties:
    country:
      maxLength: 128
    city:
      maxLength: 128
    line:
      maxLength: 128
    postcode:
      maxLength: 128
```

But the database entity allowed only 55 characters:

```text
@Column(name = "country", nullable = false, length = 55)
private String country;
```

The request validator checked whether address fields were present and not blank,
but it did not reject values longer than the database column.

### Why That Was a Bug

The API contract and the database disagreed.

A client could send a request that was valid according to OpenAPI, but the
database could reject it later with a low-level database error.

That is a bad user experience because the client should get a clear `400 Bad
Request` when input is invalid, not a surprise database failure.

### Simple Example

```text
Client sends city with 90 characters.

OpenAPI says:
  OK, max is 128.

Old database column says:
  Not OK, max is 55.
```

Before the fix, this could fail late while saving instead of failing clearly
during validation.

### What the Fix Changed

The `Address` entity now matches the contract:

```text
@Column(name = "country", nullable = false, length = 128)
private String country;
```

A Liquibase migration expands the existing database columns to 128 characters.

The validator also checks the address field length:

```text
if (value.length() > MAX_ADDRESS_FIELD_LENGTH) {
    errors.add(error(String.format(
            "Address field `%s` must not exceed %d characters.",
            fieldName,
            MAX_ADDRESS_FIELD_LENGTH)));
}
```

Now invalid data is rejected as a request problem, and valid contract data fits
in the database.

## 5. Profile Updates Replaced Address Rows and Could Leak Old Rows

### What This Feature Is For

`UserEntity` owns one profile address.

When the user updates their profile, the profile address should be created,
updated, or removed together with the user profile.

### How the Old System Behaved

The old mapper always mapped the request address to a new `Address` object:

```text
@Mapping(target = "address", source = "address", qualifiedByName = "toAddress")
void updateEntity(@MappingTarget UserEntity entity, UpdateUserAccountRequest request);
```

The relationship used cascade, but did not use orphan removal:

```text
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "address_id", referencedColumnName = "id")
private Address address;
```

There was also a dangerous database cascade direction:

```text
FOREIGN KEY (address_id) REFERENCES address(id) ON DELETE CASCADE
```

### Why That Was a Bug

Replacing the Java object does not automatically mean the old database row is
deleted.

Without `orphanRemoval = true`, the old address row can become unused data: no
user points to it, but it still exists in the `address` table.

The database cascade was also backwards for this ownership model. The user owns
the address. Deleting an address row should not delete the user row that points
to it.

### Simple Example

```text
User has Address A.
User updates profile address.

Old mapper creates Address B.
User now points to Address B.
Address A may remain in the database unused.
```

That is a leaked row.

### What the Fix Changed

The relationship now explicitly says the user owns the address and old owned
addresses should be removed:

```text
@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "address_id", referencedColumnName = "id")
private Address address;
```

The mapper now updates the existing address object in place when possible:

```text
if (entity.getAddress() == null) {
    entity.setAddress(Address.builder()
            .country(dto.getCountry())
            .city(dto.getCity())
            .line(dto.getLine())
            .postcode(dto.getPostcode())
            .build());
    return;
}

entity.getAddress().update(dto.getCountry(), dto.getCity(), dto.getLine(), dto.getPostcode());
```

A migration removes the dangerous `ON DELETE CASCADE` from
`user_details.address_id`.

In beginner terms: the user profile keeps the same address row and edits it.
When the address is removed or replaced, JPA is allowed to clean up the old row.

## 6. Expired Locked Accounts Could Stay Locked in `user_details`

### What This Feature Is For

The application tracks login lockout in two places:

```text
login_attempts:
  remembers failed login attempts and lock expiration

user_details:
  stores account_non_locked, used by Spring Security
```

When a lock expires, both places must agree that the user is unlocked.

### How the Old System Behaved

The old unlock query looked for login-attempt rows where the user was already
marked unlocked but still had an expiration time:

```text
WHERE la.isUserLocked = false
  AND la.expirationDatetime IS NOT NULL
```

But the security flow cleared expired login-attempt rows first. Clearing the row
set:

```text
isUserLocked = false
expirationDatetime = NULL
```

### Why That Was a Bug

After the login-attempt row was reset, the unlock query no longer matched it
because `expirationDatetime` was now `NULL`.

That could leave the system in a split state:

```text
login_attempts says:
  user is not locked

user_details says:
  account_non_locked = false
```

Spring Security reads `user_details.account_non_locked`, so the user could
remain locked even after the lock expired.

### Simple Example

```text
12:00 User is locked until 12:15.
12:16 Login flow resets login_attempts.
12:16 login_attempts.expirationDatetime becomes NULL.
12:16 unlockUsers query looks for expirationDatetime IS NOT NULL.
12:16 no user row is unlocked.
```

### What the Fix Changed

The unlock query now targets rows that are still locked and whose expiration is
already in the past:

```text
WHERE la.isUserLocked = true
  AND la.expirationDatetime IS NOT NULL
  AND la.expirationDatetime <= CURRENT_TIMESTAMP
```

The flow unlocks the user record before clearing the lock state in
`login_attempts`.

In beginner terms: the app now uses the expired-lock evidence while it still
exists.

## 7. Profile Update Contract Did Not Say Required Fields Were Required

### What This Feature Is For

The profile update endpoint updates the user's account details.

The backend treats the operation like a full `PUT`: the request must include
required profile fields such as first name and last name.

### How the Old System Behaved

The OpenAPI schema for `UpdateUserAccountRequest` did not list required fields.

But the service validator rejected missing names:

```text
if (name == null) {
    errors.add(error(label + " is required."));
    return;
}
```

### Why That Was a Bug

The documentation and backend behavior disagreed.

A client generated from OpenAPI could think this request is valid:

```text
{
  "phoneNumber": "+12025550123"
}
```

But the backend returned `400 Bad Request` because `firstName` and `lastName`
were missing.

### Simple Example

```text
OpenAPI says:
  firstName is optional

Backend says:
  firstName is required
```

The client cannot reliably know what to send.

### What the Fix Changed

The OpenAPI contract now declares `firstName` and `lastName` as required for the
profile update request:

```text
UpdateUserAccountRequest:
  required:
    - firstName
    - lastName
```

The service behavior did not need to become a partial update. The contract was
updated to describe the existing full-update behavior clearly.

## 8. Account and Avatar Delete Could Leave Stale File Metadata or Objects

### What This Feature Is For

Users can upload an avatar. The avatar has two pieces of state:

```text
file_metadata table:
  says which object belongs to the user

S3/MinIO/object storage:
  stores the real image bytes
```

When a user deletes their avatar or account, both pieces should eventually be
cleaned up.

### How the Old System Behaved

Originally, account deletion only deleted the user row:

```text
public void deleteProfile(UUID userId) {
    userRepository.deleteById(userId);
}
```

That meant avatar metadata and the actual object could be left behind.

Then the cleanup was moved into `deleteProfile(...)`, but the file deletion path
still deleted the external object as part of the same user operation:

```text
fileStorageApi.deleteFile(userId);
userRepository.deleteById(userId);
```

External object storage is not controlled by the database transaction.

### Why That Was a Bug

The database can roll back, but S3 or MinIO cannot roll back with it.

If the real file is deleted first and the database commit fails later, the
database may still say the avatar exists, but the file is gone.

### Simple Example

```text
1. Delete avatar object from S3.
2. Try to delete metadata/user row from PostgreSQL.
3. PostgreSQL transaction fails.

Result:
  Database still points to the avatar.
  S3 no longer has the avatar.
```

That creates broken avatar links.

### What the Fix Changed

`deleteProfile(...)` now deletes the user row first and registers profile cleanup
to run only after the database transaction commits.

In beginner terms: the database change is allowed to finish first. Only after
the delete is committed does the app ask for session revocation and avatar
cleanup. If the database transaction rolls back, the external cleanup is not
triggered for a user that still exists.

The file-storage service also no longer deletes the real object immediately.
It first deletes the metadata in the same database transaction, then writes a
durable outbox event:

```text
List<FileMetadataDto> fileMetadataList = findAllMetadata(relatedObjectId);
int deletedRows = fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId);
if (deletedRows > 0) {
    fileMetadataList.forEach(fileMetadataDto ->
            fileDeletionOutboxRepository.insertDeleteObjectEvent(fileMetadataDto, maxAttempts));
}
```

A background worker later reads the outbox event and deletes the real object:

```text
objectStorage.delete(new FileMetadataDto(
        payload.relatedObjectId(),
        payload.bucketName(),
        payload.fileName()));
outboxRepository.markDeleted(event.id(), properties.workerId());
```

In beginner terms: the database records "this file should be deleted" first.
Only after that durable record exists does a worker delete the real file.

If the worker fails, the outbox row remains retryable.

### Important Implementation Details

The file deletion cleanup intentionally reuses the existing `outbox_events`
table instead of creating a second outbox table.

The file deletion rows use:

```text
event_type = file.object.delete
aggregate_type = FileObjectDeletion
topic = internal.file.object.delete
```

The cleanup event stores the information needed to delete the object later:

```text
relatedObjectId
bucketName
fileName
```

The event uses a new random deletion ID as both `event_id` and `aggregate_id`.
That detail matters because the shared `outbox_events` table has a uniqueness
rule for aggregate/event combinations. If the file name or user ID were reused
as the aggregate ID, repeated avatar deletes for the same user could conflict
with older cleanup rows.

The service also reads all metadata rows for the related object before deleting
metadata:

```text
List<FileMetadataDto> fileMetadataList = findAllMetadata(relatedObjectId);
```

That is deliberate. If duplicate metadata rows exist, cleanup should enqueue a
delete event for every stored object, not only for the "preferred" metadata row
used when reading an avatar URL.

The worker has retry behavior:

```text
PENDING -> IN_PROGRESS -> PUBLISHED
PENDING -> IN_PROGRESS -> FAILED_RETRYABLE
PENDING -> IN_PROGRESS -> FAILED_PERMANENT
```

`PUBLISHED` is not a perfect name for file deletion because no Kafka message is
published. It is reused because the existing shared outbox status enum already
has `PUBLISHED` as the successful terminal state.

If the worker deletes the object but crashes before marking the row successful,
the row can be retried. That is acceptable because object deletion in S3/MinIO is
treated as idempotent: deleting an already-deleted object should still be safe.

## 9. File Deletion Outbox Initially Could Mark Work Done When Storage Was Off

### What This Feature Is For

The file deletion outbox worker should delete real objects from configured
object storage.

If object storage is not configured, it should not pretend deletion succeeded.

### How the Old System Behaved

The first outbox implementation claimed rows and called `objectStorage.delete`.

In local or disabled-storage environments, the `NoOpObjectStorage` implementation
could accept the call without deleting a real object.

### Why That Was a Bug

If the worker claimed an outbox row and marked it done while storage was not
actually configured, the cleanup request was lost.

Later, when real storage was configured, there would be no pending outbox row
left to process.

### Simple Example

```text
Outbox row says:
  delete user-avatar-123

Storage is disabled.
Worker runs anyway.
No-op delete does nothing.
Worker marks row PUBLISHED.
```

The row is gone from the pending queue, but the real object was never deleted.

### What the Fix Changed

The worker now exits before claiming rows when object storage is not configured:

```text
if (!objectStorage.isConfigured()) {
    log.warn("file.deletion_outbox.skipped: reason=object_storage_not_configured");
    return;
}
```

In beginner terms: if the worker cannot really delete files, it leaves the
database reminder alone so the cleanup can happen later.

## 10. Shared Outbox Rows Were Not Fully Scoped by Event Type

### What This Feature Is For

The `outbox_events` table is shared infrastructure.

Different features can store different event types in the same table, for
example:

```text
review.created
file.object.delete
```

Each worker should process only the event type it owns.

### How the Old System Behaved

Some outbox queries selected rows by status and timing, but did not consistently
guard every claim, reclaim, and terminal update by event type.

That was acceptable when only one outbox workflow existed, but it became risky
after file deletion started reusing the same table.

### Why That Was a Bug

Once multiple workflows share one table, a generic query can accidentally touch
another workflow's rows.

For example, a review publisher should never claim or mark a file-deletion row.

### Simple Example

```text
outbox_events:
  row 1: event_type = review.created
  row 2: event_type = file.object.delete

Review publisher asks:
  give me pending rows

If the query does not filter event_type:
  it might receive row 2 by accident.
```

### What the Fix Changed

Review outbox queries are now scoped to `review.created`.

File deletion outbox queries are scoped to `file.object.delete`:

```text
WHERE event_type = ?
  AND status IN ('PENDING', 'FAILED_RETRYABLE')
```

Terminal updates also check event type:

```text
WHERE id = ?
  AND event_type = ?
  AND status = 'IN_PROGRESS'
  AND locked_by = ?
```

An index was added so polling by event type and status stays efficient:

```text
CREATE INDEX IF NOT EXISTS ix_outbox_events_event_type_poll
    ON outbox_events (event_type, status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'FAILED_RETRYABLE');
```

In beginner terms: each worker now clearly says "only give me my kind of job."

The deep outbox review also checked these invariants:

- review publishing only claims `review.created` rows
- file deletion only claims `file.object.delete` rows
- stale-lock reclaim queries are scoped by event type
- final success/failure updates check row ID, event type, status, and worker ID
- file deletion rows are not claimed when object storage is not configured
- retryable rows respect `next_attempt_at`
- permanent failures stop retrying after the configured max-attempt count

## 11. Shared Inbox Terminal Updates Were Hardened Too

### What This Feature Is For

The `inbox_events` table stores incoming Kafka events until they are processed.

Like outbox, it is shared infrastructure. Different consumers and event types
can use the same table.

### How the Old System Behaved

The claim and reclaim queries were scoped by consumer and event type, but the
terminal update methods were guarded only by:

```text
id
status = IN_PROGRESS
locked_by
```

### Why That Was a Smell

The row ID came from a scoped claim, so this was not an obvious live bug. But in
a shared infrastructure table, the final updates should carry the same ownership
guard as the claim.

That makes it harder for future changes to accidentally mark another consumer's
row as processed, ignored, or failed.

### Simple Example

```text
Worker A owns:
  consumer = review-ai
  event_type = review.created

Final update should also say:
  only finish rows for review-ai + review.created
```

### What the Fix Changed

The inbox terminal methods now require `consumerName` and `eventType`:

```text
int markProcessed(UUID id, String workerId, String consumerName, String eventType)
```

The SQL checks those values:

```text
WHERE id = ?
  AND consumer_name = ?
  AND event_type = ?
  AND status = 'IN_PROGRESS'
  AND locked_by = ?
```

An inbox poll index was added:

```text
CREATE INDEX IF NOT EXISTS ix_inbox_events_consumer_event_type_poll
    ON inbox_events (consumer_name, event_type, status, next_attempt_at, created_at)
    WHERE status IN ('RECEIVED', 'FAILED_RETRYABLE');
```

In beginner terms: the worker proves it still owns the row before it changes the
row to processed, ignored, or failed.

## 12. Entity Equality Treated Two New Entities as Equal

### What This Feature Is For

Java `equals(...)` and `hashCode()` control how objects behave in collections
such as `Set` and `Map`.

JPA entities usually receive their database ID only after they are persisted.
Before that, the ID is `null`.

### How the Old System Behaved

Entities compared only their IDs using `Objects.equals(...)`:

```text
return Objects.equals(id, user.id);
```

For two new entities, both IDs are `null`, so the comparison returned `true`.

### Why That Was a Bug

Two different new objects should not be equal only because neither has been
saved yet.

If two new entities are added to a `Set`, Java may keep only one because it
thinks they are the same object.

### Simple Example

```text
new UserEntity().id = null
new UserEntity().id = null

Old equals result:
  true
```

That is wrong. They are two different unsaved users.

### What the Fix Changed

Entity equality now returns true only when the entity has a non-null ID and that
ID matches:

```text
return id != null && id.equals(user.id);
```

The hash code uses the entity class:

```text
return getClass().hashCode();
```

This was applied to user-related entities that had the same transient-entity
footgun.

## 13. Authentication Snapshot Exposed a Sensitive Field Too Generically

### What This Feature Is For

Security needs to load authentication data for a user, including the password
hash.

The password hash is not the plain password, but it is still sensitive and
should not be casually exposed through broad APIs.

### How the Old System Behaved

The user API boundary exposed a record with a password-hash field:

```text
public record UserAuthenticationSnapshot(..., String passwordHash) {}
```

### Why That Was a Smell

This was consumed internally by security, but the type lived in the user API
package. A broad API name plus a sensitive field increases the chance that
future code reuses it in the wrong place.

### Simple Example

```text
Developer sees:
  snapshot.passwordHash()

They may think:
  this is normal profile data
```

But it is credential material and should be treated carefully.

### What the Fix Changed

The field was renamed to make its meaning explicit and the record now documents
that it is authentication-only:

```text
/**
 * Authentication-only user view for the security module.
 */
public record UserAuthenticationSnapshot(..., String encodedPassword) {}
```

Security code now reads `encodedPassword()` instead of `passwordHash()`.

In beginner terms: the value is still available where authentication needs it,
but the name and Javadoc warn future developers that it is encoded credential
material and must not be reused for profile/public user responses.

The important detail is that this was intentionally kept as an authentication
contract, not a profile contract. `SecurityUserDetails` can still use the
encoded password to let Spring Security verify a login, but normal user/profile
responses still do not expose it.

## 14. User Authorities Were Eager-Loaded Everywhere

### What This Feature Is For

User authorities are roles or permissions, such as:

```text
ROLE_USER
ROLE_ADMIN
```

Authentication needs authorities, but many profile reads do not.

### How the Old System Behaved

The user entity loaded authorities eagerly:

```text
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
private Set<UserGrantedAuthority> authorities;
```

### Why That Was a Smell

`FetchType.EAGER` means JPA loads authorities whenever it loads a user, even
when the caller only needs the user's profile data.

That can cause unnecessary joins and extra data loading on ordinary user reads.

### Simple Example

```text
Profile endpoint needs:
  first name, last name, avatar

Old entity also loads:
  authorities
```

The data is not wrong, but the query does more work than needed.

### What the Fix Changed

Authorities now use the default lazy behavior for `@OneToMany`.

Authentication paths use explicit repository methods with an entity graph:

```text
@EntityGraph(attributePaths = "authorities")
@Query("SELECT u FROM UserEntity u WHERE u.email = :email")
Optional<UserEntity> findByEmailWithAuthorities(String email);
```

In beginner terms: normal user reads do not pay for roles, but login still loads
roles on purpose.

## 15. Avatar MIME Validation Did Not Require Header and Bytes to Match

### What This Feature Is For

Avatar upload should allow only real image files of supported types:

```text
image/jpeg
image/png
image/webp
```

The request has a declared content type, and the uploaded bytes have a real file
signature, also called magic bytes.

### How the Old System Behaved

The old validation checked that the declared content type was allowed, then
separately checked that the bytes looked like some allowed image type.

But it did not require those two checks to agree.

### Why That Was a Bug

A file could claim to be `image/png` while the bytes were actually JPEG. Both
checks could pass separately:

```text
Declared type image/png is allowed.
JPEG bytes are also an allowed image kind.
```

But the declaration and bytes did not match.

### Simple Example

```text
HTTP header:
  Content-Type: image/png

Actual bytes:
  JPEG file
```

Before the fix, this mismatch could be accepted.

### What the Fix Changed

The validator now detects the real content type from the bytes and compares it
to the declared content type:

```text
if (detectContentType(file).filter(contentType::equals).isEmpty()) {
    throw new InvalidAvatarFileTypeException(file.getContentType(), ALLOWED_CONTENT_TYPES);
}
```

In beginner terms: if the upload says "I am PNG", the bytes must also prove "I
am PNG."

## 16. Refresh Tokens Were Blacklisted for Too Short a Time

### What This Feature Is For

The token blacklist is a "do not accept this token anymore" list.

It is used when a token must stop working before its normal expiration time,
for example after logout or refresh-token rotation.

### How the Old System Behaved

The old blacklist method always used the access-token lifetime:

```text
temporaryStore.put(namespacedKey(token), "true", jwtProperties.expiration());
```

### Why That Was a Bug

Access tokens live for a short time. Refresh tokens live much longer.

If a refresh token is blacklisted only for the access-token lifetime, the
blacklist entry can disappear while the refresh token itself is still valid.

### Simple Example

```text
12:00 Refresh token is issued. It expires tomorrow.
12:10 Refresh token is rotated and blacklisted.
12:25 Blacklist entry expires after the short access-token TTL.
12:30 Old refresh token is tried again.
```

Before the fix, the old refresh token could become usable again after the
blacklist forgot it.

### What the Fix Changed

The blacklist now has separate methods for access tokens and refresh tokens:

```text
blacklist(token);
blacklistRefreshToken(token);
```

Refresh-token callers use the refresh-token TTL, so the blacklist remembers the
blocked refresh token for as long as the token could still be accepted.

## 17. JWT Validation Did Not Check Issuer and Audience

### What This Feature Is For

JWT validation must answer more than "is the signature valid?"

It should also answer:

```text
issuer: who created this token?
audience: who is this token for?
```

### How the Old System Behaved

The parser checked only the signature:

```text
Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
```

### Why That Was a Bug

If another service or environment accidentally shared the signing key, a token
from that other place could have a valid signature.

Without issuer and audience checks, this backend could accept a token that was
not created by the expected issuer or not meant for this API.

### Simple Example

```text
Token A:
  signed correctly
  issuer = iced-latte-api
  audience = iced-latte-frontend

Token B:
  signed correctly
  issuer = other-service
  audience = other-client
```

Before the fix, both could pass signature validation. After the fix, `Token B`
is rejected.

### What the Fix Changed

JWT parsing now requires the configured issuer and audience. Tests cover valid
tokens, wrong issuer, and wrong audience.

## 18. OAuth Could Create Accounts with an Unverified Provider Email

### What This Feature Is For

OAuth login can create or link a local account using the email returned by a
provider such as Google.

### How the Old System Behaved

The old code protected only the existing-local-user linking case:

```text
if (existingUser && !profile.emailVerified()) {
    throw new UnauthorizedException(...);
}
```

If no local user existed yet, the backend could still create a new account from
an unverified provider email.

### Why That Was a Bug

The application uses email as account identity. Creating a local account for an
email means the backend trusts that the OAuth user owns that email.

If the provider has not verified the email, the backend should not trust it for
identity.

### Simple Example

```text
Google profile:
  email = alice@example.com
  emailVerified = false

Old behavior:
  no local user exists
  create local account anyway
```

### What the Fix Changed

`OAuthLoginService` now rejects unverified provider emails before both actions:

- linking OAuth to an existing local user
- creating a new local user

The rule is now simple: no verified provider email means no OAuth login/account
creation.

## 19. Refresh-Token Rotation Was Race-Prone

### What This Feature Is For

Refresh-token rotation means a refresh token should be usable only once.

### How the Old System Behaved

The old code used read-change-save:

```text
session = repository.findByRefreshTokenHash(hash);
session.rotateTo(newHash);
repository.save(session);
```

### Why That Was a Bug

Two requests using the same refresh token at the same time could both read the
token as active before either request saved the rotated state.

### Simple Example

```text
Request A reads refresh token R1 as active.
Request B also reads R1 as active.
Request A creates R2.
Request B creates R3.
```

Before the fix, both requests could receive valid new token pairs even though
`R1` should be single-use.

### What the Fix Changed

The repository now locks the auth-session row while rotating:

```text
findByRefreshTokenHashForUpdate(refreshTokenHash)
```

The first request rotates the token. The second request waits, then sees that
the old token is no longer active.

## 20. Login-Attempt Counting Was Race-Prone

### What This Feature Is For

Login-attempt counting locks or slows abusive login attempts after too many bad
passwords.

### How the Old System Behaved

The old logic used read-increment-save:

```text
attempt.setAttempts(attempt.getAttempts() + 1);
repository.save(attempt);
```

### Why That Was a Bug

Concurrent failed-login requests could overwrite each other's increments.

### Simple Example

```text
Current attempts = 3
Request A reads 3 and saves 4.
Request B reads 3 and saves 4.
```

There were two failures, so the correct result was `5`, not `4`.

### What the Fix Changed

The login-attempt row is locked during update, so concurrent requests update the
count one at a time. First-insert collisions are also handled, so two first
attempts for the same email do not create duplicate/conflicting rows.

## 21. Email Verification and Password Reset Used Global Short Codes

### What This Feature Is For

Email verification and password reset tokens prove that the caller controls a
pending email/reset flow.

### How the Old System Behaved

The old system used short numeric codes and keyed storage mainly by the code:

```text
email:token:<code> -> request data
```

### Why That Was a Bug

Short numeric codes have limited randomness.

Also, if two codes collide, one user's pending request can overwrite another
because the code is the global key.

### Simple Example

```text
Alice gets code 123456789.
Bob also gets code 123456789.

Both use the same global storage key.
```

### What the Fix Changed

The system now uses long URL-safe opaque tokens.

Storage is scoped by token purpose and token identity:

```text
email:token:<purpose>:<token-identity>
```

The stored payload contains the normalized email and request data. Cooldown
storage is keyed separately by normalized email:

```text
email:rate:<normalized-email>
```

That makes guessing much harder, separates verification and reset flows by
purpose, and avoids the old short-code collision problem.

## 22. Turnstile Verification Had No Explicit HTTP Timeout

### What This Feature Is For

Cloudflare Turnstile verification calls an external service during login or
registration.

### How the Old System Behaved

The old verifier used a default REST client:

```text
RestClient.create()
```

### Why That Was a Bug

External calls can hang or become slow. Without explicit timeouts, authentication
requests can occupy backend threads for too long.

### Simple Example

```text
User submits login.
Backend calls Cloudflare.
Network stalls.
Login request waits too long.
```

### What the Fix Changed

`TurnstileVerifier` now builds its REST client with explicit connect and read
timeouts. Slow external verification fails instead of hanging indefinitely.

## 23. Security Imported a User Feature Internal Exception

### What This Feature Is For

The backend is a modular monolith. Feature packages should communicate through
stable APIs, not through each other's internal classes.

### How the Old System Behaved

Security imported a user feature internal exception:

```text
import com.zufar.icedlatte.user.exception.UserNotFoundException;
```

### Why That Was a Smell

This made security depend on a user implementation detail. If the user feature
renamed or changed that exception, security could break even though the public
user API still worked.

### Simple Example

```text
user.exception.UserNotFoundException is renamed.
user.api still works.
security fails to compile because it imported the internal exception.
```

### What the Fix Changed

Security now talks to the user feature through `UserLookupApi`, the stable user
boundary.

The dependency direction became:

```text
security -> user.api
```

instead of:

```text
security -> user.exception
```

## 24. JWT Auth and Refresh Ignored Current Account State

### What This Feature Is For

Account state decides whether a user may currently authenticate:

```text
enabled
account non-locked
account non-expired
credentials non-expired
```

### How the Old System Behaved

The backend validated the token and loaded the user, but did not consistently
reject users whose current account state had changed:

```text
UserDetails userDetails = userDetailsService.loadUserByUsername(email);
return authenticatedToken(userDetails);
```

Refresh-token handling had the same type of gap.

### Why That Was a Bug

Tokens can outlive account-state changes. If an admin disables or locks a user,
old tokens should not keep working.

### Simple Example

```text
11:55 User logs in.
12:00 Admin disables the user.
12:01 User calls API with old access token.
12:02 User refreshes and gets new tokens.
```

Before the fix, those actions could still succeed if the token itself was valid.

### What the Fix Changed

`JwtAccountStatusValidator.requireActive(...)` now checks account state during:

- access-token authentication
- refresh-token handling

Disabled, locked, expired, or credentials-expired accounts are rejected.

## 25. Google OAuth Code Exchange Had No Explicit HTTP Timeout

### What This Feature Is For

Google OAuth login requires the backend to exchange a temporary Google `code`
for Google tokens/profile data.

### How the Old System Behaved

The old Google OAuth exchange used default HTTP request settings:

```text
new GoogleAuthorizationCodeFlow.Builder(transport, json, clientId, secret, scopes)
```

### Why That Was a Bug

Google is an external service. The backend should not let a slow Google/network
call hold an auth request longer than intended.

### Simple Example

```text
User clicks Continue with Google.
Backend exchanges code with Google.
Google/network is slow.
Login request waits too long.
```

### What the Fix Changed

`GoogleTokenExchanger` now sets explicit connect and read timeouts through a
Google `HttpRequestInitializer`. The timeout values are configurable.

## 26. CORS Allowed Credentials Without Rejecting Wildcards

### What This Feature Is For

CORS controls which browser origins may call the backend.

When credentials are allowed, origins must be explicit and trusted.

### How the Old System Behaved

The old configuration accepted configured origin patterns directly:

```text
configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
configuration.setAllowCredentials(corsProperties.allowCredentials());
```

### Why That Was a Bug

Wildcard origins are dangerous with credentials. A broad pattern can accidentally
allow credentialed cross-origin requests from untrusted sites.

### Simple Example

```text
allow-credentials = true
allowed-origins = *
```

That is not a safe combination.

### What the Fix Changed

`AppCorsConfiguration` now validates startup configuration. When credentials are
enabled, blank origins, `null`, and origins containing `*` are rejected.

Bad CORS config now fails fast.

## 27. OAuth Returned Tokens in the Redirect URL Fragment

### What This Feature Is For

After successful OAuth login, the frontend needs the app's own token pair.

### How the Old System Behaved

The old redirect put tokens in the browser URL fragment:

```text
callbackBase + "#token=" + accessToken + "&refreshToken=" + refreshToken
```

### Why That Was a Bug

The URL fragment is readable by frontend JavaScript and can appear in browser
history or debugging tools.

This is especially risky for refresh tokens because they live longer than access
tokens.

### Simple Example

```text
https://app.example.com/auth/google/callback#token=ACCESS&refreshToken=REFRESH
```

Any script on that callback page can read `location.hash`.

### What the Fix Changed

The OAuth redirect now contains only a short-lived one-time handoff code:

```text
#oauthCode=<one-time-code>
```

The frontend sends that code back to the backend. The backend returns the real
tokens once and then removes the code from the server-side store.

## 28. Email Token Length Config Was Not Bounded

### What This Feature Is For

Token length affects how hard verification/reset tokens are to guess.

### How the Old System Behaved

The old numeric token generator depended on integer math:

```text
Math.pow(10, tokenLength)
```

### Why That Was a Bug

Bad configuration could make tokens too short and easy to guess. Very large
values could also break the numeric algorithm.

### Simple Example

```text
email.verification-token-length = 4
```

That gives only 10,000 possible numeric tokens, which is too weak for a security
token.

### What the Fix Changed

Token generation now uses secure random bytes encoded as URL-safe text.

The configured length is validated and values below the secure minimum fail
fast.

## 29. JWT Access and Refresh Tokens Did Not Carry an Explicit Purpose

### What This Feature Is For

The application uses two different JWT types:

```text
access token:
  short-lived token used to call normal API endpoints

refresh token:
  longer-lived token used only to get a new token pair
```

Even when the two token types have different signing keys, it is safer for the
token itself to say what it is for.

### How the Old System Behaved

Access tokens and refresh tokens were signed with different keys, but the token
claims did not include an explicit purpose:

```text
claims.put(JwtClaimNames.JWT_ID, UUID.randomUUID().toString());
```

Refresh tokens had a version claim:

```text
claims.put(JwtClaimNames.VERSION, 2);
```

But `ver = 2` only described the refresh-token format. It did not clearly say
"this token is a refresh token and must never be accepted as an access token."

### Why That Was a Bug

Security checks are easier to reason about when every token has an explicit
purpose.

Without a purpose claim, the system relies mostly on using the correct parser
and signing key for each token type. That is good, but a future configuration or
code mistake could weaken the boundary between access-token handling and
refresh-token handling.

There was also no startup guard preventing the access-token signing key and the
refresh-token signing key from being configured to the same value.

### Simple Example

```text
Access-token parser:
  should accept only tokens with purpose = access

Refresh-token parser:
  should accept only tokens with purpose = refresh
```

Before the fix, the token type was not stated this directly inside the token.

### What the Fix Changed

JWT claim names now define token purpose constants:

```text
public static final String TOKEN_PURPOSE = "purpose";
public static final String ACCESS_TOKEN_PURPOSE = "access";
public static final String REFRESH_TOKEN_PURPOSE = "refresh";
```

Access-token generation now writes:

```text
claims.put(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.ACCESS_TOKEN_PURPOSE);
```

Refresh-token generation now writes:

```text
claims.put(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.REFRESH_TOKEN_PURPOSE);
```

The access-token parser now requires:

```text
require(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.ACCESS_TOKEN_PURPOSE)
```

The refresh-token parser now requires:

```text
require(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.REFRESH_TOKEN_PURPOSE)
```

`JwtSigningKeys` also rejects configuration where both token types use the same
signing key:

```text
if (Arrays.equals(accessKeyBytes, refreshKeyBytes)) {
    throw new IllegalStateException("JWT access and refresh signing keys must be different");
}
```

In beginner terms: an access token now carries a label saying "I am for API
access", and a refresh token carries a label saying "I am only for refresh."
The parser checks that label before trusting the token.

## 30. Delivery Address Fields Accepted Whitespace-Only Text

### What This Feature Is For

Delivery addresses require real text for fields like label, street line, city,
country, and postcode.

### How the Old System Behaved

The OpenAPI contract used `minLength: 1`:

```text
label:
  type: string
  minLength: 1
```

That generated validation similar to "must be at least one character." A value
like this passed because it has three characters:

```text
"   "
```

`DeliveryAddressService` then saved that value directly.

### Why That Was a Bug

Whitespace is not a real address value. A user could create an address whose
label or postcode looked empty in the UI, but the backend still stored it.

In beginner terms: the old check counted spaces as text. We needed a check that
says "after ignoring spaces, there must still be something left."

### What the Fix Changed

The delivery address OpenAPI fields now add generated `@NotBlank` validation.
For example:

```text
label:
  type: string
  minLength: 1
  maxLength: 64
  x-field-extra-annotation: "@jakarta.validation.constraints.NotBlank(message = \"Address label must not be blank\")"
```

This was added for `label`, `line`, `city`, `country`, and `postcode`.

After OpenAPI generation, `DeliveryAddressRequest` now contains annotations like
this on the generated fields:

```text
@jakarta.validation.constraints.NotBlank(message = "Address label must not be blank")
private String label;
```

A new endpoint test sends a whitespace-only label and expects `400 Bad Request`.
It also verifies that no address was saved.

In beginner terms: the API contract still says "this field is required," but it
now also says "spaces alone do not count as a real value."

## 31. User Registration Relied on Callers to Normalize Email

### What This Feature Is For

The user module creates and finds users by email address.

### How the Old System Behaved

Registration saved whatever email the caller passed:

```text
.email(email)
```

Lookups also searched for exactly the caller-provided value:

```text
userCrudRepository.findByEmail(email)
```

Security callers already normalized emails today, but the user API boundary can
be reused by future code.

### Why That Was a Bug

PostgreSQL's normal unique constraint treats these as different strings:

```text
Alice@example.com
alice@example.com
```

So a future caller that forgot to normalize could create duplicate accounts for
the same real email.

In beginner terms: the system was trusting every caller to clean the email
first. The safer place to do that is inside the user package, right before user
data is checked or saved.

### What the Fix Changed

`UserAccountRegistrationService` now normalizes email inside the user package
before checking existence:

```text
return userRepository.existsByEmail(
        Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"));
```

It also normalizes before saving a new user:

```text
.email(Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null"))
```

`SingleUserProvider` now applies the same normalization before user and
authentication lookups.

Unit tests now verify that mixed-case emails with surrounding spaces are sent to
repositories as normalized lowercase emails.

In beginner terms: even if a future caller sends `" Alice@Example.COM "`, the
user module looks up and stores `"alice@example.com"`.

One important design detail: the user package does not import a security helper
to do this. Email normalization is now in `common.util.EmailNormalizer`, so both
security and user code can use it without creating a backwards dependency from
`user` to `security`.

## 32. User API Exposed an Internal Exception Type

### What This Feature Is For

The `user.api` package is the stable boundary other packages use to talk to the
user module.

### How the Old System Behaved

`UserLookupApi` imported an internal exception:

```text
import com.zufar.icedlatte.user.exception.UserNotFoundException;

UserLookupSnapshot getUserById(UUID userId) throws UserNotFoundException;
```

### Why That Was a Smell

The repository architecture says `api/` should expose stable interfaces,
records, and DTOs. Importing `user.exception.UserNotFoundException` makes the
public boundary depend on an internal implementation detail.

In beginner terms: other packages should not need to know the user module's
private exception classes just to call the user API.

### What the Fix Changed

The API interface no longer imports or declares the internal exception:

```text
UserLookupSnapshot getUserById(UUID userId);
UserLookupSnapshot getUserByEmail(String email);
```

The implementation can still throw its unchecked internal exception from inside
the user module, but the public boundary no longer advertises that internal type
as part of the API contract.

In beginner terms: the actual behavior did not change for callers that ask for a
missing user. The cleanup is about the package boundary. The public user API is
now less tied to the user module's internal exception package.

## 33. Email Normalization Was Duplicated in the Wrong Package

### What This Feature Is For

Email normalization converts emails to the canonical form used by the backend:

```text
" Alice@Example.COM " -> "alice@example.com"
```

This matters anywhere the system checks, saves, or signs in with an email.

### How the Old System Behaved

There was already an email normalizer, but it lived in the security package:

```text
package com.zufar.icedlatte.security.util;

public class EmailNormalizer {
    public static String normalize(String email) {
        return email == null ? null : email.toLowerCase(Locale.ROOT).trim();
    }
}
```

When email normalization was added inside the user package, the first fix used
small private helper methods:

```text
private static String normalizeEmail(String email) {
    return Objects.requireNonNull(email, "email must not be null")
            .toLowerCase(Locale.ROOT)
            .trim();
}
```

That worked functionally, but it duplicated logic that already existed.

### Why That Was a Smell

The user package should not import from the security package. `security` depends
on user APIs for login and registration, but user profile/account code should not
depend back on security internals.

So there were two bad options:

```text
1. Keep duplicated private normalization helpers in user.
2. Make user import security.util.EmailNormalizer.
```

The first option duplicates logic. The second option creates the wrong module
dependency direction.

In beginner terms: if two parts of the app need the same tiny generic email
cleanup, it should live in a neutral shared place, not inside one feature that
the other feature has to reach into.

### What the Fix Changed

The normalizer was moved to `common.util`:

```text
package com.zufar.icedlatte.common.util;

@UtilityClass
public class EmailNormalizer {
    public static String normalize(String email) {
        return email == null ? null : email.toLowerCase(Locale.ROOT).trim();
    }
}
```

Security imports were updated from:

```text
import com.zufar.icedlatte.security.util.EmailNormalizer;
```

to:

```text
import com.zufar.icedlatte.common.util.EmailNormalizer;
```

The duplicated private helper methods were removed from user services, and the
old `security.util.EmailNormalizer` file was deleted.

The user package now depends only on the neutral common utility:

```text
import com.zufar.icedlatte.common.util.EmailNormalizer;
```

In beginner terms: there is now one email-normalization rule in one shared
place, and both security and user code use that same rule.

## 34. Prometheus Metrics Were Public

### What This Feature Is For

`/actuator/prometheus` is the endpoint Prometheus scrapes to collect backend
metrics.

Those metrics are useful for Grafana dashboards, alerting, and debugging
production behavior.

### How the Old System Behaved

The old security configuration treated Prometheus like health and readiness
checks:

```text
.requestMatchers(
        "/actuator/health", "/actuator/info", "/actuator/prometheus", "/livez", "/readyz")
.permitAll()
```

The rate-limiting filter also skips actuator paths:

```text
path.startsWith(ApiPaths.ACTUATOR_ROOT)
```

So an unauthenticated client could repeatedly fetch Prometheus metrics without
normal API authentication and without normal rate limiting.

### Why That Was a Bug

Prometheus metrics usually do not contain passwords or full tokens, but they can
still expose internal operational information.

Examples include:

```text
route names
request counts
error counts
latency patterns
auth failure patterns
traffic volume
database/cache/client metric names
```

That information helps operators, but it can also help an attacker understand
which endpoints exist, which endpoints are failing, and when the application is
busy.

In beginner terms: metrics are not the same as health checks. A health check can
say "the app is alive." Prometheus can say much more about what the app is
doing.

### Simple Example

```text
Public user opens:
  GET /actuator/prometheus

Old behavior:
  HTTP 200
  response contains internal metric names and route/activity information
```

Before the fix, the backend exposed operational details to anyone who could
reach the service.

### What the Fix Changed

The public allow-list now keeps only health, info, and liveness/readiness
endpoints public:

```text
.requestMatchers("/actuator/health", "/actuator/info", "/livez", "/readyz")
.permitAll()
```

All other actuator endpoints, including Prometheus, now fall through to the
admin actuator rule:

```text
.requestMatchers(ApiPaths.ACTUATOR_ROOT + "**")
.hasRole("ADMIN")
```

New tests verify the intended behavior:

```text
GET /actuator/prometheus without auth       -> 401 Unauthorized
GET /actuator/prometheus as normal user     -> 403 Forbidden
GET /actuator/prometheus as admin           -> 200 OK
GET /actuator/health without auth           -> 200 OK
```

In beginner terms: basic "is the app alive?" endpoints stay public, but detailed
metrics now require admin access.

## 35. Email Verification and Password Reset Tokens Were Visible in Cache Keys

### What This Feature Is For

Email verification and password reset flows create a temporary token, send it to
the user, and store a temporary server-side record so the backend can verify the
token later.

The token itself is a secret. Whoever has a valid password-reset token can prove
they control that reset flow.

### How the Old System Behaved

After the earlier short-code fix, the token was long and high entropy. That was
good.

But the backend still used the raw token directly in the cache key:

```text
private static String tokenKey(TokenPurpose purpose, String token) {
    return TOKEN_KEY_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + token;
}
```

That produced keys like:

```text
email:token:password_reset:qL8xV4...realResetToken
email:token:email_verification:Av7pK...realVerificationToken
```

The token value was not only in the email sent to the user. It was also visible
as part of the Redis/cache key name.

### Why That Was a Bug

Cache keys are often easier to see than cache values.

For example, keys may appear in:

```text
Redis CLI output
cache admin tools
debugging screenshots
metrics/cardinality tools
developer logs
incident investigation notes
```

If the raw password-reset token is inside the key, anyone who can see the key
can see the real token.

The token was already hard to guess, so this was not the old "short code can be
guessed" bug. This was an exposure bug: the secret was placed somewhere that is
commonly inspected.

### Simple Example

```text
Backend creates reset token:
  qL8xV4-real-secret-token

Old Redis/cache key:
  email:token:password_reset:qL8xV4-real-secret-token

Someone views cache keys:
  they can copy the real reset token from the key
```

In beginner terms: even if the token is strong, we should not write the secret
in a label that admins, tools, or logs might show.

### What the Fix Changed

The backend still sends the real token to the user.

But before using the token as part of the cache key, it now hashes the token with
SHA-256:

```text
private static String tokenKey(TokenPurpose purpose, String token) {
    return TOKEN_KEY_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + hashToken(token);
}

private static String hashToken(String token) {
    byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
}
```

The flow now looks like this:

```text
1. Backend creates real token:
   qL8xV4-real-secret-token

2. Backend emails real token to the user.

3. Backend stores cache entry under:
   email:token:password_reset:<sha256-hash-of-token>

4. User submits the real token.

5. Backend hashes the submitted token and looks up the hashed cache key.
```

So the user experience does not change. The user still receives and submits the
same token.

The internal cache key changes from:

```text
email:token:password_reset:<raw-token>
```

to:

```text
email:token:password_reset:<hashed-token>
```

The test now captures the key passed to the temporary store and checks that it
still has the right purpose prefix but does not end with the raw token.

## 36. Login Lockout Used Raw Email Input Before Normalization

### What This Feature Is For

Login lockout counts failed password attempts for an email address.

After too many failures, the account is temporarily locked so repeated password
guessing becomes harder.

### How the Old System Behaved

The login flow used the email from the request directly when it authenticated
and when it recorded a failed attempt:

```text
String userEmail = request.getEmail();

authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(userEmail, request.getPassword()));

loginAttemptService.recordFailure(userEmail);
```

That meant these inputs could be treated differently:

```text
alice@example.com
Alice@Example.COM
" alice@example.com "
```

### Why That Was a Bug

The database stores user emails in normalized form. The custom user-details
lookup also expects normalized email behavior.

If login failure tracking uses raw input, the same real email can create
separate lockout counters. That weakens lockout because failed attempts may be
split across different spellings of the same email.

In beginner terms: the system was counting attempts against exactly what the
attacker typed, not against the real normalized account identity.

### Simple Example

```text
Attempt 1: alice@example.com
Attempt 2: Alice@Example.COM
Attempt 3: " alice@example.com "
```

Before the fix, those could be counted as separate values instead of one
account.

### What the Fix Changed

`UserAuthenticationService` now normalizes the request email once at the start:

```text
String userEmail = EmailNormalizer.normalize(request.getEmail());
```

The normalized email is then used for authentication and failed-attempt
tracking:

```text
UsernamePasswordAuthenticationToken.unauthenticated(userEmail, request.getPassword())
loginAttemptService.recordFailure(userEmail);
```

`CustomUserDetailsService` also normalizes before user lookup:

```text
String normalizedEmail = EmailNormalizer.normalize(email);
userLookupApi.findUserAuthenticationByEmail(normalizedEmail);
```

So login, lockout, reset, and user lookup now speak the same email format.

## 37. Password Reset Lookup Used Raw Email Input

### What This Feature Is For

Password reset starts when a user enters their email address. The backend checks
whether that user exists and then sends a reset token.

### How the Old System Behaved

The password reset service looked up the user with the raw email string from the
request:

```text
userLookupApi.findUserByEmail(email);
emailVerificationService.sendPasswordResetCode(email);
```

So these could behave differently:

```text
alice@example.com
Alice@Example.COM
 alice@example.com 
```

### Why That Was a Bug

User lookup is exact-match at the repository boundary. If the stored email is
`alice@example.com`, a reset request for `Alice@Example.COM` could miss the real
user.

In beginner terms: a user could type the right email with different casing or
spaces and not receive a reset email, even though the account exists.

### Simple Example

```text
Stored email:
  alice@example.com

Reset request:
  Alice@Example.COM

Old lookup:
  find exactly "Alice@Example.COM"
```

That exact lookup may not find the existing account.

### What the Fix Changed

`PasswordResetService` now normalizes first:

```text
String normalizedEmail = EmailNormalizer.normalize(email);
```

Then it uses that same normalized value for both the lookup and reset email
flow:

```text
userLookupApi.findUserByEmail(normalizedEmail);
emailVerificationService.sendPasswordResetCode(normalizedEmail);
```

So the password reset flow now finds the same account regardless of harmless
email casing or surrounding spaces.

## 38. Session Revocation Paths Were Still Race-Prone

### What This Feature Is For

Session revocation is used when a refresh session must stop working.

Examples:

```text
User logs out.
User revokes one session from the session list.
The backend detects replay and revokes related sessions.
```

### How the Old System Behaved

Refresh-token rotation already used a database row lock, but some revocation
paths still used plain reads:

```text
sessionRepository.findByRefreshTokenHash(refreshTokenHash)
sessionRepository.findById(sessionId)
```

Then the service mutated the entity and saved it.

### Why That Was a Bug

Plain reads do not stop another transaction from reading or changing the same
session at the same time.

So a logout/revoke request and a refresh request could race. One request could
read the session as active while the other was trying to revoke or rotate it.
That creates last-write-wins behavior where the final database state depends on
timing.

In beginner terms: we locked the door for refresh rotation, but two other
session-changing paths were still using the unlocked door.

### Simple Example

```text
Request A: refresh token rotation starts.
Request B: logout starts.

Both read the same active session without a shared lock.
Both make changes.
Whichever saves last decides the final state.
```

### What the Fix Changed

Revocation by refresh-token hash now uses the locked repository method:

```text
sessionRepository.findByRefreshTokenHashForUpdate(refreshTokenHash)
```

Revocation by session id now also locks the row:

```text
sessionRepository.findByIdForUpdate(sessionId)
```

Replay handling that looks up a session by id also uses the locked lookup.

Now refresh, logout, single-session revoke, and replay cleanup all coordinate on
the same database row lock before changing a session.

## Known Follow-Up Not Fixed in This Chat

### File Upload Still Writes the External Object Before Metadata

The deletion side now uses the outbox pattern, but the generic upload path still
uploads the real object before saving metadata:

```text
objectStorage.upload(file, fileMetadataDto.bucketName(), fileMetadataDto.fileName());
fileMetadataRepository.deleteByRelatedObjectId(fileMetadataDto.relatedObjectId());
fileMetadataRepository.save(fileMetadataDtoConverter.toEntity(fileMetadataDto));
```

For the current avatar use case, this is lower risk because avatar file names
are stable:

```text
user-avatar-<userId>
```

If metadata save fails after upload, the next avatar upload for the same user
uses the same object key and can overwrite the orphaned object.

For future generic file uploads with unique object names, this could become a
real orphan-object problem:

```text
1. Upload object to S3/MinIO.
2. Database metadata save fails.
3. No metadata row points to the uploaded object.
```

That was not changed in this remediation because it is a separate upload-side
compensation design. A future fix could add an upload outbox/cleanup intent or a
best-effort compensating delete when metadata save fails.

## 39. Admin Users Could Not Be Represented by the User Authority Enum

### What This Feature Is For

The security configuration protects admin endpoints with admin-role checks:

```text
hasRole("ADMIN")
```

That requires the application to be able to persist and load an admin authority
for a user.

### How the Old System Behaved

The user authority enum contained only:

```text
USER
```

So even though the security rules expected an admin role, the user module could
not represent `ADMIN` as a normal persisted authority.

### Why That Was a Bug

An authorization rule is only useful if the account model can express the
required role.

Before the fix, a database-backed admin account could not be modeled cleanly
through the `Authority` enum.

### Simple Example

```text
Security rule:
  /api/v1/admin/orders requires ADMIN

User authority enum:
  only USER exists
```

The application had a role check that the user role model could not satisfy.

### What the Fix Changed

`Authority` now includes:

```text
ADMIN
USER
```

This lets the user module persist the same authority names that security expects
to authorize admin endpoints.

## 40. Persisted Authorities Did Not Match Spring `hasRole(...)` Names

### What This Feature Is For

Spring Security's `hasRole("ADMIN")` check expects a granted authority named:

```text
ROLE_ADMIN
```

But the database stores role names without the Spring prefix:

```text
ADMIN
USER
```

### How the Old System Behaved

Authentication snapshots carried authority names like:

```text
USER
```

Those values were converted directly into Spring granted authorities.

### Why That Was a Bug

If Spring receives `USER`, then `hasRole("USER")` does not match it. Spring's
role check looks for `ROLE_USER`.

The result is that a valid persisted user role can fail authorization checks
because the runtime granted-authority name is missing the prefix Spring expects.

### Simple Example

```text
Database role:
  USER

Spring check:
  hasRole("USER")

Spring internally checks for:
  ROLE_USER
```

Before the fix, the names did not line up.

### What the Fix Changed

`SecurityUserDetails` now prefixes snapshot authorities with `ROLE_` when
needed.

In beginner terms: the database keeps simple enum names, while the Spring
security object gets the exact authority names Spring's role checks expect.

## 41. User Authority Equality Collapsed Different Users' Same Role

### What This Feature Is For

`UserGrantedAuthority` represents one user's role row.

Two users can both have the `USER` role, but those are still two different
database rows owned by two different users.

### How the Old System Behaved

`UserGrantedAuthority.equals(...)` compared only the authority value:

```text
return authority == that.authority;
```

That means these two rows compared equal:

```text
alice -> USER
bob   -> USER
```

### Why That Was a Bug

Entity equality should not collapse two different persisted rows just because
one business field has the same value.

This is especially risky with sets and persistence collections. A collection can
drop one item because Java thinks the two role rows are the same object.

### Simple Example

```text
Set<UserGrantedAuthority> roles = new HashSet<>();
roles.add(aliceUserRole);
roles.add(bobUserRole);

Old result:
  set size could be 1
```

That is wrong because the rows belong to different users.

### What the Fix Changed

`UserGrantedAuthority` now follows the same persisted-ID equality pattern as the
other user entities:

```text
return userAuthorityId != null && userAuthorityId.equals(that.userAuthorityId);
```

Duplicate role names for the same user are prevented separately by user-level
add logic and a database uniqueness constraint.

## 42. User Authority Rows Had No User Foreign Key or Per-User Uniqueness

### What This Feature Is For

Every row in `user_granted_authority` should belong to a real user.

A user should also not need duplicate rows for the same authority:

```text
same user + same authority = only one row
```

### How the Old System Behaved

The authority table had a `user_id`, but the database did not enforce a foreign
key back to `user_details`.

It also did not enforce uniqueness for `(user_id, authority)`.

### Why That Was a Bug

Without a foreign key, the database can contain orphan authority rows that point
to no real user.

Without the uniqueness constraint, the same user can have duplicate copies of
the same authority. That makes authorization data harder to reason about and can
hide application bugs.

### Simple Example

```text
user_details:
  no row with id = 1111

user_granted_authority:
  user_id = 1111, authority = USER
```

The authority row is meaningless because the user does not exist.

### What the Fix Changed

A migration now:

- deletes orphan authority rows
- removes duplicate `(user_id, authority)` rows
- adds a foreign key from `user_granted_authority.user_id` to `user_details.id`
- adds a unique constraint on `(user_id, authority)`

In beginner terms: the database now protects the same invariant the Java code
expects.

## 43. Default-Address Delete and Change Were Not Serialized Per User

### What This Feature Is For

Only one delivery address should be default for a user.

Creating the first address already used a user-row lock so concurrent requests
could not both decide to create the default address.

### How the Old System Behaved

`create(...)` locked the user row, but `delete(...)` and `setDefault(...)` did
not use the same per-user lock.

### Why That Was a Bug

Default-address operations all update the same per-user invariant:

```text
at most one default address
if addresses remain, one should be default
```

If one request deletes the current default while another request changes the
default, both transactions can read stale state and then write conflicting
updates.

### Simple Example

```text
Request A:
  delete current default address

Request B:
  set another address as default

Both operate at the same time without a shared user-level lock.
```

The database unique index helps prevent two defaults, but the service should
also serialize the business decision about which row should become default.

### What the Fix Changed

`delete(...)` and `setDefault(...)` now lock the user row before reading and
mutating delivery-address default state.

In beginner terms: all operations that choose or replace a user's default
address now enter the same one-at-a-time section for that user.

## 44. Account Deletion Could Revoke Sessions or Delete Avatar State Before Commit

### What This Feature Is For

Deleting an account should remove the user record and clean up related external
state, such as sessions and avatar files.

### How the Old System Behaved

The cleanup could run inside the same service method before the user-delete
transaction had committed.

### Why That Was a Bug

Session revocation and object-storage cleanup are external side effects. They
are not automatically rolled back if the database transaction rolls back.

That can leave the system inconsistent:

```text
database rollback:
  user still exists

external cleanup already ran:
  sessions revoked or avatar object scheduled for deletion
```

### What the Fix Changed

`deleteProfile(...)` now deletes the user row and registers cleanup to run after
the transaction commits.

If transaction synchronization is not active, the cleanup still runs
immediately, which keeps plain unit-test or non-transactional behavior simple.

Cleanup failures are logged instead of failing an already-committed account
delete.

## 45. Authentication Snapshot Assumed Authorities Were Always Loaded

### What This Feature Is For

`SingleUserProvider` builds an authentication snapshot that security uses for
login and token flows.

That snapshot includes user authorities.

### How the Old System Behaved

The conversion code streamed `user.getAuthorities()` directly.

### Why That Was a Bug

Authentication paths must load authorities intentionally. If a future repository
change accidentally returns a user without loaded authorities, the code would
fail with a less helpful null-pointer error.

More importantly, it would be unclear whether the bug was "user has no roles" or
"authentication loaded the wrong shape of user."

### What the Fix Changed

The conversion now fails fast with an explicit message if authorities are
absent:

```text
Objects.requireNonNull(user.getAuthorities(), "user authorities must not be null")
```

In beginner terms: authentication code now makes the required data shape clear.
If a future query forgets authorities, the failure points directly at that
contract.

## Verification Added

The fixes were covered with focused tests around the changed behavior:

- refresh-token blacklist TTL
- JWT issuer/audience validation
- JWT access/refresh purpose validation and separate signing-key enforcement
- delivery address whitespace-only field rejection
- user email normalization on registration and lookups
- shared email normalizer moved to common utility package
- user API boundary cleanup for internal exceptions
- authentication snapshot encoded-password naming and auth-only documentation
- Prometheus actuator endpoint admin-only protection
- hashed cache keys for email verification/password reset tokens
- login lockout email normalization
- password reset email normalization
- session revocation row locking
- OAuth verified-email handling
- refresh-token rotation locking
- login-attempt locking
- email verification/password reset token generation and namespacing
- external HTTP timeouts for Turnstile and Google OAuth
- CORS credentialed-origin validation
- OAuth token handoff
- account-state checks during JWT auth and refresh
- delivery address default selection, deletion, and concurrency behavior
- profile address validation and profile address mapping
- account lock expiration synchronization
- entity equality for transient user entities
- avatar MIME and magic-byte validation
- file-storage metadata deletion and outbox enqueueing
- file deletion outbox worker retry/no-storage behavior
- review outbox event-type scoping
- inbox consumer/event-type scoping

The latest focused verification run covered:

```text
com.zufar.icedlatte.user.**.*Test
CustomUserDetailsServiceTest
UserAuthenticationServiceTest
UserRegistrationServiceTest
OAuthLoginServiceTest
EmailVerificationServiceTest
PasswordResetServiceTest
DefaultCurrentUserProviderTest
ProductReviewManagerTest
ProductReviewsProviderTest
```

That latest run passed with:

```text
Tests run: 139, Failures: 0, Errors: 0, Skipped: 0
```

The generated `DeliveryAddressRequest` was also checked to confirm that
`@NotBlank` was actually generated for all delivery-address fields.

`mvn spotless:check` and `git diff --check` were also run after the latest user
and security boundary cleanup changes.
