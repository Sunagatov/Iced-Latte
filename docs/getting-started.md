# 🚀 Getting Started with Iced Latte

Pick the setup that fits you:

| Option | What runs where | Best for |
|--------|----------------|----------|
| **1** | Frontend + Backend + Infrastructure (PostgreSQL database, Redis cache, MinIO file storage) in Docker | Full Docker run, quick smoke test |
| **2** | Frontend + Backend locally, Infrastructure (PostgreSQL database, Redis cache, MinIO file storage) in Docker | Active development on both repos |
| **3** | Frontend locally, Backend + Infrastructure (PostgreSQL database, Redis cache, MinIO file storage) in Docker | Backend isolated in Docker, frontend local |
| **4** | Backend locally, Frontend + Infrastructure (PostgreSQL database, Redis cache, MinIO file storage) in Docker | Backend debugging in IntelliJ, frontend isolated in Docker |

---

## 📋 Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 25 | https://adoptium.net/ (backend-local modes only) |
| Maven | 3.9+ | https://maven.apache.org/download.cgi (backend-local modes only) |
| Docker Desktop | latest | https://www.docker.com/products/docker-desktop/ |
| IntelliJ IDEA | any edition | https://www.jetbrains.com/idea/download/ (backend-local modes only) |

Verify your setup:
```bash
java -version       # openjdk 25...
mvn -version        # Apache Maven 3.9...
docker --version    # Docker version...
```

---

## Before you start

### Clone the backend repository

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

✅ You should see a new `Iced-Latte/` folder created locally.

### Use the shared local env file

All contributor commands in this guide use `.env.example` directly.

> ⚠️ `.env.example` is intentionally tuned for contributors: `SPRING_PROFILES_ACTIVE=dev`, Swagger UI stays on locally, Liquibase re-seeds on restart, optional integrations such as Stripe stay disabled, and HTTP access logs run at `DEBUG`.

### Start the shared infrastructure

All four modes use the same infrastructure containers:

```bash
docker compose --env-file .env.example up -d postgres redis minio minio-init
```

This starts:
- `iced-latte-postgresdb` — PostgreSQL on port `5432`
- `iced-latte-redis` — Redis on port `6379`
- `iced-latte-minio` — S3-compatible object storage on port `9000` (console at http://localhost:9001)

Verify all containers are running:
```bash
docker ps
```

✅ You should see 3 containers with status `Up`: `iced-latte-postgresdb`, `iced-latte-redis`, `iced-latte-minio`.

### What "backend locally" means

You can run the backend locally in either of these ways:

**Option 1 — IntelliJ green run button**
1. Edit Configurations → `IcedLatteApplication`
2. In **Environment variables**, load `.env.example`
3. Click ▶ next to `IcedLatteApplication`

**Option 2 — Terminal**
```bash
set -a && source .env.example && set +a && mvn spring-boot:run
```

> 🪟 **Windows users:** the shell command above is for Linux/macOS/Git Bash. On Windows, use IntelliJ with `.env.example` loaded in the run configuration.

✅ When you see `Tomcat started on port 8083`, the backend is ready.

### What "frontend locally" means

Clone the frontend repo as a sibling:

```bash
git clone https://github.com/Sunagatov/Iced-Latte-Frontend.git
```

✅ Both folders should sit next to each other: `Iced-Latte/` and `Iced-Latte-Frontend/`.

For local frontend startup, use the frontend repo's normal local-dev flow.

---

## Option 1 — Frontend + Backend + Infrastructure in Docker

> Best for: full Docker run, quick smoke test, and checking that both app tiers work together without local IDE setup.

### Step 1 — Start everything

```bash
docker compose --env-file .env.example --profile backend --profile frontend up -d --build
```

If `3000` or `8083` is already in use on your machine, override the Docker host ports:

```bash
FRONTEND_HOST_PORT=3001 BACKEND_HOST_PORT=8084 \
docker compose --env-file .env.example --profile backend --profile frontend up -d --build
```

This builds and starts:
- `iced-latte-postgresdb`
- `iced-latte-redis`
- `iced-latte-minio`
- `iced-latte-backend`
- `iced-latte-frontend`

> ⏳ The first build takes several minutes (Maven + frontend build). Subsequent builds are faster.

### Step 2 — Verify everything is running

- 🌐 Frontend: http://localhost:3000
- 🔌 Backend API: http://localhost:8083
- 📚 Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html
- 🪣 MinIO console: http://localhost:9001

---

## Option 2 — Frontend + Backend locally, Infrastructure in Docker

> Best for: active development on both repositories.

### Step 1 — Run the backend locally

Use IntelliJ + `.env.example` or:

```bash
set -a && source .env.example && set +a && mvn spring-boot:run
```

### Step 2 — Run the frontend locally

Run the frontend from the sibling `Iced-Latte-Frontend/` repo using its normal local-dev flow.

### Step 3 — Verify everything is running

- 🌐 Frontend: usually http://localhost:3000
- 🔌 Backend API: http://localhost:8083
- 📚 Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html

---

## Option 3 — Frontend locally, Backend + Infrastructure in Docker

> Best for: keeping backend runtime identical to Docker while editing the frontend locally.

### Step 1 — Start the backend in Docker

```bash
docker compose --env-file .env.example --profile backend up -d --build
```

This starts:
- `iced-latte-postgresdb`
- `iced-latte-redis`
- `iced-latte-minio`
- `iced-latte-backend`

### Step 2 — Run the frontend locally

Run the frontend from the sibling `Iced-Latte-Frontend/` repo and point it to:

```text
http://localhost:8083/api/v1
```

### Step 3 — Verify everything is running

- 🌐 Frontend: usually http://localhost:3000
- 🔌 Backend API: http://localhost:8083
- 📚 Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html

---

## Option 4 — Backend locally, Frontend + Infrastructure in Docker

> Best for: debugging the backend in IntelliJ while still running the frontend in a container.

### Step 1 — Run the backend locally

Use IntelliJ + `.env.example` or:

```bash
set -a && source .env.example && set +a && mvn spring-boot:run
```

### Step 2 — Build the frontend container against the local backend

```bash
FRONTEND_DOCKER_API_URL=http://host.docker.internal:8083/api/v1 \
docker compose --env-file .env.example --profile frontend up -d --build
```

This starts:
- `iced-latte-postgresdb`
- `iced-latte-redis`
- `iced-latte-minio`
- `iced-latte-frontend`

The frontend container uses `host.docker.internal` to reach the backend running on your machine.

### Step 3 — Verify everything is running

- 🌐 Frontend: http://localhost:3000
- 🔌 Backend API: http://localhost:8083
- 📚 Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html

---

## Google OAuth callback note

The backend returns OAuth tokens in the URL fragment, not the query string.

If the frontend callback page reads tokens after Google sign-in, parse:

```text
window.location.hash
```

instead of:

```text
window.location.search
```

Example callback URL:

```text
http://localhost:3000/auth/google/callback#token=...&refreshToken=...
```

---

## 🛠️ Useful Docker commands

```bash
# Backend logs
docker compose logs -f backend

# Frontend logs
docker compose logs -f frontend

# Stop everything but keep data
docker compose --profile backend --profile frontend down

# Stop everything and wipe all data
docker compose --profile backend --profile frontend down -v
```

---

## 🔍 Observability (Optional)

Observability is optional and disabled by default — normal local development does not require it.

The repo contains optional observability and logging playground pieces:

- Sentry
- Datadog-related setup
- Loki / Prometheus / Grafana
- ELK (Elasticsearch, Logstash, Kibana)

To explore them, check the existing config files:

- `docker-compose.yml`
- `src/main/resources/application.yaml`
- `src/main/resources/application-prod.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/resources/logback-spring.xml`

---

## 🧪 Running the tests

```bash
mvn test
```

Tests use Testcontainers — they spin up their own temporary containers. Docker Desktop must be running. No manual container setup needed.

---

## 🗄️ Connecting to the database

- Host: `localhost`
- Port: `5432`
- Database: `postgres`
- Username: `postgres`
- Password: `postgres`

**IntelliJ Ultimate**: View → Tool Windows → Database → `+` → PostgreSQL

**Community edition**: Install the free "Database Navigator" plugin (Settings → Plugins).

> ⚠️ Liquibase behavior depends on the active profile. The contributor default in `.env.example` is `SPRING_PROFILES_ACTIVE=dev`, so startup uses `drop-first: true` and re-seeds the database on every restart. If you intentionally switch away from `dev`, that behavior changes.

---

## 🪣 MinIO object storage console

Open http://localhost:9001 in your browser.

Login: `minioadmin` / `minioadmin` (matches the defaults in `.env.example`).

Two buckets are created automatically on first start: `iced-latte-products` and `iced-latte-users`.

---

## 🔧 Troubleshooting

**❌ "Connection refused" on port 5432**
→ PostgreSQL is not running. Run: `docker compose --env-file .env.example up -d postgres`

**❌ "Connection refused" on port 6379**
→ Redis is not running. Run: `docker compose --env-file .env.example up -d redis`

**❌ "Could not resolve placeholder"**
→ A required env var is missing. Check the console output for the variable name and add it to `.env.example` or pass it explicitly in your shell.

**❌ Login returns 401**
→ Use password `p@ss1logic11` and an email from the seed data (e.g. `olivia@example.com`).

**❌ Port 8083 already in use**
→ Stop the conflicting process or override the Docker host port with `BACKEND_HOST_PORT=8084 docker compose --env-file .env.example --profile backend up -d --build`.

**❌ Port 3000 already in use**
→ Stop the conflicting process or override the Docker host port with `FRONTEND_HOST_PORT=3001 docker compose --env-file .env.example --profile frontend up -d --build`.

**❌ Frontend container cannot reach local backend**
→ Rebuild with `FRONTEND_DOCKER_API_URL=http://host.docker.internal:8083/api/v1 docker compose --env-file .env.example --profile frontend up -d --build`.

**❌ Windows: `export` command not found**
→ The shell command in this guide only works on Linux/macOS/Git Bash. On Windows, use IntelliJ with `.env.example` loaded in the run configuration.

**❌ Tests fail with "Connection refused"**
→ Docker Desktop must be running — Testcontainers needs it.

---

## 📁 Project structure

```
src/
├── main/java/com/zufar/icedlatte/
│   ├── security/       # JWT auth, Google OAuth2, registration, login, sessions, rate limiting
│   ├── auth/           # Google OAuth2 callback, auth redirects
│   ├── user/           # User profile, addresses, avatars
│   ├── product/        # Product catalog, filters, images
│   ├── cart/           # Shopping cart
│   ├── order/          # Orders, order lifecycle, order history
│   ├── payment/        # Stripe payment, checkout, webhooks
│   ├── review/         # Product reviews, ratings, AI moderation
│   ├── favorite/       # Favorites list
│   ├── email/          # Email verification & notifications
│   ├── filestorage/    # AWS S3 / MinIO file upload/download
│   ├── common/         # Shared utilities, validation, monitoring, HTTP helpers
│   └── astartup/       # Startup data migration and bootstrap tasks
└── test/               # Unit and integration tests
```

API docs:
- 🖥️ Local Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html
- 📄 Repo-local OpenAPI specs: `src/main/resources/api-specs/`

---

## 📞 Contact

- 💬 **Telegram community:** [Project community](https://t.me/zufarexplained)
- 👤 **Personal Telegram:** [@lucky_1uck](https://web.telegram.org/k/#@lucky_1uck)
- 📧 **Email:** [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)
- 🐛 **Issues:** [GitHub Issues](https://github.com/Sunagatov/Iced-Latte/issues)
