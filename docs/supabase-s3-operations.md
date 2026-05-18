# Supabase S3 Operations

How to upload, list, delete, and troubleshoot files in the Supabase Storage bucket used by Iced Latte.

---

## Connection Details

| Item | Value |
|------|-------|
| Bucket | `iced-latte-products` |
| S3 endpoint | `https://fzvwwpzdudxrdzwbucaw.storage.supabase.co/storage/v1/s3` |
| Public URL base | `https://fzvwwpzdudxrdzwbucaw.supabase.co/storage/v1/object/public/iced-latte-products` |
| Region | `eu-west-2` |
| Credentials | Stored in the private deployment repository (SOPS-encrypted) |
| Prod server | `root@116.203.197.65` (SSH key auth) |
| Container name | `iced-latte-backend` |
| Docker image | `zufarexplainedit/iced-latte-backend:latest` |

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
```

> **Important:** Always use `--no-verify-ssl` with Supabase S3 — their endpoint uses a certificate that the AWS CLI doesn't trust by default.

---

## Common Operations

### List all files

```bash
aws s3 ls s3://iced-latte-products/ --recursive \
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
  [ -f "$KEY" ] && aws s3 cp "$KEY" "s3://iced-latte-products/$KEY" \
    --endpoint-url "$ENDPOINT" --no-verify-ssl \
    --content-type "image/png" && echo "✓ $KEY"
done
```

### Delete a single file

```bash
aws s3 rm "s3://iced-latte-products/Latte_1e5b295f-8f50-4425-90e9-8b590a27b3a9/old_file.jpeg" \
  --endpoint-url "$ENDPOINT" --no-verify-ssl
```

### Delete all files EXCEPT card_logo.png

Use Python to handle filenames with spaces correctly:

```bash
python3 -c "
import subprocess, re

ENDPOINT = '$ENDPOINT'
result = subprocess.run(
    ['aws', 's3', 'ls', 's3://iced-latte-products/', '--recursive',
     '--endpoint-url', ENDPOINT, '--no-verify-ssl'],
    capture_output=True, text=True
)
for line in result.stdout.strip().split('\n'):
    m = re.match(r'\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+\d+\s+(.*)', line)
    if m:
        key = m.group(1)
        if not key.endswith('card_logo.png'):
            subprocess.run(
                ['aws', 's3', 'rm', f's3://iced-latte-products/{key}',
                 '--endpoint-url', ENDPOINT, '--no-verify-ssl'],
                capture_output=True
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

## Decrypting Credentials

The prod `.env` is SOPS-encrypted with age:

```bash
SOPS_AGE_KEY_FILE=~/.config/sops/age/keys.txt \
  sops --input-type dotenv --output-type dotenv -d \
  /path/to/private/deployment/env/.env.prod
```

---

## Task Commands (Deployment)

Deployment operations are run from the private deployment repository using [Task](https://taskfile.dev/):

```bash
cd /path/to/private/deployment/repo

# Restart container without pulling new image (uses existing image on server)
task release:restart:app APP=iced-latte

# Pull latest image and deploy
task release:deploy:app APP=iced-latte

# Build the Docker image locally
task release:build:app APP=iced-latte

# Push image to Docker Hub
task release:push:app APP=iced-latte

# Check app health
task release:health:app APP=iced-latte
```

> **Note:** The APP name is `iced-latte` (not `iced-latte-backend`). The deployment manifest maps this to the correct container.

---

## Checking Logs on Production

```bash
# SSH to server and check recent logs
ssh root@116.203.197.65 "docker logs iced-latte-backend --tail 50 2>&1"

# Check for migration success
ssh root@116.203.197.65 "docker logs iced-latte-backend 2>&1 | grep -i 'migration.metadata.refreshed'"

# Check for errors
ssh root@116.203.197.65 "docker logs iced-latte-backend 2>&1 | grep -i 'error\|exception' | tail -10"

# Check for duplicate key issues specifically
ssh root@116.203.197.65 "docker logs iced-latte-backend 2>&1 | grep -i 'Duplicate key\|IllegalState' | tail -5"

# Watch logs in real-time after restart
ssh root@116.203.197.65 "docker logs iced-latte-backend -f 2>&1" 
```

> **Timing:** After restart, the backend takes ~30-60 seconds to become healthy (healthcheck has `start_period: 60s`). The migration runs asynchronously on a virtual thread — the app may report healthy before the S3 index refresh completes.

---

## Content-Type Handling

When uploading PNGs, always set `--content-type "image/png"`. Without it, Supabase may serve the file as `application/octet-stream`, which can break browser image rendering and Next.js `<Image>` optimization.

For the old files that existed before our migration:
- `.jpeg` files → were served as `image/jpeg`
- `.webp` files → were served as `image/webp`
- `.png` files → served as `image/png`

All are now standardized to `card_logo.png` with `image/png` content type.

---

## Old Files We Replaced

The bucket previously contained a mix of formats with inconsistent naming:

| Old pattern | Example |
|-------------|---------|
| `<ProductName>.jpeg` | `Vanilla Latte_uuid/Vanilla Latte.jpeg` |
| `card_logo.webp` | `Iced Coffee_uuid/card_logo.webp` |
| `<ProductName>.png` | `Hazelnut Latte_uuid/Hazwlnut Latte.png` (note: typo in original) |

These were all deleted and replaced with `card_logo.png` per folder.

---

## Troubleshooting

### `aws s3 ls` splits filenames with spaces

The `awk '{print $4}'` trick breaks on keys like `Vanilla Latte_uuid/file.png`. Use the Python regex approach above instead — it parses the fixed-width date/time/size prefix and captures everything after.

### SSL certificate errors

Always pass `--no-verify-ssl`. Supabase S3 endpoint uses a cert chain that AWS CLI doesn't trust. This is safe for our use case (non-sensitive public images).

### Upload succeeds but file not visible on website

1. **Check for duplicate files** — if a product folder has >1 file, the backend crashes with `Duplicate key` in `ProductImageReceiver`. Delete old files first.
2. **Restart the backend** — the metadata index is built at startup only:
   ```bash
   cd /path/to/private/deployment/repo && task release:restart:app APP=iced-latte
   ```
3. **Check Redis cache** — `@Cacheable(cacheNames = "productImageUrl")` may serve stale URLs. A restart clears the cache.

### Backend returns 500 on /api/v1/products

Check logs for `Duplicate key` errors:
```bash
ssh root@116.203.197.65 "docker logs iced-latte-backend 2>&1 | grep -i 'Duplicate key\|IllegalState' | tail -5"
```

This means multiple files exist for one product in S3. Delete the extras and restart.

### Cannot curl backend from host

The backend container uses `expose: "8083"` (not `ports:`), meaning it's only reachable from other containers on the `reverse-network` Docker network. It is NOT accessible via `localhost:8083` on the host.

To hit the backend directly:
```bash
# From inside the container
docker exec iced-latte-backend wget -qO- 'http://localhost:8083/api/v1/products?size=1'

# From the host, through the frontend proxy
curl -s 'https://iced-latte.uk/api/proxy/products?size=1'
```

### Frontend returns 404 for /api/v1/products

The frontend does NOT proxy `/api/v1/*`. It proxies `/api/proxy/*`:
- Frontend URL: `https://iced-latte.uk/api/proxy/products` → Backend: `http://iced-latte-backend:8083/api/v1/products`
- The proxy route is at `src/app/api/proxy/[...path]/route.ts`
- It reads `NEXT_PUBLIC_API_URL` (e.g., `http://iced-latte-backend:8083/api/v1`) and appends the path segments

If you curl `https://iced-latte.uk/api/v1/products` directly, you'll get the Next.js 404 page (HTML), not JSON.

### Docker network architecture

```
┌─────────────────────────────────────────────────┐
│              reverse-network (external)          │
│                                                 │
│  ┌──────────────┐    ┌──────────────────────┐  │
│  │ reverse proxy│───►│ iced-latte-backend   │  │
│  │ (Caddy/nginx)│    │ expose: 8083         │  │
│  └──────────────┘    └──────────────────────┘  │
│         │                                       │
│         ▼                                       │
│  ┌──────────────┐                               │
│  │ frontend     │                               │
│  │ (Next.js)    │                               │
│  └──────────────┘                               │
└─────────────────────────────────────────────────┘
```

The reverse proxy terminates TLS and routes to containers by hostname alias. The backend is reachable as `iced-latte-backend` on the shared network.
