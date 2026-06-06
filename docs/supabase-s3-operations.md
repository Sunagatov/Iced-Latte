# Supabase S3 Operations

How to upload, list, delete, and troubleshoot files in the Supabase Storage bucket used by Iced Latte product images.

Production credentials, host access, deployment commands, container logs, reverse proxy routing, and exact runtime values are owned by the private Vault repository. This backend document keeps the storage contract, local/operator workflow shape, and troubleshooting logic that are useful for application development.

---

## Connection Details

| Item | Value |
|------|-------|
| Bucket | `iced-latte-products` |
| S3 endpoint | `https://fzvwwpzdudxrdzwbucaw.storage.supabase.co/storage/v1/s3` |
| Public URL base | `https://fzvwwpzdudxrdzwbucaw.supabase.co/storage/v1/object/public/iced-latte-products` |
| Region | `eu-west-2` |
| Credentials | Stored outside this repository; read Vault for production/runtime access |

---

## Object Key Contract

Product image metadata is rebuilt from object-storage keys by `StorageKeyMetadataParser`.

Expected object key shape:

```text
<ProductName>_<ProductUUID>/card_logo.png
```

Examples:

```text
Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/card_logo.png
Cold_Brew_Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/card_logo.png
```

The parser reads the UUID after the last underscore in the folder name, so product names may contain underscores.

---

## Setup

### Prerequisites

```bash
# Install AWS CLI if not present (macOS)
pip3 install awscli
# or: brew install awscli
```

### Environment Variables

```bash
export AWS_ACCESS_KEY_ID=<access_key>
export AWS_SECRET_ACCESS_KEY=<secret_key>
export AWS_DEFAULT_REGION=eu-west-2
ENDPOINT="https://fzvwwpzdudxrdzwbucaw.storage.supabase.co/storage/v1/s3"
BUCKET="iced-latte-products"
```

Credentials must come from the approved runtime source, not from this repository.

> **Important:** The historical Supabase S3 workflow used `--no-verify-ssl` with AWS CLI because the endpoint certificate chain was not trusted by that CLI setup. Prefer fixing the local trust chain when possible; if Vault still documents `--no-verify-ssl` for the current runtime, follow Vault.

---

## Common Operations

### List all files

```bash
aws s3 ls "s3://$BUCKET/" --recursive \
  --endpoint-url "$ENDPOINT" --no-verify-ssl
```

### Upload a single file

```bash
aws s3 cp seed/products/Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/card_logo.png \
  s3://iced-latte-products/Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/card_logo.png \
  --endpoint-url "$ENDPOINT" --no-verify-ssl \
  --content-type "image/png"
```

### Bulk upload all product images

```bash
cd seed/products
for dir in */; do
  KEY="${dir}card_logo.png"
  [ -f "$KEY" ] && aws s3 cp "$KEY" "s3://$BUCKET/$KEY" \
    --endpoint-url "$ENDPOINT" --no-verify-ssl \
    --content-type "image/png" && echo "uploaded $KEY"
done
```

### Delete a single file

```bash
aws s3 rm "s3://$BUCKET/Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/old_file.jpeg" \
  --endpoint-url "$ENDPOINT" --no-verify-ssl
```

### Delete all files except `card_logo.png`

Use Python to handle filenames with spaces correctly:

```bash
python3 -c "
import re
import subprocess

endpoint = '$ENDPOINT'
bucket = '$BUCKET'
result = subprocess.run(
    ['aws', 's3', 'ls', f's3://{bucket}/', '--recursive',
     '--endpoint-url', endpoint, '--no-verify-ssl'],
    capture_output=True, text=True, check=False
)
for line in result.stdout.strip().split('\\n'):
    match = re.match(r'\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+\\d+\\s+(.*)', line)
    if match:
        key = match.group(1)
        if not key.endswith('card_logo.png'):
            subprocess.run(
                ['aws', 's3', 'rm', f's3://{bucket}/{key}',
                 '--endpoint-url', endpoint, '--no-verify-ssl'],
                check=False
            )
            print(f'Deleted: {key}')
"
```

### Verify a public URL

```bash
curl -s -o /dev/null -w "%{http_code}" \
  "https://fzvwwpzdudxrdzwbucaw.supabase.co/storage/v1/object/public/iced-latte-products/Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/card_logo.png"
# Should return 200
```

---

## Backend Refresh Flow

```text
Supabase bucket
      |
      | list object keys
      v
FileStorageService.refreshBucketIndex(...)
      |
      | parse <ProductName>_<UUID>/card_logo.png
      v
StorageKeyMetadataParser
      |
      | upsert metadata rows
      v
file_metadata
      |
      | resolve product image URLs
      v
ProductImageReceiver -> product API response
```

`refreshBucketIndex()` is the backend-owned point where object storage becomes application metadata. Runtime scheduling and production restart/refresh commands are Vault-owned.

---

## Content-Type Handling

When uploading PNGs, always set:

```text
image/png
```

Without the correct content type, Supabase may serve the file as `application/octet-stream`, which can break browser image rendering and frontend image optimization.

For older files that existed before image standardization:

| Extension | Expected content type |
|-----------|-----------------------|
| `.jpeg` / `.jpg` | `image/jpeg` |
| `.webp` | `image/webp` |
| `.png` | `image/png` |

The current product-card convention is one `card_logo.png` file per product folder.

---

## Old Files We Replaced

The bucket previously contained a mix of formats with inconsistent naming:

| Old pattern | Example |
|-------------|---------|
| `<ProductName>.jpeg` | `Vanilla Latte_<uuid>/Vanilla Latte.jpeg` |
| `card_logo.webp` | `Iced Coffee_<uuid>/card_logo.webp` |
| `<ProductName>.png` | `Hazelnut Latte_<uuid>/Hazwlnut Latte.png` |

These were standardized to `card_logo.png` per folder. Old files can be removed as cleanup, but the backend now prefers a single image instead of failing the whole product listing when multiple rows exist for one product UUID.

---

## Duplicate Files

Each product folder should contain one preferred product card image:

```text
<ProductName>_<UUID>/card_logo.png
```

If multiple objects exist for one product UUID, `ProductImageReceiver` should not throw a duplicate-key exception. The backend selects a preferred row and falls back to the configured placeholder if URL resolution fails. Duplicate objects are still operational noise and should be cleaned up through the approved storage workflow.

---

## Troubleshooting

### `aws s3 ls` splits filenames with spaces

The `awk '{print $4}'` trick breaks on keys like `Vanilla Latte_<uuid>/file.png`. Use the Python regex approach above; it parses the fixed-width date/time/size prefix and captures the full key.

### SSL certificate errors

If AWS CLI rejects the Supabase endpoint certificate, first check whether the local trust chain can be fixed. If the current Vault runbook still requires `--no-verify-ssl`, use it for the public product-image bucket workflow.

### Upload succeeds but file is not visible

1. Confirm the key follows `<ProductName>_<UUID>/card_logo.png`.
2. Confirm the object is in the `iced-latte-products` bucket.
3. Confirm the content type is `image/png`.
4. Refresh backend metadata using the runtime flow documented in Vault.
5. Check whether the product API returns the real URL or the placeholder.
6. If the old image still appears, clear the relevant runtime cache using the Vault-owned procedure.

### Product API returns placeholder

| Cause | Backend check |
|-------|---------------|
| `file_metadata` empty or stale | Run or verify `FileStorageService.refreshBucketIndex(...)` |
| S3 key skipped | Check for `storage.key.skipped` or `storage.key.invalid_uuid` logs |
| URL resolver failed | Check `product.image.fallback` metric tags |
| Missing object | List the bucket and verify the expected key exists |

### Product API fails after image changes

Check for storage parsing or URL resolution errors in application logs. Production log access is Vault-owned; locally, run the relevant tests or start the backend with local configuration.

### Frontend cannot load images

This repo owns the API response and storage metadata. The frontend repo owns image domain allow-lists, proxy routes, and Next.js image optimization behavior. Check `Iced-Latte-Frontend` for frontend-side image configuration.

---

## Runtime Boundary

The following items intentionally live in Vault instead of this backend repo:

- SOPS decryption commands and secret file paths.
- Production SSH host/user details.
- Container names and host-specific Docker commands.
- Task release commands.
- Reverse-proxy routing and shared network wiring.
- Production log commands and health checks.

Keep this document focused on the backend storage contract and repeatable object-storage operations.
