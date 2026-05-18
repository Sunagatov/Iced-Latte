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

**⚠️ Async timing:** The migration runs in a `CompletableFuture` on a virtual thread. The app reports healthy (healthcheck passes) **before** the migration completes. This means:
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
S3 key:  "Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/card_logo.png"
          ─────────────────────────────────────────── ──────────────
          parts[0] (folder name)                       parts[1] (file)

Folder:  "Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9"
          ───── ────────────────────────────────────
          packageName[0]  packageName[1] → UUID (product ID)
```

- Splits key by `/` → takes `parts[0]` (folder name).
- Splits folder by `_` → takes `packageName[1]` as the product UUID.
- Returns `FileMetadataDto(relatedObjectId=UUID, bucketName, fileName=full_key)`.

**⚠️ Critical constraint:** Only ONE file per product folder. Multiple files → `Duplicate key` exception when the API tries to build a `Map<UUID, String>`.

#### ⚠️ Underscore Split Bug Risk

The parsing code does:
```
String[] packageName = parts[0].split("_");
UUID.fromString(packageName[1]);
```

This works because:
- Product names use **spaces** (e.g., `Vanilla Latte_uuid`)
- The split produces `["Vanilla Latte", "uuid"]`

**But if a product name contained underscores** (e.g., `Cold_Brew_uuid`), the split would produce `["Cold", "Brew", "uuid"]` and `packageName[1]` would be `"Brew"` — not a valid UUID. The `UUID.fromString()` would throw `IllegalArgumentException`, and the key would be logged as `storage.key.invalid_uuid` and skipped.

**Rule:** Never use underscores in the product name portion of folder names. The single underscore before the UUID is the delimiter.

### 3. ProductImageReceiver

**File:** `com.zufar.icedlatte.product.api.filestorage.ProductImageReceiver`

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
   publicUrlBase + "/" + fileName
   → https://fzvwwpzdudxrdzwbucaw.supabase.co/storage/v1/object/public/iced-latte-products/Latte_uuid/card_logo.png
   ```

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
| 500 on `/api/v1/products` with `Duplicate key` | Multiple files in one product folder | Delete extras from S3, restart |
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

**Root cause:** `getProductFileUrls()` calls `findFileUrls()` which uses `Collectors.toMap()` — this throws on duplicate keys. The `file_metadata` table had two rows for the same product UUID because `refreshBucketIndex()` indexed both files in the folder.

**Fix:** Delete all old files from S3 so each folder has exactly one file, then restart.

---

## Frontend → Backend Routing

The frontend (Next.js) does NOT expose the backend API directly. It uses a **server-side proxy route**:

```
Browser → https://iced-latte.uk/api/proxy/products?size=10
                                    │
                                    ▼
         Next.js Route Handler: src/app/api/proxy/[...path]/route.ts
                                    │
                                    │ fetch(`${NEXT_PUBLIC_API_URL}/products?size=10`)
                                    │       = http://iced-latte-backend:8083/api/v1/products?size=10
                                    ▼
                          Backend container (port 8083)
```

Key details:
- `NEXT_PUBLIC_API_URL` in prod = `http://iced-latte-backend:8083/api/v1` (internal Docker network hostname)
- The proxy handles auth (reads JWT from httpOnly cookies, adds `Authorization` header)
- The proxy has a 30-second timeout (`FETCH_TIMEOUT_MS`)
- Path validation: only `[a-zA-Z0-9/_-]` characters allowed
- If the backend is down, the proxy returns `503 API unavailable`

**Common mistake:** Curling `https://iced-latte.uk/api/v1/products` returns a Next.js 404 page (HTML). The correct public URL is `https://iced-latte.uk/api/proxy/products`.

---

## Docker Network Architecture

```yaml
# From the production backend docker-compose.yml
services:
  backend:
    image: zufarexplainedit/iced-latte-backend:latest
    container_name: iced-latte-backend
    expose:
      - "8083"          # NOT ports: — only reachable on Docker network
    networks:
      reverse-network:
        aliases:
          - iced-latte-backend   # hostname other containers use

networks:
  reverse-network:
    external: true      # shared across all compose projects on the server
```

The backend is **not accessible from the host** via `localhost:8083`. To debug:
```bash
# From inside the container
docker exec iced-latte-backend wget -qO- 'http://localhost:8083/api/v1/products?size=1'
```

---

## Deployment Checklist (after image changes)

1. ✅ Ensure exactly **one file per product folder** in S3.
2. ✅ Folder names follow `<Name>_<UUID>` pattern.
3. ✅ Restart backend through the private deployment repository.
4. ✅ Wait ~30-60s, then verify logs show `migration.metadata.refreshed: bucket=iced-latte-products`.
5. ✅ Test a direct public URL returns HTTP 200.
6. ✅ Test the API returns the new URL (not placeholder).
7. ✅ Check the website renders images (not broken image icons).

---

## Configuration Reference

### Backend (application.yaml / .env.prod)

```yaml
spring:
  aws:
    public-url-base: ${AWS_PUBLIC_URL_BASE:}     # If set, uses public URLs (no pre-signing)
    buckets:
      products: ${AWS_PRODUCT_BUCKET}            # "iced-latte-products"
    link-expiration-time: PT1H                   # Pre-signed URL TTL (fallback only)
    default-image-directory:
      products: ${AWS_DEFAULT_PRODUCT_IMAGES_PATH}  # "seed/products"

product:
  placeholder-image-url: "/assets/images/product-placeholder.png"  # Returned when no image found

migration:
  upload:
    enabled: false    # Set true only for initial seeding from local files
  timeout-minutes: 5  # Max time for the async migration to complete
```

### Frontend (env vars)

| Variable | Purpose | Prod value |
|----------|---------|------------|
| `NEXT_PUBLIC_API_URL` | Backend API base (used by proxy) | `http://iced-latte-backend:8083/api/v1` |
| `NEXT_IMAGE_REMOTE_SOURCES` | Allowed domains for Next.js Image optimization | `https://fzvwwpzdudxrdzwbucaw.supabase.co` |

> **If `NEXT_IMAGE_REMOTE_SOURCES` doesn't include the Supabase domain**, Next.js `<Image>` will refuse to optimize/load the images and they'll appear broken.
