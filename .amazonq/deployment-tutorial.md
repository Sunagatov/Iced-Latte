# Deploying Iced Latte to Hetzner Cloud in 2026
### A real-world tutorial — including every issue we hit and how we solved it

---

## Context

This tutorial documents the exact steps taken to deploy the [Iced Latte](https://github.com/Sunagatov/Iced-Latte) Spring Boot backend to a self-managed Hetzner Cloud server in 2026. It is written as a narrative — not just commands — so you understand the reasoning behind every decision.

**Why Hetzner?** The app was previously hosted on Render (free tier, 512 MB RAM) and Oracle Cloud (free tier, 1 GB RAM). Both failed with out-of-memory errors running a single Spring Boot instance with no other services. A Hetzner CX22 instance with 8 GB RAM and 80 GB SSD costs a few euros per month and handles the full stack comfortably.

---

## Stack overview

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL via Supabase (external) |
| Cache | Redis via Upstash (external, SSL) |
| Object storage | Supabase Storage (S3-compatible, external) |
| Email | Gmail SMTP (external) |
| Container runtime | Docker 28 |
| Server OS | Ubuntu 24.04 LTS (ARM64) |
| Server provider | Hetzner Cloud |
| Frontend | Next.js deployed on Vercel |

The key architectural insight: **all stateful services are external managed services**. The Hetzner server runs exactly one Docker container — the Spring Boot backend. No PostgreSQL, no Redis, no MinIO on the server itself. This keeps the server clean, cheap, and easy to replace.

---

## Prerequisites

- A Hetzner Cloud account with a server created (Ubuntu 24.04, ARM64, 8 GB RAM)
- An SSH key registered on the server (your `~/.ssh/id_rsa.pub` copied to Hetzner during server creation)
- Docker Desktop running locally on your MacBook
- A Docker Hub account (`zufarexplainedit` in this case)
- The Iced Latte repository cloned locally
- A `.env.prod` file with all production secrets

---

## Step 1 — Understand what you are deploying

Before touching any server, read the project's existing files:

- `Dockerfile` — multi-stage build: Maven compile → layer extraction → CDS training → runtime image
- `docker-compose.yml` — the local dev compose file (has postgres, redis, minio — not needed in prod)
- `.env.prod` — production environment variables

Reading `.env.prod` reveals the architecture immediately:

```
DATASOURCE_HOST=aws-1-eu-west-2.pooler.supabase.com   # external DB
REDIS_HOST=literate-ringtail-36568.upstash.io          # external Redis
AWS_ENDPOINT_URL=https://...supabase.co/storage/v1/s3  # external S3
```

This means the server only needs to run the backend container. No infrastructure containers needed.

---

## Step 2 — Create the production compose file

The existing `docker-compose.yml` is for local development — it spins up postgres, redis, and minio locally. For production we need a minimal file that just runs the backend and points it at the external services via `.env.prod`.

Create `docker-compose.prod.yml` in the project root:

```yaml
networks:
  iced-latte-network:
    name: iced-latte-network

services:
  backend:
    image: zufarexplainedit/iced-latte-backend:latest
    container_name: iced-latte-backend
    env_file: .env.prod
    ports:
      - '8083:8083'
    networks:
      - iced-latte-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8083/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

Key decisions here:
- `image:` pulls from Docker Hub instead of building on the server — keeps the server clean
- `env_file: .env.prod` injects all secrets at runtime — no secrets baked into the image
- `restart: unless-stopped` — container auto-restarts after server reboots or crashes
- `start_period: 60s` — gives Spring Boot time to start before health checks begin failing

---

## Step 3 — Verify SSH access

```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "uname -a && free -h"
```

**Issue encountered:** The project contained a key file `ssh-key-2026-03-04.key` in the repo root. We tried that first and got `Permission denied (publickey)`.

**Why it failed:** That key was generated on March 4 but its public key was never registered on the Hetzner server. The key that was registered was `~/.ssh/id_rsa` — the default Mac SSH key whose public half (`~/.ssh/id_rsa.pub`) was copied to Hetzner during server creation.

**Fix:** Use `~/.ssh/id_rsa` instead.

**What the server reported:**

```
Linux zufar-pet-projects-cloud-server 6.8.0-71-generic aarch64 GNU/Linux
Mem:  7.5Gi total, 325Mi used, 7.2Gi free
Disk: 75G total, 1.1G used, 71G available
```

Two important discoveries:
1. Architecture is `aarch64` (ARM64) — not x86/amd64
2. 7.2 GB RAM free — plenty for Spring Boot

---

## Step 4 — Discover the ARM64 problem

This is the most important issue in the whole deployment.

**The problem:** The existing Docker Hub image `zufarexplainedit/iced-latte-backend:latest` was built by the GitHub Actions CI pipeline running on GitHub's standard runners, which are `linux/amd64`. The Hetzner server is `linux/arm64`. These are incompatible architectures.

Running an amd64 image on an arm64 host is possible via QEMU emulation, but it is slow, unstable, and defeats the purpose of a clean deployment.

**How we confirmed it:**

```bash
docker manifest inspect zufarexplainedit/iced-latte-backend:latest | grep architecture
# "architecture": "amd64"
# "architecture": "unknown"
```

Only one real platform — amd64. No arm64 variant.

**Two options considered:**

| Option | Pros | Cons |
|---|---|---|
| Build on the server | Simple, no Docker Hub push needed | Installs Maven/JDK on server, makes it "dirty", slow on first build |
| Build on Mac, push to Docker Hub | Server stays clean, fast pull | Requires Docker Hub credentials, one extra step |

**Decision: build on the Mac.** The MacBook is Apple Silicon — also ARM64. So a native arm64 image built locally will run natively on the Hetzner server with zero emulation overhead. The server stays clean with only Docker installed.

---

## Step 5 — Build the ARM64 image and push to Docker Hub

```bash
docker buildx build \
  --platform linux/arm64 \
  --tag zufarexplainedit/iced-latte-backend:latest \
  --push \
  /Users/zufar/IdeaProjects/Iced-Latte
```

**What `buildx` does:** Docker's standard `build` command builds for the host's native architecture. `buildx` is the extended builder that supports cross-platform builds and can push directly to a registry in one step with `--push`.

**What happened during the build:**

The Dockerfile has four stages:

1. **build** — pulls `zufarexplainedit/iced-latte-deps:latest` (the Maven deps base image), copies `pom.xml` and `src/`, runs `mvn package -DskipTests`. Took ~2 minutes. Maven downloaded a few missing plugin JARs (the deps image covers most dependencies but not all).

2. **extract** — runs `java -Djarmode=layertools -jar app.jar extract` to split the fat JAR into layers: `dependencies/`, `spring-boot-loader/`, `snapshot-dependencies/`, `application/`. This enables Docker layer caching — the `dependencies/` layer (which rarely changes) is cached separately from `application/` (which changes every build).

3. **cds-train** — runs the app with `-XX:ArchiveClassesAtExit=app-cds.jsa` to generate a Class Data Sharing archive. CDS pre-loads class metadata so the JVM starts faster. During this stage the app tried to start with `prod` profile but had no `REDIS_HOST` env var — it failed with a Spring context error. This is expected and intentional — the `|| true` at the end of the RUN command means the stage succeeds regardless. The CDS archive is still written before the failure.

4. **runtime** — copies the layered JARs and the CDS archive into a clean `eclipse-temurin:25-jre-alpine` image. Final image size is lean — no Maven, no source code, no test classes.

**Warning seen (not an error):**

```
InvalidBaseImagePlatform: Base image zufarexplainedit/iced-latte-deps:latest
was pulled with platform "linux/amd64", expected "linux/arm64"
```

This warning appears because the deps base image on Docker Hub is amd64. Buildx handled the cross-compilation correctly anyway — the warning is informational, not fatal. The final image is native arm64.

**Subsequent builds are fast:** On the second build (after the JVM fix below), all layers were `CACHED` because only the ENTRYPOINT changed. The push took 6 seconds instead of several minutes.

**Build result:** Image pushed to `docker.io/zufarexplainedit/iced-latte-backend:latest` as arm64.

---

## Step 6 — Install Docker on the server

```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 bash << 'EOF'
apt-get update -qq
apt-get install -y docker.io docker-compose
systemctl enable --now docker
mkdir -p /opt/iced-latte
EOF
```

**Issue encountered:** First attempt used `docker-compose-plugin` as the package name. This failed:

```
E: Unable to locate package docker-compose-plugin
```

**Why:** `docker-compose-plugin` is the package name for Docker's modern Compose V2 plugin when installed via Docker's own apt repository. Ubuntu's default apt repository ships the older standalone `docker-compose` (Python-based, V1) under a different package name.

**Fix:** Use `docker-compose` instead of `docker-compose-plugin`. This installs Compose V1 (1.29.2).

The debconf warnings about "unable to initialize frontend: Dialog" are harmless — they appear because we're running apt non-interactively over SSH without a TTY.

---

## Step 7 — Copy files to the server

```bash
scp -i ~/.ssh/id_rsa \
  .env.prod \
  docker-compose.prod.yml \
  root@116.203.197.65:/opt/iced-latte/
```

We copy only two files:
- `.env.prod` — all runtime secrets (DB password, Redis password, JWT secrets, etc.)
- `docker-compose.prod.yml` — the minimal single-service compose file

We do **not** copy the source code, the JAR, or the full `docker-compose.yml`. The server pulls the pre-built image from Docker Hub.

`/opt/iced-latte/` is the conventional location for self-managed application data on Linux — under `/opt` (optional software), in a named subdirectory.

---

## Step 8 — Start the container

```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "
  docker pull zufarexplainedit/iced-latte-backend:latest
  docker run -d \
    --name iced-latte-backend \
    --network iced-latte-network \
    --env-file /opt/iced-latte/.env.prod \
    -p 8083:8083 \
    --restart unless-stopped \
    zufarexplainedit/iced-latte-backend:latest
"
```

> **Why `docker run` and not `docker-compose up`?**
> See Issue #6 below — `docker-compose` v1.29.2 crashes with `KeyError: 'ContainerConfig'` on images built with modern `docker buildx`. Use `docker run` directly for all deployments.

Docker pulled 11 layers totalling ~200 MB. The container started immediately.

```
CONTAINER ID   IMAGE                                        STATUS
391b2e192fee   zufarexplainedit/iced-latte-backend:latest   Up 5 seconds (health: starting)
```

Port mapping: `0.0.0.0:8083->8083/tcp` — the backend is reachable on the public IP.

---

## Step 9 — Verify Spring Boot started correctly

After 35 seconds:

```bash
docker logs iced-latte-backend --tail 10
```

Key log lines confirming a healthy start:

```
INFO  c.z.i.c.config.RedisConfig - cache.mode: Redis
INFO  o.s.b.tomcat.TomcatWebServer - Tomcat started on port 8083 (http)
INFO  c.z.i.IcedLatteApplication - Started IcedLatteApplication in 17.425 seconds
INFO  c.z.i.a.ApplicationMigration - migration.upload.skipped: reason=disabled
INFO  c.z.i.a.ProductsReviewsAndRatingInfoUpdater - migration.ratings.finish: durationMs=215
INFO  c.z.i.a.ApplicationMigration - migration.metadata.saved
```

What each line tells us:
- `cache.mode: Redis` — connected to Upstash Redis successfully
- `Tomcat started on port 8083` — HTTP server is up
- `Started in 17.425 seconds` — healthy startup time for a Spring Boot app of this size
- `migration.upload.skipped: reason=disabled` — `MIGRATION_UPLOAD_ENABLED=false` in `.env.prod`, correct
- `migration.ratings.finish` — Liquibase ran, DB is connected, ratings recalculated
- `migration.metadata.saved` — S3/Supabase Storage is reachable

---

## Step 10 — Smoke test the API

```bash
curl "http://116.203.197.65:8083/api/v1/products?page=0&size=2"
```

Response: a JSON array of coffee products with names, prices, ratings, AI summaries, and Supabase image URLs. The backend is live and serving real data.

---

## Step 11 — Connect the Vercel frontend

The Next.js frontend deployed on Vercel needs to know where the backend API is. In Vercel's environment variable settings:

```
NEXT_PUBLIC_API_URL = http://116.203.197.65:8083/api/v1
```

**Important caveat — mixed content:** Vercel serves the frontend over `https://`. Browsers enforce a security policy called "mixed content" that blocks `https://` pages from making requests to `http://` endpoints. This means API calls will be silently blocked in production browsers.

**The fix (when you're ready):** Put Nginx in front of the backend with a Let's Encrypt TLS certificate:

```bash
apt install -y nginx certbot python3-certbot-nginx
certbot --nginx -d your-domain.com
```

Then configure Nginx to proxy `https://your-domain.com` → `http://localhost:8083`. After that, set:

```
NEXT_PUBLIC_API_URL = https://your-domain.com/api/v1
```

---

## Issue #6 — OOM crash under real traffic (post-deployment)

This issue appeared after the app was running. It is the most instructive part of the whole deployment.

### What happened

The app started fine, served a few requests, then crashed:

```
WARN  c.z.i.c.m.SlowQueryAspect - db.slow_query: method=ProductReviewRepository.findAllProductReviews(..), durationMs=529
Terminating due to java.lang.OutOfMemoryError: Metaspace
```

The container restarted automatically (due to `restart: unless-stopped`) and came back up — but would crash again under any real load.

### Why it crashed — Metaspace explained

The JVM memory model has two main regions:

- **Heap** — where your objects live (Spring beans, request data, etc.)
- **Metaspace** — where the JVM stores class metadata: every class definition, method bytecode, annotations, reflection data

Metaspace grows as the JVM loads more classes. A Spring Boot app with Hibernate, MapStruct, OpenAPI codegen, LangChain4j, Spring Security, Redis client, and S3 client loads a very large number of classes — especially under the first real traffic when lazy-loaded components initialize.

The original Dockerfile had:

```
-XX:MaxMetaspaceSize=128m
```

This is a hard ceiling. When Metaspace hit 128 MB, the JVM threw `OutOfMemoryError: Metaspace` and died. It had nothing to do with heap — the heap was fine. It was purely the class metadata space being too small.

### Why that setting existed

Those JVM flags were written for the **Render/Oracle free tier** deployments — survival settings for a memory-starved environment with 512 MB–1 GB total RAM. On those platforms you had to cap everything tightly just to fit the JVM into the available memory. `128m` for Metaspace, `G1HeapRegionSize=4m` (meant for heaps over 4 GB — wrong here), all of it was defensive tuning for a constrained environment.

When the project moved to Hetzner with 8 GB RAM, nobody updated the JVM flags. The constraints were gone but the caps remained.

### The fix

Remove `-XX:MaxMetaspaceSize` entirely. On a server with 8 GB RAM there is no reason to cap Metaspace. The JVM will grow it to whatever it needs and stop there naturally.

Also removed `-XX:G1HeapRegionSize=4m` — this flag is only beneficial for heaps in the 4–8 GB range. With `MaxRAMPercentage=60.0` on an 8 GB server the heap is ~4.8 GB, which is borderline, but the flag was causing more harm than good by forcing large region sizes that increase GC pause times for a web app with many small short-lived objects.

**Final ENTRYPOINT in Dockerfile:**

```dockerfile
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=60.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-XX:SharedArchiveFile=app-cds.jsa", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "org.springframework.boot.loader.launch.JarLauncher"]
```

What each remaining flag does:

| Flag | Why keep it |
|---|---|
| `-XX:+UseContainerSupport` | Makes JVM respect Docker's cgroup memory limit instead of seeing the host's full 8 GB |
| `-XX:MaxRAMPercentage=60.0` | Caps heap at 60% of container limit (~4.8 GB) — leaves room for Metaspace, threads, native memory |
| `-XX:+ExitOnOutOfMemoryError` | If heap genuinely OOMs, crash cleanly so Docker's `restart: unless-stopped` recovers |
| `-XX:+UseG1GC` | Best GC for latency-sensitive web apps with mixed object lifetimes |
| `-XX:+UseStringDeduplication` | G1 feature that deduplicates identical String objects — useful for a product catalog with repeated names/descriptions |
| `-XX:SharedArchiveFile=app-cds.jsa` | CDS archive for faster startup — pre-loaded class metadata |
| `-Djava.security.egd=...` | Faster random number generation — avoids startup delays on Linux |

### Redeployment issue — docker-compose v1 crash

After fixing the Dockerfile and rebuilding the image, the redeploy via `docker-compose` failed:

```
KeyError: 'ContainerConfig'
Traceback:
  File "compose/service.py", line 1579, in get_container_data_volumes
    container.image_config['ContainerConfig'].get('Volumes') or {}
```

**Why:** `docker-compose` v1.29.2 (the Python-based version shipped with Ubuntu's default apt repo) does not understand the image manifest format produced by modern `docker buildx`. It tries to read a `ContainerConfig` key that no longer exists in the new OCI image spec.

**Fix:** Bypass `docker-compose` entirely and use `docker run` directly:

```bash
docker stop iced-latte-backend && docker rm iced-latte-backend
docker pull zufarexplainedit/iced-latte-backend:latest
docker run -d \
  --name iced-latte-backend \
  --network iced-latte-network \
  --env-file /opt/iced-latte/.env.prod \
  -p 8083:8083 \
  --restart unless-stopped \
  zufarexplainedit/iced-latte-backend:latest
```

---

## Step 12 — Verify JVM memory under real load

After fixing the JVM flags, we verified the fix was actually working by measuring real memory under load — not just at idle.

### Load generation (from MacBook)

```bash
for i in $(seq 1 100); do
  curl -s "http://116.203.197.65:8083/api/v1/products?page=0&size=20&sort=averageRating" > /dev/null &
  curl -s "http://116.203.197.65:8083/api/v1/products?page=1&size=20" > /dev/null &
  curl -s "http://116.203.197.65:8083/api/v1/products/fc88cd5d-5049-4b00-8d88-df1d9b4a3ce1" > /dev/null &
done
wait
```

300 concurrent requests across product listing and product detail endpoints.

### Memory measurement (on server)

```bash
docker stats iced-latte-backend --no-stream
docker exec iced-latte-backend sh -c 'cat /proc/1/status | grep -E "VmRSS|VmPeak|VmHWM"'
docker exec iced-latte-backend sh -c 'cat /proc/1/smaps_rollup'
```

### Real numbers under load

| Metric | Value | What it means |
|---|---|---|
| RSS (current) | **568 MB** | Total physical RAM the JVM process is using |
| VmHWM (peak ever) | **568 MB** | Highest RSS ever recorded — same as current, we caught the peak |
| VmPeak (virtual) | **6.5 GB** | Virtual address space — irrelevant, JVM reserves huge virtual ranges upfront |
| Anonymous memory | **539 MB** | Heap + Metaspace + thread stacks — actual JVM-managed memory |
| File-backed memory | **29 MB** | Shared libs, JRE class files |
| Swap | **0 MB** | Nothing swapped — healthy |
| Container RAM % | **6.86%** | Using 530 MB out of 7.5 GB available |

### What this tells us

The entire JVM — heap, Metaspace, thread stacks, everything — sits at **~568 MB** under real concurrent load. The server has 7+ GB of headroom remaining. Metaspace settled naturally at roughly 200–250 MB (part of the 539 MB anonymous total) without any artificial ceiling.

The old `128m` cap was killing the app because Metaspace alone needs ~200–250 MB for this application. 128m is simply below the natural settling point.

---

## Final architecture

```
┌─────────────────────────────────────────────────────┐
│  Vercel (Frontend)                                  │
│  Next.js → NEXT_PUBLIC_API_URL                      │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP (port 8083)
                       ▼
┌─────────────────────────────────────────────────────┐
│  Hetzner CX22 — 116.203.197.65                      │
│  Ubuntu 24.04 ARM64 — 8 GB RAM — 80 GB SSD          │
│                                                     │
│  ┌─────────────────────────────────────────────┐    │
│  │  Docker container: iced-latte-backend       │    │
│  │  Image: zufarexplainedit/iced-latte-backend │    │
│  │  JVM: Java 25, G1GC, MaxRAM 60%, CDS        │    │
│  │  RSS under load: ~568 MB (6.86% of 8 GB)    │    │
│  └──────┬──────────┬──────────────┬────────────┘    │
└─────────┼──────────┼──────────────┼─────────────────┘
          │          │              │
          ▼          ▼              ▼
   Supabase DB   Upstash Redis  Supabase S3
   (PostgreSQL)  (SSL, cloud)   (object storage)
```

---

## All issues summary

| # | Issue | Root cause | Fix |
|---|---|---|---|
| 1 | SSH `Permission denied` with `ssh-key-2026-03-04.key` | That key's public half was never registered on Hetzner | Use `~/.ssh/id_rsa` — the key registered during server creation |
| 2 | Existing Docker Hub image incompatible with server | GitHub Actions CI builds amd64; Hetzner server is ARM64 | Build ARM64 image locally on Apple Silicon Mac with `docker buildx --platform linux/arm64` |
| 3 | `apt` package `docker-compose-plugin` not found | Ubuntu's default repo ships standalone `docker-compose`, not Docker's plugin variant | Use `docker-compose` package name instead |
| 4 | CDS training stage logs a Spring context error | `REDIS_HOST` env var not available at image build time | Intentional: `\|\| true` in Dockerfile suppresses the exit code; CDS archive is still written |
| 5 | Buildx warning about amd64 base image | `iced-latte-deps` base image on Docker Hub is amd64-only | Warning only — buildx cross-compiled correctly; final image is native arm64 |
| 6 | `OutOfMemoryError: Metaspace` under real traffic | `-XX:MaxMetaspaceSize=128m` — a leftover survival setting from Render/Oracle free tier, too small for this app | Remove the cap entirely — JVM manages Metaspace freely, settles at ~200–250 MB naturally |
| 7 | `docker-compose` crashes with `KeyError: 'ContainerConfig'` on redeploy | `docker-compose` v1.29.2 (Ubuntu default) incompatible with OCI images built by modern `docker buildx` | Use `docker run` directly instead of `docker-compose up` |

---

## Day-2 operations

### View live logs
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65
docker logs -f iced-latte-backend
```

### Deploy a new version (full workflow from MacBook)
```bash
# 1. Build new ARM64 image and push to Docker Hub
docker buildx build \
  --platform linux/arm64 \
  --tag zufarexplainedit/iced-latte-backend:latest \
  --push \
  /Users/zufar/IdeaProjects/Iced-Latte

# 2. Pull and restart on server
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
```

### Check container status and memory
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "
  docker ps
  docker stats iced-latte-backend --no-stream
"
```

### Health check
```bash
curl http://116.203.197.65:8083/actuator/health
```

### Restart without pulling a new image
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker restart iced-latte-backend"
```

### Stop
```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker stop iced-latte-backend"
```

---

## Why this approach beats Render and Oracle free tiers

| | Render free | Oracle free | Hetzner CX22 |
|---|---|---|---|
| RAM | 512 MB | 1 GB | 8 GB |
| Result with Iced Latte | OOM crash | OOM crash | 568 MB used, stable |
| Cost | Free (but unusable) | Free (but unusable) | ~€4/month |
| Architecture | amd64 | aarch64 | aarch64 |
| Sleep on inactivity | Yes (30s cold start) | No | No |
| Full control | No | Partial | Yes |

The JVM at steady state under real traffic uses ~568 MB total. That already exceeds Render's 512 MB hard limit for the entire container. Oracle's 1 GB is technically enough for the JVM alone but leaves almost nothing for the OS, Docker daemon, and any memory spikes. Hetzner's 8 GB means the app uses 6.86% of available RAM and has enormous headroom.

---

## Server hardening (do this right after first SSH login)

The fresh Hetzner server has no firewall, Docker logs have no size limit, and the secrets file has default permissions. Three things to fix immediately.

### 1 — UFW firewall

Port 8083 is currently exposed to the entire internet. Anyone can hit the raw Spring Boot API directly. Once Nginx is in front, you want 8083 blocked externally — only Nginx on the same machine should talk to it.

```bash
ssh -i ~/.ssh/id_rsa root@116.203.197.65

ufw allow OpenSSH        # keep SSH open — do this FIRST or you lock yourself out
ufw allow 80             # HTTP — needed for Let's Encrypt certificate issuance
ufw allow 443            # HTTPS — production traffic
ufw --force enable
ufw status
```

Expected output:
```
Status: active
To                         Action      From
--                         ------      ----
OpenSSH                    ALLOW       Anywhere
80/tcp                     ALLOW       Anywhere
443/tcp                    ALLOW       Anywhere
```

Port 8083 is intentionally absent. Spring Boot is only reachable via Nginx on 443.

> **Why `ufw allow OpenSSH` first?** UFW blocks everything not explicitly allowed. If you enable it before allowing SSH, your current session survives but you can never SSH in again. Always allow SSH before enabling UFW.

### 2 — Docker log rotation

Docker writes container logs to `/var/lib/docker/containers/<id>/<id>-json.log` with no size limit by default. A busy Spring Boot app logging at INFO level generates hundreds of MB per day. On an 80 GB disk this silently fills the disk — Docker stops writing logs, then the container crashes.

Fix it globally for all containers:

```bash
cat > /etc/docker/daemon.json << 'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "50m",
    "max-file": "3"
  }
}
EOF

systemctl restart docker
```

This caps each container at 3 files × 50 MB = 150 MB maximum. Old logs rotate out automatically. Applies to all future containers — existing ones need to be recreated to pick it up (which the redeploy step does anyway).

### 3 — Protect the .env.prod file

The `.env.prod` file contains JWT secrets, database passwords, Redis passwords, and API keys. By default `scp` creates files with `644` permissions — readable by any user on the system.

```bash
chmod 600 /opt/iced-latte/.env.prod
ls -la /opt/iced-latte/
# expected: -rw------- 1 root root ... .env.prod
```

---

## Server reboot survival

`--restart unless-stopped` tells Docker to restart the container if it crashes or if the Docker daemon restarts. The Docker daemon itself starts on boot via `systemctl enable docker` (done during installation). So the full chain on reboot is:

```
Server boots
  → systemd starts Docker daemon
    → Docker daemon restarts all containers with restart policy
      → iced-latte-backend starts automatically
```

**The network gotcha:** The `iced-latte-network` Docker network was created implicitly the first time we ran the container. After a reboot Docker restores it alongside the container from its internal state — so in practice it survives. But if you ever manually delete the network or start fresh on a new server, `docker run` will fail with:

```
docker: Error response from daemon: network iced-latte-network not found.
```

The fix is to always create the network explicitly before running the container:

```bash
docker network create iced-latte-network 2>/dev/null || true
docker run -d \
  --name iced-latte-backend \
  --network iced-latte-network \
  --env-file /opt/iced-latte/.env.prod \
  -p 8083:8083 \
  --restart unless-stopped \
  zufarexplainedit/iced-latte-backend:latest
```

The `2>/dev/null || true` makes it idempotent — if the network already exists, the error is suppressed and the command continues.

**To verify survival after reboot:**

```bash
reboot
# wait ~40 seconds, then:
ssh -i ~/.ssh/id_rsa root@116.203.197.65 "docker ps && curl -s http://localhost:8083/actuator/health"
```

---

## Nginx + TLS — full setup

This is what makes the Vercel frontend work. Browsers block `https://` → `http://` requests (mixed content policy). You need HTTPS on the backend.

### Install

```bash
apt install -y nginx certbot python3-certbot-nginx
```

### Create the Nginx site config

```bash
cat > /etc/nginx/sites-available/iced-latte << 'EOF'
server {
    listen 80;
    server_name your-domain.com;
}
EOF

ln -s /etc/nginx/sites-available/iced-latte /etc/nginx/sites-enabled/iced-latte
nginx -t && systemctl reload nginx
```

### Get the TLS certificate

```bash
certbot --nginx -d your-domain.com
```

Certbot modifies the config automatically — it adds the HTTPS block and the HTTP→HTTPS redirect. After it runs, `/etc/nginx/sites-available/iced-latte` looks like this:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    include             /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam         /etc/letsencrypt/ssl-dhparams.pem;

    location / {
        proxy_pass         http://localhost:8083;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

Why each proxy header matters:
- `X-Real-IP` / `X-Forwarded-For` — Spring Boot's rate limiter and audit logs see the real client IP, not `127.0.0.1`
- `X-Forwarded-Proto` — Spring Boot knows the original request was HTTPS, important for redirect generation
- `proxy_read_timeout 60s` — Liquibase migrations on first startup can take several seconds; without this Nginx times out on the first request after a restart

### After TLS is working

Update Vercel:
```
NEXT_PUBLIC_API_URL = https://your-domain.com/api/v1
```

Block direct access to port 8083 — Nginx handles all external traffic now:
```bash
ufw deny 8083
ufw status
```

Certbot auto-renews certificates via a systemd timer — no manual renewal needed.

---

## Troubleshooting

Work through these in order when something breaks.

### Is the container running?

```bash
docker ps
```

If `iced-latte-backend` is missing:
```bash
docker ps -a                              # shows stopped containers too
docker logs iced-latte-backend --tail 50  # why did it stop?
```

Common reasons a container is stopped and not restarting:
- Crashed too many times too fast — Docker backs off restarts exponentially
- Image was deleted: `docker images | grep iced-latte`
- Network was deleted: `docker network ls | grep iced-latte`

### Container is running but API returns nothing

```bash
ss -tlnp | grep 8083                           # is port 8083 actually bound?
curl -v http://localhost:8083/actuator/health   # can the server reach itself?
docker logs iced-latte-backend --tail 30        # what do logs say right now?
```

### Spring Boot failed to start — log patterns

| Log pattern | Meaning | Fix |
|---|---|---|
| `Connection refused` on Supabase host | DB unreachable | Check `DATASOURCE_HOST` in `.env.prod`, check Supabase status page |
| `Unable to connect to Redis` | Upstash unreachable | Check `REDIS_HOST`, `REDIS_PASSWORD`, confirm `REDIS_SSL_ENABLED=true` |
| `Could not resolve placeholder` | Required env var missing from `.env.prod` | Check the variable name in the error, add it to `.env.prod` |
| `OutOfMemoryError: Metaspace` | JVM Metaspace cap too low | Remove `-XX:MaxMetaspaceSize` from Dockerfile ENTRYPOINT — see Issue #6 |
| `OutOfMemoryError: Java heap space` | Heap exhausted | Check `MaxRAMPercentage` and actual RSS with `docker stats` |
| `Port 8083 already in use` | Another process using the port | `ss -tlnp | grep 8083` to find it |

### Can the server reach external services?

```bash
nc -zv aws-1-eu-west-2.pooler.supabase.com 6543   # Supabase DB
nc -zv literate-ringtail-36568.upstash.io 6379     # Upstash Redis
nslookup aws-1-eu-west-2.pooler.supabase.com       # DNS resolution
```

### Disk full

```bash
df -h /
du -sh /var/lib/docker/containers/*/   # find which container's logs are large
```

If Docker logs filled the disk despite the rotation config, the container was created before the daemon config was applied. Recreate it with the standard `docker stop && docker rm && docker run` sequence.

### Check memory at any time

```bash
docker stats iced-latte-backend --no-stream
docker exec iced-latte-backend sh -c 'cat /proc/1/status | grep -E "VmRSS|VmHWM"'
```

Healthy baseline: RSS ~568 MB under load, ~530 MB at idle, Swap 0. If RSS climbs past 1 GB and does not come back down, there is a memory leak — check for request-scoped objects held in static fields.
