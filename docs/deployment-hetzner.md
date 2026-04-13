# Hetzner Deployment Playbook

A step-by-step guide to deploying a new project (FastAPI backend + React frontend) on the existing Hetzner server with Docker, nginx-proxy-manager, Supabase, and Cloudflare.

---

## Prerequisites

Before starting, make sure you have:

- Docker Desktop installed and running locally
- `task` CLI installed (`brew install go-task`)
- SSH access to the server: `ssh -i ~/.ssh/id_rsa root@116.203.197.65`
- A Docker Hub account (`zufarexplainedit`)
- A domain managed in Cloudflare
- A Supabase project (or any external Postgres)

---

## Table of Contents

1. [Project file structure](#1-project-file-structure)
2. [Backend Dockerfile](#2-backend-dockerfile)
3. [Frontend Dockerfile](#3-frontend-dockerfile)
4. [Frontend nginx.conf](#4-frontend-nginxconf)
5. [docker-compose.prod.yml](#5-docker-composeprodymll)
6. [Auth protection](#6-auth-protection)
7. [Maintainer ops scripts](#7-maintainer-ops-scripts)
8. [Taskfile](#8-taskfile)
9. [.env.prod](#9-envprod)
10. [Cloudflare DNS](#10-cloudflare-dns)
11. [First deploy](#11-first-deploy)
12. [SSL certificate](#12-ssl-certificate)
13. [nginx-proxy-manager config](#13-nginx-proxy-manager-config)
14. [Supabase — existing tables](#14-supabase--existing-tables)
15. [Verify everything works](#15-verify-everything-works)
16. [Ongoing deployments](#16-ongoing-deployments)
17. [Troubleshooting](#17-troubleshooting)

---

## 1. Project File Structure

Your project should have this layout before you start:

```
your-project/
├── backend/
│   ├── app/
│   │   ├── main.py          # FastAPI app, CORS config
│   │   ├── core/config.py   # Settings via pydantic-settings
│   │   └── ...
│   ├── alembic/             # DB migrations
│   ├── Dockerfile
│   └── pyproject.toml
├── frontend/
│   ├── src/
│   ├── Dockerfile           # Multi-stage: build + nginx
│   ├── nginx.conf           # SPA routing + API proxy
│   └── package.json
├── maintaner/
│   ├── ops/
│   │   ├── build.sh
│   │   ├── push.sh
│   │   ├── deploy.sh
│   │   ├── sync-compose.sh
│   │   ├── smoke.sh
│   │   └── logs.sh
│   ├── .env.prod            # ← gitignored, never committed
│   └── Taskfile.yml
├── docker-compose.yml       # Local dev
├── docker-compose.prod.yml  # Production
└── .gitignore
```

Make sure `.gitignore` includes:
```
.env
.env.prod
maintaner/.env.prod
```

---

## 2. Backend Dockerfile

Place this at `backend/Dockerfile`:

```dockerfile
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY . .

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -e .

CMD ["sh", "-c", "alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8000"]
```

Key points:
- The CMD runs `alembic upgrade head` before starting uvicorn — migrations run automatically on every container start
- If you are reusing an existing Supabase database that already has tables, see [section 14](#14-supabase--existing-tables)

---

## 3. Frontend Dockerfile

Place this at `frontend/Dockerfile`:

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=https://yourdomain.com
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

Key points:
- `VITE_API_BASE_URL` is baked into the JS bundle at build time — pass the correct production domain via `--build-arg` in `build.sh`
- The final image is just nginx serving static files — very small and fast
- The `nginx.conf` (next section) handles proxying API calls to the backend

---

## 4. Frontend nginx.conf

Place this at `frontend/nginx.conf`:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://your-backend-container-name:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /auth/ {
        proxy_pass http://your-backend-container-name:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /health {
        proxy_pass http://your-backend-container-name:8000;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Key points:
- Replace `your-backend-container-name` with the actual `container_name` from `docker-compose.prod.yml` (e.g. `lexora-backend`)
- `/api/`, `/auth/`, `/health` are proxied to the backend — this means nginx-proxy-manager only needs one entry pointing to the frontend, not two
- `try_files $uri $uri/ /index.html` is required for React Router — without it, refreshing any non-root page returns 404
- Both frontend and backend must be on the same Docker network (`reverse-network`) for the container name DNS to resolve

---

## 5. docker-compose.prod.yml

Place this at the project root:

```yaml
services:
  backend:
    image: yourdockerhubuser/your-backend:latest
    container_name: your-backend
    env_file: .env.prod
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8000/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    networks:
      - reverse-network

  frontend:
    image: yourdockerhubuser/your-frontend:latest
    container_name: your-frontend
    restart: unless-stopped
    networks:
      - reverse-network

networks:
  reverse-network:
    external: true
```

**Critical rules:**

1. **Do NOT add an `environment:` block with `${VAR}` interpolations.** When `docker compose` runs on the server, it tries to expand `${VAR}` from the shell environment — not from `.env.prod`. The shell has none of these vars, so they all become empty strings. Use only `env_file: .env.prod` and let Docker load everything from the file.

2. **Hardcode image names.** Do not use `${DOCKER_IMAGE:-fallback}` — it causes the same interpolation problem and also makes the frontend accidentally use the backend image name as a fallback.

3. **No postgres service.** Use Supabase or another external DB. The server does not run a local postgres in production.

4. **`reverse-network` must be external.** This network already exists on the server and is shared with nginx-proxy-manager. Do not define it as a new network.

---

## 6. Auth Protection

For a personal app exposed to the internet, implement cookie-based auth:

**Backend** (`backend/app/core/config.py`):
```python
app_password: str        # your login password
secret_key: str          # JWT signing key — generate with: openssl rand -hex 32
cookie_max_age: int = 60 * 60 * 24 * 30  # 30 days
```

**Backend** (`backend/app/routes/auth.py`):
- `POST /auth/login` — checks password, sets httpOnly cookie with JWT
- `POST /auth/logout` — deletes the cookie

**Backend** (`backend/app/api/deps.py`):
- `verify_session` dependency — reads the cookie, validates JWT, raises 401 if invalid
- Apply to all protected routers in `main.py`

**Frontend** (`frontend/src/lib/api.ts`):
- Add `credentials: 'include'` to every fetch call so cookies are sent
- On 401 response, redirect to `/login`

**Frontend** (`frontend/src/pages/LoginPage.tsx`):
- Simple password form, calls `POST /auth/login`
- On success, redirect to `/`

**Important:** `secure=True` on the cookie requires HTTPS. The cookie will not be sent over plain HTTP. Make sure SSL is set up (section 12) before testing login.

**Password rules:** `APP_PASSWORD` in `.env.prod` must use only letters, numbers, and hyphens. No single quotes, double quotes, backslashes, or forward slashes. These characters break bash `source` when the ops scripts load the env file.

---

## 7. Maintainer Ops Scripts

All scripts live in `maintaner/ops/`. They all start with the same header to load `.env.prod`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="${PROJECT_CONFIG:-$SCRIPT_DIR/../.env.prod}"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "❌ Config file not found: $CONFIG_FILE"
  exit 1
fi

source "$CONFIG_FILE"
```

### build.sh

Builds backend and/or frontend Docker images for `linux/arm64` (Hetzner ARM server).

Accepts a positional argument: `backend`, `frontend`, or `all`.

Uses `docker buildx build --platform linux/arm64`. Pass `--build-arg VITE_API_BASE_URL=https://yourdomain.com` for the frontend build.

### push.sh

Pushes backend and/or frontend images to Docker Hub.

Accepts: `backend`, `frontend`, or `all`.

### sync-compose.sh

Copies two files to the server via `scp`:
- `docker-compose.prod.yml` → `/opt/apps/yourapp/docker-compose.yml`
- `maintaner/.env.prod` → `/opt/apps/yourapp/.env.prod`

Creates the remote directory if it doesn't exist.

### deploy.sh

SSHes into the server and runs:
- `docker pull` for the target image(s)
- `docker compose up -d --no-deps` for the target service(s)

Accepts: `backend`, `frontend`, or `all`.

### smoke.sh

Polls `HEALTHCHECK_URL` and `SMOKE_URL` from `.env.prod` until they return HTTP 200 or timeout.

### logs.sh

SSHes into the server and tails `docker compose logs` for the remote app.

---

## 8. Taskfile

Place this at `maintaner/Taskfile.yml`. Run all tasks from the **project root**:
```bash
task -t maintaner/Taskfile.yml <task-name>
```

```yaml
version: '3'

vars:
  SCRIPTS_DIR: ops

tasks:
  build-backend:
    desc: Build backend Docker image (ARM64)
    cmds:
      - bash {{.SCRIPTS_DIR}}/build.sh backend

  build-frontend:
    desc: Build frontend Docker image (ARM64)
    cmds:
      - bash {{.SCRIPTS_DIR}}/build.sh frontend

  build:
    desc: Build both images (ARM64)
    cmds:
      - bash {{.SCRIPTS_DIR}}/build.sh all

  push-backend:
    desc: Push backend image to Docker Hub
    cmds:
      - bash {{.SCRIPTS_DIR}}/push.sh backend

  push-frontend:
    desc: Push frontend image to Docker Hub
    cmds:
      - bash {{.SCRIPTS_DIR}}/push.sh frontend

  push:
    desc: Push both images to Docker Hub
    cmds:
      - bash {{.SCRIPTS_DIR}}/push.sh all

  sync-compose:
    desc: Sync docker-compose.prod.yml and .env.prod to server
    cmds:
      - bash {{.SCRIPTS_DIR}}/sync-compose.sh

  deploy-backend:
    desc: Deploy only backend to production
    cmds:
      - bash {{.SCRIPTS_DIR}}/deploy.sh backend

  deploy-frontend:
    desc: Deploy only frontend to production
    cmds:
      - bash {{.SCRIPTS_DIR}}/deploy.sh frontend

  deploy:
    desc: Deploy both to production
    cmds:
      - bash {{.SCRIPTS_DIR}}/deploy.sh all

  smoke:
    desc: Run smoke test against production
    cmds:
      - bash {{.SCRIPTS_DIR}}/smoke.sh

  logs:
    desc: Show recent production logs
    cmds:
      - bash {{.SCRIPTS_DIR}}/logs.sh

  ship-backend:
    desc: Build → push → sync-compose → deploy backend
    cmds:
      - task: build-backend
      - task: push-backend
      - task: sync-compose
      - task: deploy-backend

  ship-frontend:
    desc: Build → push → deploy frontend
    cmds:
      - task: build-frontend
      - task: push-frontend
      - task: deploy-frontend

  full-deploy:
    desc: Build → push → sync-compose → deploy → smoke (everything)
    cmds:
      - task: build
      - task: push
      - task: sync-compose
      - task: deploy
      - task: smoke
```

**Important:** Use multiline `cmds:` format, not inline `cmds: [bash ...]`. The inline format causes YAML parse errors when task variable interpolation (`{{.SCRIPTS_DIR}}`) contains `}}`.

---

## 9. .env.prod

Create `maintaner/.env.prod`. This file is never committed to git.

```bash
########################################
# Database
########################################
POSTGRES_HOST=aws-0-eu-west-2.pooler.supabase.com
POSTGRES_PORT=6543
POSTGRES_DB=postgres
POSTGRES_USER=postgres.yourprojectref
POSTGRES_PASSWORD=yoursupabasepassword

########################################
# App
########################################
APP_HOST=0.0.0.0
APP_PORT=8000
APP_DEBUG=false

########################################
# Auth
########################################
APP_PASSWORD=your-password-letters-and-numbers-only
SECRET_KEY=paste-output-of-openssl-rand-hex-32-here

########################################
# Deployment
########################################
DOCKER_IMAGE=yourdockerhubuser/your-backend
DOCKER_TAG=latest

SSH_KEY=~/.ssh/id_rsa
SSH_USER=root
SSH_HOST=116.203.197.65

REMOTE_APP_DIR=/opt/apps/yourapp
REMOTE_COMPOSE_FILE=docker-compose.yml

########################################
# Smoke tests
########################################
HEALTHCHECK_URL=https://yourdomain.com/health
SMOKE_URL=https://yourdomain.com/api/something
HEALTH_TIMEOUT=10
```

Generate `SECRET_KEY`:
```bash
openssl rand -hex 32
```

**Password rules (very important):**
- `APP_PASSWORD` — use only `a-z A-Z 0-9 -`
- No `'` `"` `\` `/` `,` `.` or any other shell-special character
- These characters break `source .env.prod` in bash and will cause the backend container to crash with missing env vars

---

## 10. Cloudflare DNS

Go to **Cloudflare → your domain → DNS → Add record**:

| Type | Name | Content | Proxy status |
|------|------|---------|--------------|
| A | `yoursubdomain` | `116.203.197.65` | Proxied ☁️ (orange cloud) |

For example, for `app.yourdomain.com`:
- Name: `app`
- Content: `116.203.197.65`
- Proxy: **Proxied** (orange cloud, not grey)

The orange cloud means Cloudflare proxies the traffic — this gives you free HTTPS via Cloudflare's edge, DDoS protection, and hides your server IP.

Wait 1-2 minutes for DNS to propagate before proceeding.

---

## 11. First Deploy

Run these two commands from the project root:

```bash
task -t maintaner/Taskfile.yml ship-backend
task -t maintaner/Taskfile.yml ship-frontend
```

What `ship-backend` does:
1. Builds the backend Docker image for `linux/arm64`
2. Pushes it to Docker Hub
3. SCPs `docker-compose.prod.yml` and `.env.prod` to `/opt/apps/yourapp/` on the server
4. SSHes into the server, pulls the new image, restarts the backend container

What `ship-frontend` does:
1. Builds the frontend Docker image for `linux/arm64` with `VITE_API_BASE_URL` baked in
2. Pushes it to Docker Hub
3. SSHes into the server, pulls the new image, restarts the frontend container

After both commands complete, verify containers are running:
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker ps | grep yourapp"
```

You should see both `your-backend` and `your-frontend` containers with status `Up`.

---

## 12. SSL Certificate

The server uses nginx-proxy-manager (NPM) which has certbot inside. You need to issue a Let's Encrypt certificate for your domain.

**Step 1 — Create a temporary HTTP config** so the ACME challenge can pass.

SSH into the server:
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65
```

Create the file:
```bash
cat > /opt/apps/nginx-proxy-manager/data/nginx/proxy_host/yourapp.conf << 'EOF'
server {
    listen 80;
    listen [::]:80;
    server_name yourdomain.com;

    location /.well-known/acme-challenge/ {
        root /data/letsencrypt-acme-challenge;
    }

    location / {
        proxy_pass http://your-frontend:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
EOF
```

Reload nginx:
```bash
docker exec nginx-proxy-manager nginx -s reload
```

**Step 2 — Issue the certificate:**
```bash
docker exec nginx-proxy-manager certbot certonly \
  --webroot -w /data/letsencrypt-acme-challenge \
  -d yourdomain.com \
  --non-interactive --agree-tos \
  --email your@email.com
```

If successful you will see:
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/yourdomain.com/fullchain.pem
```

If it fails with `unauthorized`, it means the DNS hasn't propagated yet or the HTTP config wasn't reloaded. Wait a minute and retry.

---

## 13. nginx-proxy-manager Config

After getting the certificate, replace the temporary config with the full HTTPS config.

SSH into the server and overwrite the file:

```bash
cat > /opt/apps/nginx-proxy-manager/data/nginx/proxy_host/yourapp.conf << 'EOF'
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:MozSSL:10m;
    ssl_session_tickets off;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    access_log /data/logs/yourapp-access.log proxy;
    error_log /data/logs/yourapp-error.log warn;

    location /.well-known/acme-challenge/ {
        root /data/letsencrypt-acme-challenge;
    }

    location / {
        proxy_pass http://your-frontend:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}

server {
    listen 80;
    listen [::]:80;
    server_name yourdomain.com;

    location /.well-known/acme-challenge/ {
        root /data/letsencrypt-acme-challenge;
    }

    location / {
        return 301 https://yourdomain.com$request_uri;
    }
}
EOF
```

Reload nginx:
```bash
docker exec nginx-proxy-manager nginx -s reload
```

Key points:
- Replace `yourdomain.com` with your actual domain everywhere
- Replace `your-frontend` with your actual frontend container name
- The `http2 on` directive is the modern syntax — do not use `listen 443 ssl http2` (deprecated)
- Keep the `/.well-known/acme-challenge/` block in both HTTP and HTTPS servers — certbot needs it for renewals
- The `access_log` and `error_log` paths write to NPM's data volume — useful for debugging

---

## 14. Supabase — Existing Tables

If you are reusing a Supabase database that already has tables from a previous deployment (e.g. you wiped and redeployed, or you're sharing a Supabase project), alembic will crash on startup with:

```
sqlalchemy.exc.ProgrammingError: relation "your_table" already exists
```

This happens because alembic can't find its `alembic_version` tracking table, so it thinks no migrations have run and tries to create everything from scratch.

Fix — tell alembic the DB is already at the latest migration:

```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 \
  "cd /opt/apps/yourapp && docker compose run --rm backend alembic stamp head"
```

Then restart the backend:
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 \
  "cd /opt/apps/yourapp && docker compose restart backend"
```

---

## 15. Verify Everything Works

**Check containers are running:**
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker ps | grep yourapp"
```

**Check backend logs:**
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker logs your-backend --tail 20"
```
You should see `Uvicorn running on http://0.0.0.0:8000`.

**Check health endpoint:**
```bash
curl https://yourdomain.com/health
```
Should return `{"status": "ok"}`.

**Check the app in browser:**
Open `https://yourdomain.com` — you should see your frontend. If auth is implemented, you should be redirected to `/login`.

**Check nginx routing (API through frontend nginx):**
```bash
curl https://yourdomain.com/api/something
```
Should return a real API response, not an nginx 404.

---

## 16. Ongoing Deployments

After the initial setup, day-to-day deployments are:

**Deploy only backend** (after API/logic changes):
```bash
task -t maintaner/Taskfile.yml ship-backend
```

**Deploy only frontend** (after UI changes):
```bash
task -t maintaner/Taskfile.yml ship-frontend
```

**Deploy everything:**
```bash
task -t maintaner/Taskfile.yml full-deploy
```

**View production logs:**
```bash
task -t maintaner/Taskfile.yml logs
```

---

## 17. Troubleshooting

### Backend container keeps restarting

Check logs:
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker logs your-backend --tail 50"
```

Common causes:
- Missing env vars — `APP_PASSWORD` or `SECRET_KEY` not in `.env.prod`, or `.env.prod` wasn't synced to the server. Run `task sync-compose` and restart.
- Special characters in `APP_PASSWORD` — bash `source` fails silently, env vars come through empty. Use only letters, numbers, hyphens.
- Alembic trying to recreate existing tables — see [section 14](#14-supabase--existing-tables).
- Wrong `POSTGRES_PORT` — Supabase pooler uses `6543`, not `5432`.

### Frontend shows wrong app (another app on the server)

The server has a `default_server` nginx block that catches all unmatched traffic. Your `server_name yourdomain.com` block should take priority — but only if nginx has been reloaded after you created the config file.

Run:
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker exec nginx-proxy-manager nginx -s reload"
```

Then test with curl to confirm routing is correct before opening in browser:
```bash
curl -I http://yourdomain.com
# Should return 301 to https://

curl -I https://yourdomain.com
# Should return 200
```

### SSL certificate fails (unauthorized)

The ACME challenge failed — Let's Encrypt couldn't reach `http://yourdomain.com/.well-known/acme-challenge/`.

Checklist:
1. DNS A record is pointing to `116.203.197.65` — verify with `dig yourdomain.com`
2. The temporary HTTP nginx config was created and nginx was reloaded before running certbot
3. Cloudflare proxy is enabled (orange cloud) — if it's grey (DNS only), the challenge still works but make sure port 80 is open on the server

### API calls return 502 Bad Gateway

The frontend nginx can't reach the backend container. Causes:
- Backend container is not running — check `docker ps`
- Container name in `nginx.conf` doesn't match `container_name` in `docker-compose.prod.yml`
- Frontend and backend are not on the same Docker network — both must be on `reverse-network`

### docker compose warnings about unset variables

```
level=warning msg="The \"POSTGRES_DB\" variable is not set. Defaulting to a blank string."
```

These warnings appear when `docker compose pull` or `docker compose ps` runs — it reads the compose file and tries to interpolate `${VAR}` from the shell before `.env.prod` is loaded. They are harmless **as long as your compose file uses `env_file: .env.prod` and does NOT have an `environment:` block with `${VAR}` references**. If you have both, the `environment:` block wins with empty values and overrides what's in the file.

### Frontend shows blank page or 404 on refresh

`nginx.conf` is missing the SPA fallback. Make sure you have:
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```
Without this, React Router routes like `/login` or `/topics/1` return nginx 404 on hard refresh.
