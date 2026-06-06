# Image Pipeline Architecture

How the backend resolves product images — from S3 bucket to public URL in the API response.

---

## High-Level Flow

```
┌─────────────┐     startup      ┌──────────────────┐     S3 ListObjects     ┌─────────────┐
│  App Boot   │ ───────────────► │ ApplicationMigration │ ──────────────────► │  Supabase   │
└─────────────┘                  └──────────────────┘                        │  S3 Bucket  │
                                         │                                    └─────────────┘
                                         │ refreshBucketIndex()
                                         ▼
                                 ┌──────────────────┐
                                 │  file_metadata   │  (PostgreSQL table)
                                 │  table rebuilt   │
                                 └──────────────────┘
                                         │
         GET /api/v1/products            │
                 │                       │
                 ▼                       ▼
         ┌───────────────┐      ┌──────────────────┐      ┌─────────────┐
         │ ProductImage  │ ───► │ FileStorageService│ ───► │ file_metadata│
         │ Receiver      │      └──────────────────┘      │ (DB lookup) │
         └───────────────┘               │                └─────────────┘
                 │                       │
                 │                       ▼
                 │              ┌──────────────────┐
                 │              │ AwsObjectStorage  │
                 │              │   .getUrl()       │
                 │              └──────────────────┘
                 │                       │
                 ▼                       ▼
         ┌───────────────┐      Public URL returned
         │  Redis Cache  │      (publicUrlBase + S3 key)
         │ "productImage │
         │  Url" cache   │
         └───────────────┘
```

---

## Components

### 1. ApplicationMigration (startup)

**File:** `com.zufar.icedlatte.astartup.ApplicationMigration`

Runs on boot as an `ApplicationRunner`. Executes asynchronously on a virtual thread:

1. **(Optional) Upload** — if `migration.upload.enabled=true`, uploads local `seed/products/` directory to S3.
2. **Refresh metadata index** — calls `FileStorageService.refreshBucketIndex(bucketName)`.

**Async timing:** The migration runs on a virtual thread. The app may report healthy before the migration completes. This means:
- Requests hitting the API immediately after restart may get placeholder images
- Wait for `migration.metadata.refreshed` in logs before verifying
- The migration has a configurable timeout (`migration.timeout-minutes: 5`)

Key config:
```yaml
spring.aws.buckets.products: iced-latte-products
spring.aws.default-image-directory.products: seed/products
migration.upload.enabled: false   # true only for initial seeding
migration.timeout-minutes: 5
```

### Environment Variables (from .env.prod)

| Variable | Purpose | Example |
|----------|---------|---------|
| `AWS_PRODUCT_BUCKET` | Bucket name | `iced-latte-products` |
| `AWS_PUBLIC_URL_BASE` | Public URL prefix for image URLs | `https://fzvwwpzdudxrdzwbucaw.supabase.co/storage/v1/object/public/iced-latte-products` |
| `AWS_DEFAULT_PRODUCT_IMAGES_PATH` | Local seed directory | `seed/products` |
| `AWS_ACCESS_KEY_ID` | S3 access key | (from SOPS) |
| `AWS_SECRET_ACCESS_KEY` | S3 secret key | (from SOPS) |
| `AWS_ENDPOINT_URL` | S3 endpoint | `https://fzvwwpzdudxrdzwbucaw.storage.supabase.co/storage/v1/s3` |

### 2. FileStorageService.refreshBucketIndex()

**File:** `com.zufar.icedlatte.filestorage.FileStorageService`

1. Calls `objectStorage.listObjectKeys(bucketName)` → gets all S3 keys.
2. Parses each key with `toFileMetadata(fileName, bucketName)`.
3. **Deletes all existing** `file_metadata` rows for that bucket.
4. **Inserts fresh** rows from the parsed keys.

#### Key Parsing Logic (`toFileMetadata`)

```
S3 key:  "Cold_Brew_Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/card_logo.png"
          ─────────────────────────────────────────── ──────────────
          parts[0] (folder name)                       parts[1] (file)

Folder:  "Cold_Brew_Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9"
          ─────────────── ────────────────────────────────────
          product name    final underscore segment → UUID (product ID)
```

- Splits key by `/` → takes `parts[0]` (folder name).
- Reads the substring after the folder's last `_` as the product UUID.
- Returns `FileMetadataDto(relatedObjectId=UUID, bucketName, fileName=full_key)`.

**⚠️ Critical constraint:** Only ONE file per product folder. Multiple files → `Duplicate key` exception when the API tries to build a `Map<UUID, String>`.

#### Underscore-Safe Key Parsing

The parsing code does:
```
String folderName = fileName.split("/", 2)[0];
int uuidSeparatorIndex = folderName.lastIndexOf('_');
UUID.fromString(folderName.substring(uuidSeparatorIndex + 1));
```

This works because:
- Product names use **spaces** (e.g., `Vanilla Latte_uuid`)
- Product names may also use underscores (e.g., `Cold_Brew_Latte_uuid`)
- The UUID is always parsed from the final underscore-delimited segment

**Rule:** Keep the final underscore before the UUID. Earlier underscores are treated as part of the product name.

### 3. ProductImageReceiver

**File:** `com.zufar.icedlatte.product.api.ProductImageReceiver`

Two resolution paths:

| Method | Use case | Cache |
|--------|----------|-------|
| `getProductFileUrl(productId)` | Single product detail | `@Cacheable("productImageUrl")` keyed by productId |
| `getProductFileUrls(productIds)` | Product list/catalog | No cache (batch) |

Both delegate to `FileStorageService.findFileUrl(s)` → DB lookup → URL generation.

Falls back to `placeholderImageUrl` on error or missing metadata.

#### Naming Quirk

The `FileMetadataRepository` method is called `findAvatarInfoByRelatedObjectId` — this is a legacy name from when the system was only used for user avatars. It now serves both user avatars and product images. The `relatedObjectId` is the product UUID for products, or user UUID for avatars.

### Dual Image System

The backend has **two** independent image resolution mechanisms:

| System | DB Table | API Field | Resolution |
|--------|----------|-----------|------------|
| File metadata (S3 index) | `file_metadata` | `productFileUrl` (string) | S3 key → public URL at runtime |
| Product images (direct URLs) | `product_image` | `productImageUrls` (array) | URLs stored directly in DB |

- **`productFileUrl`** — the main card image. Resolved from S3 bucket contents via `file_metadata`. This is what `card_logo.png` populates.
- **`productImageUrls`** — a gallery of images for the product detail page. URLs are stored directly in the `product_image` table with a `position` column for ordering.

Currently, only `productFileUrl` is populated via the S3 workflow. The `product_image` table is for future use (e.g., multiple angles, lifestyle shots).

### 4. AwsObjectStorage.getUrl()

**File:** `com.zufar.icedlatte.filestorage.aws.AwsObjectStorage`

Two URL strategies:

1. **Public URL (preferred):** If `spring.aws.public-url-base` is set:
   ```
   publicUrlBase + "/" + bucketName + "/" + fileName
   → https://fzvwwpzdudxrdzwbucaw.supabase.co/storage/v1/object/public/iced-latte-products/Latte_uuid/card_logo.png
   ```

   If the configured base URL already ends with the bucket name, the bucket segment is not duplicated.

2. **Pre-signed URL (fallback):** If no public base, generates a time-limited signed URL via `S3Presigner`.

---

## Caching

| Cache name | Key | TTL | Invalidation |
|-----------|-----|-----|--------------|
| `productImageUrl` | productId (UUID) | Configured in Redis/Caffeine config | App restart clears it |
| `productImageUrls` | productId (UUID) | Same | App restart |

**Gotcha:** After uploading new images and restarting, the metadata index is rebuilt, but if Redis persists across restarts, stale URLs may be served. In practice, the prod Redis container restarts with the app (same compose), so cache is cleared.

---

## S3 Key Convention

```
<ProductName>_<UUID>/card_logo.png
```

- `ProductName` — human-readable, spaces allowed (e.g., `Vanilla Latte`).
- `UUID` — the product's primary key from the `products` table.
- `card_logo.png` — the single image file.

The product name in the folder is purely for human readability. The backend only uses the UUID portion.

---

## Failure Modes & Fixes

| Symptom | Cause | Fix |
|---------|-------|-----|
| 500 on `/api/v1/products` with `Duplicate key` | Multiple files in one product folder | Backend now selects one preferred file; remove obsolete S3 files only as cleanup |
| Images show placeholder | `file_metadata` empty or stale | Restart backend (triggers `refreshBucketIndex`) |
| Images show placeholder after restart | S3 unreachable or bucket empty | Check S3 credentials and bucket contents |
| Old images still showing | Redis cache serving stale URLs | Restart backend (clears cache) |
| `storage.key.skipped` in logs | S3 key doesn't match `Name_UUID/file` pattern | Fix the folder naming in S3 |
| `storage.key.invalid_uuid` in logs | Folder name has `_` but second part isn't a valid UUID | Fix folder name (likely underscore in product name) |
| Frontend returns 404 HTML for API call | Hitting wrong URL path | Use `/api/proxy/products` not `/api/v1/products` |

### The Exact Error We Hit (May 2026)

After uploading new `card_logo.png` files without deleting the old `.jpeg`/`.webp` files:

```
java.lang.IllegalStateException: Duplicate key fc88cd5d-5049-4b00-8d88-df1d9b4a3ce1
  (attempted merging values
    FileMetadataDto[relatedObjectId=fc88cd5d-..., fileName=Vanilla Latte_fc88cd5d-.../Vanilla Latte.jpeg]
    and
    FileMetadataDto[relatedObjectId=fc88cd5d-..., fileName=Vanilla Latte_fc88cd5d-.../card_logo.png])
```

**Root cause:** `getProductFileUrls()` called `findFileUrls()` which used `Collectors.toMap()` without a merge function — this threw on duplicate keys. The `file_metadata` table had two rows for the same product UUID because `refreshBucketIndex()` indexed both files in the folder.

**Fix:** `FileStorageService` now deduplicates metadata by `relatedObjectId` when reading existing rows and when refreshing the bucket index. It prefers `card_logo.webp`, then `card_logo.png`, then JPEG card logos, followed by other WEBP/PNG/JPEG files. Old files can still be deleted from S3 to reduce noise, but duplicate files should no longer take down product listing.

---

## Frontend -> Backend Routing

The frontend does not need to expose the backend API directly to browsers. The
normal production shape is a server-side proxy route in `Iced-Latte-Frontend`
that forwards allowed API paths to the backend service.

```text
Browser
  |
  | GET /api/proxy/products?size=10
  v
Frontend proxy route
  |
  | fetch(<backend-api-base>/products?size=10)
  | adds auth headers from server-side session/cookies when required
  v
Backend product API
  |
  | resolves image URLs from file_metadata
  v
Product response with imageUrl
```

Key details:

- Frontend proxy source code and image-domain allow-lists belong in
  `Iced-Latte-Frontend`.
- Production API base URLs, container aliases, and reverse-proxy routing belong
  in Vault.
- Backend ownership starts at the API request and image URL resolution path.
- If a browser receives HTML for a product API call, the request likely hit the
  wrong public route rather than the backend JSON endpoint.

---

## Runtime Network Shape

The backend is normally reached by another service on a private runtime network;
it should not require direct public exposure for product API traffic.

```text
                 public HTTPS
Browser ─────────────────────────► edge / frontend entrypoint
                                           |
                                           | server-side API proxy
                                           v
                                  private runtime network
                                           |
                                           v
                                  backend application
                                           |
                                           v
                                  product API + image metadata
```

Exact production network names, container aliases, host ports, health checks,
and shell commands are Vault-owned. Keep those values out of this backend repo
unless they are also part of the backend source contract.

---

## Deployment Checklist After Image Changes

Backend-owned checks:

1. Folder names follow `<Name>_<UUID>`.
2. Product image objects use the expected card-image file name.
3. `StorageKeyMetadataParser` accepts product names that contain underscores.
4. `FileStorageService.refreshBucketIndex` maps object keys into
   `file_metadata`.
5. The product API returns a resolved image URL or the configured placeholder.
6. `ProductImageReceiver` records `product.image.fallback` if URL resolution
   fails.

Vault-owned/runtime checks:

1. Upload or remove objects in the production bucket.
2. Refresh or restart the production backend if needed.
3. Check production logs, health checks, and reverse-proxy routing.
4. Verify the public website renders product images.

---

## Configuration Reference

### Backend

```yaml
spring:
  aws:
    public-url-base: ${AWS_PUBLIC_URL_BASE:}     # If set, uses public URLs (no pre-signing)
    buckets:
      products: ${AWS_PRODUCT_BUCKET}
    link-expiration-time: PT1H                   # Pre-signed URL TTL (fallback only)
    default-image-directory:
      products: ${AWS_DEFAULT_PRODUCT_IMAGES_PATH}

product:
  placeholder-image-url: "/assets/images/product-placeholder.png"  # Returned when no image found

migration:
  upload:
    enabled: false    # Set true only for initial seeding from local files
  timeout-minutes: 5  # Max time for the async migration to complete
```

### Frontend

| Configuration | Owner | Purpose |
|---------------|-------|---------|
| Backend API base for proxying | `Iced-Latte-Frontend` / Vault | Server-side proxy target for backend API calls |
| Image remote-source allow-list | `Iced-Latte-Frontend` | Allows frontend image optimization to load storage URLs |
| Public routing and reverse proxy | Vault | Runtime mapping from public traffic to services |

If the frontend image allow-list does not include the storage public URL host,
frontend image optimization can reject otherwise valid backend image URLs.
