# Hetzner Production Deployment

## Server
- Provider: Hetzner Cloud
- IP: `116.203.197.65`
- OS: Ubuntu (latest)
- RAM: 8 GB | Disk: 80 GB SSD
- SSH key: `~/.ssh/id_rsa` (MacBook Pro — fingerprint `66:b2:42:30:8d:af:8a:10:b9:7c:02:2b:da:d1:49:fa`)
  - ⚠️ The repo also contains `ssh-key-2026-03-04.key` — this is a **different key**, NOT registered on the server. Always use `~/.ssh/id_rsa`.
- SSH user: `root`

## Architecture Decision
Production uses **external managed services** — the server runs only the backend container:

| Service    | Provider              | Why local is not needed          |
|------------|-----------------------|----------------------------------|
| PostgreSQL | Supabase (pooler)     | `DATASOURCE_HOST` in `.env.prod` |
| Redis      | Upstash (SSL)         | `REDIS_HOST` in `.env.prod`      |
| S3 Storage | Supabase Storage      | `AWS_ENDPOINT_URL` in `.env.prod`|
| Email      | Gmail SMTP            | `MAIL_USERNAME` in `.env.prod`   |

This means **zero infrastructure containers** on the server — only `iced-latte-backend`.

## Deployment Model

No CI/CD pipeline. All deployments are done manually from the MacBook:
1. Build Docker image locally with `docker buildx`
2. Push to Docker Hub
3. SSH to server, pull new image, recreate container

> ⚠️ `cd.yml` in the repo targets **Oracle Cloud** (`OCI_HOST` secret) — it does NOT deploy to this Hetzner server and should be ignored.

## Files
- Env vars: `.env.prod` (copy to server as `/opt/iced-latte/.env.prod`)
- Compose: `docker-compose.prod.yml` — kept for reference only, **not used**
- nginx vhost: `/etc/nginx/sites-available/iced-latte-api` (managed on server, not in repo)
- TLS cert: `/etc/letsencrypt/live/api.iced-latte.uk/` (managed by Certbot)

## One-time Server Prerequisites

Before the first `docker run`, the Docker network must exist:
```bash
docker network create iced-latte-network
```
This only needs to be done once. Verify with `docker network ls`.

## JVM Tuning (already in Dockerfile)
- `-XX:MaxRAMPercentage=60.0` → ~4.8 GB heap out of 8 GB
- `-XX:+UseG1GC` + CDS archive for faster startup
- `-XX:+ExitOnOutOfMemoryError` → container restarts cleanly on OOM
- `-XX:+UseStringDeduplication`

## Ports
- `8083` → Spring Boot backend (HTTP, internal only — but currently **publicly reachable** since ufw is inactive)
- `80` / `443` → nginx reverse proxy → `127.0.0.1:8083`
- Public HTTPS endpoint: `https://api.iced-latte.uk` (Let's Encrypt cert, expires 2026-06-10, auto-renews)

> ⚠️ Port 8083 is exposed to the internet (`0.0.0.0:8083`). To lock it down to localhost only, either change the `docker run` binding to `-p 127.0.0.1:8083:8083` or enable ufw: `ufw allow 80 && ufw allow 443 && ufw allow 22 && ufw enable`.

## Deployment (run from MacBook)

> No CI/CD pipeline. Build and push from MacBook, then pull on server.

> ⚠️ Hetzner server is **ARM64** (`aarch64`). Always build for `linux/arm64`.

```bash
# 1 — Build and push image to Docker Hub
cd /Users/zufar/IdeaProjects/Iced-Latte
docker buildx build --platform linux/arm64 \
  -t zufarexplainedit/iced-latte-backend:latest \
  --push .

# 2 — Pull and recreate container on server
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "
  docker stop iced-latte-backend && docker rm iced-latte-backend
  docker pull zufarexplainedit/iced-latte-backend:latest
  docker run -d \
    --name iced-latte-backend \
    --network iced-latte-network \
    --env-file /opt/iced-latte/.env.prod \
    -p 8083:8083 \
    --restart unless-stopped \
    zufarexplainedit/iced-latte-backend:latest
"

# 3 — Verify
curl -s -o /dev/null -w "%{http_code}" https://api.iced-latte.uk/api/v1/products?page=0&size=1
```

### One-time MacBook setup (if Docker Desktop not running)
```bash
open -a Docker && sleep 15  # wait for daemon
docker login                # log in to Docker Hub once
```

### One-time server setup
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65
apt-get update && apt-get install -y docker.io
mkdir -p /opt/iced-latte
docker network create iced-latte-network
```

### Copy env file to server (when .env.prod changes)
```bash
scp -i ~/.ssh/id_rsa .env.prod root@116.203.197.65:/opt/iced-latte/.env.prod
```

## Useful Ops Commands (on server)

> ⚠️ Use `docker` directly — `docker-compose` v1.29.2 (Ubuntu default) crashes with `KeyError: 'ContainerConfig'` on images built with modern `docker buildx`.

```bash
# Tail logs
docker logs -f iced-latte-backend

# Recreate with latest image (after pushing new image from MacBook)
docker stop iced-latte-backend && docker rm iced-latte-backend
docker pull zufarexplainedit/iced-latte-backend:latest
docker run -d \
  --name iced-latte-backend \
  --network iced-latte-network \
  --env-file /opt/iced-latte/.env.prod \
  -p 8083:8083 \
  --restart unless-stopped \
  zufarexplainedit/iced-latte-backend:latest

# Stop
docker stop iced-latte-backend

# Verify env vars loaded correctly
docker exec iced-latte-backend env | grep -E 'GOOGLE_AUTH_REDIRECT|FRONTEND_URL|SPRING_PROFILES'
```

## Health Check
```bash
# HTTPS endpoint (primary)
curl https://api.iced-latte.uk/api/v1/products?page=0&size=1

# Verify Google OAuth redirect_uri is correct (must show api.iced-latte.uk, not iced-latte.uk)
curl -s -o /dev/null -w "%{redirect_url}" https://api.iced-latte.uk/api/v1/auth/google

# Verify container has correct env vars after deploy
ssh -i ~/.ssh/id_rsa root@116.203.197.65 \
  "docker exec iced-latte-backend env | grep -E 'GOOGLE_AUTH_REDIRECT|FRONTEND_URL|SPRING_PROFILES'"

# Direct HTTP (internal check only)
curl http://116.203.197.65:8083/api/v1/products?page=0&size=1
```

## HTTPS Setup (Nginx + Let's Encrypt)

Required for Google OAuth — Google only allows HTTPS redirect URIs.

### Step 1 — Add DNS A record
In your DNS provider (wherever `iced-latte.uk` is managed), add:
```
Type: A
Name: api
Value: 116.203.197.65
TTL: 300
```
This makes `api.iced-latte.uk` point to the Hetzner server.
Verify with: `dig +short A api.iced-latte.uk` — should return `116.203.197.65`.

### Step 2 — Install Nginx + Certbot on server
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65

apt-get update && apt-get install -y nginx certbot python3-certbot-nginx
```

### Step 3 — Create Nginx config
```bash
cat > /etc/nginx/sites-available/iced-latte-api << 'EOF'
server {
    listen 80;
    server_name api.iced-latte.uk;

    location / {
        proxy_pass http://127.0.0.1:8083;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
EOF

ln -sf /etc/nginx/sites-available/iced-latte-api /etc/nginx/sites-enabled/iced-latte-api
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx
```

> ⚠️ The `rm -f default` step is required — Ubuntu nginx ships with a `default` site that catches all traffic and will shadow the new vhost if left in place.

### Step 4 — Issue Let's Encrypt certificate
```bash
certbot --nginx -d api.iced-latte.uk --non-interactive --agree-tos -m zufar.sunagatov@gmail.com
```
Certbot rewrites the nginx config to add `listen 443 ssl` and inserts a second `server {}` block that redirects all HTTP traffic to HTTPS. The original HTTP-only config you wrote in Step 3 is replaced — this is expected and correct.

### Step 5 — Copy updated .env.prod and recreate container
```bash
# From MacBook — copy the env file
scp -i ~/.ssh/id_rsa .env.prod root@116.203.197.65:/opt/iced-latte/.env.prod

# On server — full recreate (docker restart does NOT re-read --env-file)
docker stop iced-latte-backend && docker rm iced-latte-backend
docker run -d \
  --name iced-latte-backend \
  --network iced-latte-network \
  --env-file /opt/iced-latte/.env.prod \
  -p 8083:8083 \
  --restart unless-stopped \
  zufarexplainedit/iced-latte-backend:latest
```

### Step 6 — Update Google Cloud Console
In [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials), edit the OAuth 2.0 client:
- **Authorized JavaScript origins**: add `https://api.iced-latte.uk`
- **Authorized redirect URIs**: add `https://api.iced-latte.uk/api/v1/auth/google/callback`
- Remove the old `https://iced-latte.uk/api/v1/auth/google/callback` entry

> Google Console changes take up to 5 minutes to propagate. Test with an incognito window after saving.

### Auto-renewal
Certbot installs a systemd timer automatically. Verify:
```bash
systemctl status certbot.timer
# Test renewal dry-run:
certbot renew --dry-run
```

---

## Lessons Learned

### SSH key confusion
The repo contains `ssh-key-2026-03-04.key` (fingerprint `ed:4a:24:5f`) — this key was **never registered** on the Hetzner server. The correct key is `~/.ssh/id_rsa` (fingerprint `66:b2:42:30`), which was added via the Hetzner Cloud Console. Always verify with:
```bash
ssh-keygen -l -E md5 -f ~/.ssh/id_rsa
# must match the fingerprint shown in Hetzner Console → SSH Keys
```

### `docker restart` does not re-read `--env-file`
`docker restart` reuses the original `docker run` parameters — env changes in `.env.prod` are silently ignored. Always do a full stop/rm/run after copying a new env file:
```bash
docker stop iced-latte-backend && docker rm iced-latte-backend
docker run -d --name iced-latte-backend --network iced-latte-network \
  --env-file /opt/iced-latte/.env.prod -p 8083:8083 \
  --restart unless-stopped zufarexplainedit/iced-latte-backend:latest
```
Verify the running container actually has the new value:
```bash
docker exec iced-latte-backend env | grep GOOGLE_AUTH_REDIRECT_URI
```

### Hetzner server is ARM64, not AMD64
The Hetzner server CPU is `aarch64` (ARM64). Building with `--platform linux/amd64` produces an image that runs under emulation with a warning:
```
WARNING: The requested image's platform (linux/amd64) does not match the detected host platform (linux/arm64/v8)
```
Always build for the correct platform:
```bash
docker buildx build --platform linux/arm64 -t zufarexplainedit/iced-latte-backend:latest --push .
```
Verify server arch anytime with: `ssh -i ~/.ssh/id_rsa root@116.203.197.65 "uname -m"` — should return `aarch64`.

### Docker Desktop must be running before `docker buildx build`
If Docker Desktop is not running, `docker buildx build` fails with:
```
ERROR: failed to connect to the docker API at unix:///Users/zufar/.docker/run/docker.sock
```
Fix: `open -a Docker && sleep 15` before building.

### Redis cache corruption after redeployment (`WRAPPER_ARRAY` error)
`GenericJacksonJsonRedisSerializer` with `OBJECT_AND_NON_CONCRETE` default typing embeds Java class names into cached JSON as `["java.lang.String", "value"]`. Any redeployment that changes the Jackson config makes the new code unable to read old cache entries, causing 500 errors on every cached endpoint.

Symptom in logs:
```
MismatchedInputException: Unexpected token START_OBJECT, expected START_ARRAY:
need Array value to contain WRAPPER_ARRAY type information
```

Fix applied: replaced `GenericJacksonJsonRedisSerializer` with `JacksonJsonRedisSerializer<T>` — one per cache, each knows its exact type. Plain JSON stored, no type metadata, no compatibility issues across deployments.

If stale keys are already in Redis, flush them before deploying:
```bash
redis-cli -h literate-ringtail-36568.upstash.io -p 6379 \
  -a "<REDIS_PASSWORD>" --tls KEYS "v1:*" | \
  xargs redis-cli -h literate-ringtail-36568.upstash.io -p 6379 \
  -a "<REDIS_PASSWORD>" --tls DEL
# Keys with spaces in name (e.g. "v1:brands::SimpleKey []") must be deleted explicitly:
redis-cli ... DEL "v1:brands::SimpleKey []" "v1:sellers::SimpleKey []"
```
Note: `xargs` splits on spaces — keys containing spaces are silently skipped. Always check `KEYS "v1:*"` returns empty after the flush.

### Google OAuth requires HTTPS redirect URIs
Google rejects `http://` redirect URIs for production OAuth clients. The backend was HTTP-only on port 8083 — Google would never redirect back to it. Solution: nginx + Let's Encrypt on a subdomain (`api.iced-latte.uk`) so the backend gets a valid HTTPS endpoint.

### Root cause of Google OAuth 404
The original `GOOGLE_AUTH_REDIRECT_URI` pointed to `https://iced-latte.uk/api/v1/auth/google/callback` — the Vercel frontend domain. Vercel has no route for `/api/v1/...` (the proxy is at `/api/proxy/[...path]`), so every OAuth callback returned 404. The fix: point the redirect URI to the backend's own HTTPS domain (`api.iced-latte.uk`).

### nginx `sites-enabled/default` blocks new configs
Ubuntu nginx ships with a `default` site that catches all traffic. Remove it when adding a named vhost:
```bash
rm -f /etc/nginx/sites-enabled/default
```

### DNS propagation is fast with low TTL
Setting TTL=300 (5 min) on the new A record meant `dig +short A api.iced-latte.uk` returned `116.203.197.65` within seconds of adding it in Cloudflare. Certbot's HTTP-01 challenge succeeded immediately after.
