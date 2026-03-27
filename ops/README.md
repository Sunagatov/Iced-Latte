# Iced Latte Deployment Toolkit

Simple local deployment toolkit for manual deployments to Hetzner production server.

## Prerequisites

- [Task](https://taskfile.dev/) installed (`brew install go-task`)
- Docker Desktop running
- SSH key `~/.ssh/id_rsa` with access to Hetzner server
- Docker Hub credentials configured (`docker login`)
- `.env.prod` file in project root with deployment config

## Configuration

All configuration is in `.env.prod` (project root):

**Application config** (used by Spring Boot on server):
- Database, Redis, AWS, email, OAuth, etc.

**Deployment config** (used by deployment scripts):
- `DOCKER_IMAGE` - Docker Hub image name
- `DOCKER_TAG` - Image tag (can be overridden at runtime)
- `SSH_KEY`, `SSH_USER`, `SSH_HOST` - Server connection
- `REMOTE_APP_DIR` - App directory on server
- `REMOTE_COMPOSE_FILE` - Compose file name
- `HEALTHCHECK_URL` - Technical health endpoint
- `SMOKE_URL` - Business endpoint
- `HEALTH_TIMEOUT` - Health check timeout

## Quick Start

```bash
# Full deployment (build → push → sync-compose → deploy → smoke test)
task full-deploy

# Individual steps
task build          # Build ARM64 image
task push           # Push to Docker Hub
task sync-compose   # Copy compose file to server
task deploy         # Deploy via docker-compose
task smoke          # Run smoke tests (health + business)
task logs           # View production logs

# Combined steps
task build-and-push # Build + push only
```

## Runtime Overrides

### Custom Docker Tag
```bash
# Deploy with a specific tag
DOCKER_TAG=2026-03-18-1 task full-deploy

# Or individual steps
DOCKER_TAG=v4.0.3 task build
DOCKER_TAG=v4.0.3 task push
DOCKER_TAG=v4.0.3 task deploy
```

### Custom Config File
```bash
# Use a different config file (e.g., staging)
PROJECT_CONFIG=.env.staging task deploy
```

## Architecture

- **Server**: Hetzner Cloud ARM64 (116.203.197.65)
- **Deployment model**: Docker Compose on server
- **Image flow**: Build locally → Docker Hub → pull on server
- **No CI/CD**: All deployments are manual from local machine
- **External services**: PostgreSQL (Supabase), Redis (Upstash), S3 (Supabase Storage)
- **Network**: Uses default Docker Compose network (no custom network needed)

## Scripts

| Script | Purpose |
|--------|---------|
| `build.sh` | Build Docker image for linux/arm64 |
| `push.sh` | Push image to Docker Hub |
| `sync-compose.sh` | Copy local compose file to server |
| `deploy.sh` | SSH to server, cd to app dir, docker-compose pull + up -d |
| `logs.sh` | Show recent compose logs (use `-f` to follow) |
| `smoke.sh` | Check health + smoke endpoints with retries |

## Deployment Flow

1. **Build**: Creates ARM64 Docker image locally
2. **Push**: Uploads image to Docker Hub
3. **Sync Compose**: Copies `docker-compose.prod.yml` to server (prevents config drift)
4. **Deploy**: SSH to server, cd to `REMOTE_APP_DIR`, run `docker-compose pull` and `docker-compose up -d`
5. **Smoke**: Verifies deployment by calling health and business endpoints

## First-Time Server Setup

### 1. Copy .env.prod to server
```bash
scp -i ~/.ssh/id_rsa .env.prod root@116.203.197.65:/opt/iced-latte/.env.prod
```

Note: This will auto-create `/opt/iced-latte/` if it doesn't exist.

### 2. Deploy
```bash
task full-deploy
```

The `sync-compose` step (part of `full-deploy`) will:
- Auto-create remote app directory if needed
- Copy compose file to server
- Then deploy proceeds normally

## Troubleshooting

### Docker Desktop not running
```bash
open -a Docker && sleep 15
```

### SSH key not found
Verify key exists: `ls -la ~/.ssh/id_rsa`

### Deployment fails with "directory does not exist"
Create remote app directory:
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "mkdir -p /opt/iced-latte"
```

### Deployment fails with "compose file not found"
Sync compose file:
```bash
task sync-compose
```

### Smoke test fails
- Check container status: `ssh -i ~/.ssh/id_rsa root@116.203.197.65 "cd /opt/iced-latte && docker-compose ps"`
- Check logs: `task logs`
- Verify health URL: `curl https://api.iced-latte.uk/actuator/health`
- Verify smoke URL: `curl https://api.iced-latte.uk/api/v1/products?page=0&size=1`

### Tag override not working
Make sure to set the variable before the task command:
```bash
DOCKER_TAG=my-tag task build  # ✅ Correct
task build DOCKER_TAG=my-tag  # ❌ Wrong
```

## Files

```
ops/
├── projects/
│   ├── iced-latte.env          # Active config (gitignored)
│   └── iced-latte.env.example  # Template
└── scripts/
    ├── build.sh                # Build Docker image
    ├── push.sh                 # Push to Docker Hub
    ├── sync-compose.sh         # Sync compose file to server
    ├── deploy.sh               # Deploy via docker-compose
    ├── logs.sh                 # View compose logs
    └── smoke.sh                # Health + smoke tests
```

## Notes

- Server is ARM64 — always build with `--platform linux/arm64`
- `.env.prod` on server is managed separately (not deployed by these scripts)
- Compose file is synced automatically during `full-deploy`
- Health check retries for 60 seconds before failing
- All scripts support `PROJECT_CONFIG` environment variable for custom config paths
- Taskfile passes `PROJECT_CONFIG` explicitly to all scripts
