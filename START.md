# Getting Started with Iced Latte

Pick the setup that fits you:

| Option | What runs where | Best for |
|--------|----------------|----------|
| **A — IDE + Docker infra** | Backend in IntelliJ, everything else in Docker | Active development, debugging |
| **B — Full Docker** | Everything in Docker | Quick smoke test, no IDE needed |

---

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 25 | https://adoptium.net/ |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |
| Docker Desktop | latest | https://www.docker.com/products/docker-desktop/ |
| IntelliJ IDEA | any edition | https://www.jetbrains.com/idea/download/ (Option A only) |

```bash
java -version       # openjdk 25...
mvn -version        # Apache Maven 3.9...
docker --version    # Docker version...
```

---

## Option A — Backend in IntelliJ, infrastructure in Docker

### Step 1 — Clone

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

### Step 2 — Start infrastructure (PostgreSQL, Redis, MinIO)

```bash
docker compose up -d postgres redis minio minio-init
```

This starts:
- `iced-latte-postgresdb` — PostgreSQL on port `5432`
- `iced-latte-redis` — Redis on port `6379`
- `iced-latte-minio` — S3-compatible object storage on port `9000` (console at http://localhost:9001)

Verify:
```bash
docker ps
```

### Step 3 — Configure environment

The `.env` file in the project root is committed with safe defaults — it works out of the box for local development (MinIO for S3, local postgres/redis, placeholder secrets).

If you need real credentials (Supabase S3, real Google OAuth, Datadog, etc.), copy `.env.prod` and fill it in:

```bash
cp .env.prod .env  # then edit .env with your real values
```

> `.env.prod` is gitignored. Never commit real secrets.

### Step 4 — Load `.env` into IntelliJ

IntelliJ needs the environment variables from `.env` when it runs the app.

**Option 1 — EnvFile plugin (recommended)**
1. Settings → Plugins → search "EnvFile" → Install
2. Edit Configurations → `IcedLatteApplication` → check **Enable EnvFile** → add `.env`

**Option 2 — Manual**
1. Edit Configurations → `IcedLatteApplication` (create as Spring Boot if missing)
2. **Environment variables** field → click the folder icon → paste the contents of `.env`

### Step 5 — Run

Click ▶ next to `IcedLatteApplication` or press **Shift+F10**.

When you see `Tomcat started on port 8083` the app is ready.

### Step 6 — Verify

```bash
curl "http://localhost:8083/api/v1/products?page=0&size=3"
```

Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html

### Step 7 — Log in with a test user

```bash
curl -X POST http://localhost:8083/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email": "olivia@example.com", "password": "p@ss1logic11"}'
```

All 15 seed users share the password `p@ss1logic11`.

---

## Option B — Full Docker (everything in containers)

### Step 1 — Clone

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

### Step 2 — Start everything

```bash
docker compose --profile backend up -d --build
```

This builds the backend image and starts:
- `iced-latte-postgresdb` — PostgreSQL on port `5432`
- `iced-latte-redis` — Redis on port `6379`
- `iced-latte-minio` — MinIO S3 on port `9000`
- `iced-latte-backend` — Spring Boot app on port `8083`

The first build takes a few minutes (Maven downloads dependencies). Subsequent builds are faster.

### Step 3 — Verify

```bash
curl "http://localhost:8083/api/v1/products?page=0&size=3"
```

### Useful Docker commands

```bash
# Live logs
docker compose logs -f backend

# Stop (keeps data)
docker compose --profile backend down

# Stop and wipe all data (fresh start)
docker compose --profile backend down -v

# Rebuild after code changes
docker compose --profile backend up -d --build
```

---

## Option C — Full stack including frontend

```bash
docker compose --profile backend --profile frontend up -d --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8083
- MinIO console: http://localhost:9001

> ⚠️ Grafana also uses port `3000` if you run it separately. Don't start both at the same time.

---

## Running the tests

```bash
mvn test
```

Tests use Testcontainers — they spin up their own temporary containers. Docker Desktop must be running. No manual container setup needed.

---

## Connecting to the database

| Field | Value |
|-------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `postgres` |
| Username | `postgres` |
| Password | `postgres` |

**IntelliJ Ultimate**: View → Tool Windows → Database → `+` → PostgreSQL

**Community edition**: Install the free "Database Navigator" plugin (Settings → Plugins).

> ⚠️ Liquibase runs with `drop-first: true` on every startup — the database is wiped and re-seeded each time the app restarts. Any manual DB changes will be lost.

---

## MinIO object storage console

Open http://localhost:9001 in your browser.

Login: `minioadmin` / `minioadmin` (matches the defaults in `.env`).

Two buckets are created automatically on first start: `iced-latte-products` and `iced-latte-users`.

---

## Remote debugging (Docker)

1. IntelliJ: Run → Edit Configurations → `+` → **Remote JVM Debug**
2. Host `localhost`, port `5005`, mode **Attach to Remote JVM**
3. Start containers: `docker compose --profile backend up -d --build`
4. Click **Debug** in IntelliJ and set a breakpoint

---

## Troubleshooting

**"Connection refused" on port 5432**
→ PostgreSQL is not running. Run: `docker compose up -d postgres`

**"Connection refused" on port 6379**
→ Redis is not running. Run: `docker compose up -d redis`

**"Could not resolve placeholder"**
→ A required env var is missing. Check the console output for the variable name and add it to `.env`.

**Login returns 401**
→ Use password `p@ss1logic11` and an email from the seed data (e.g. `olivia@example.com`).

**Port 8083 already in use**
→ Stop the conflicting process or change `APP_SERVER_PORT` in `.env`.

**Tests fail with "Connection refused"**
→ Docker Desktop must be running — Testcontainers needs it.

---

## Project structure

```
src/
├── main/java/com/zufar/icedlatte/
│   ├── security/       # JWT auth, registration, login
│   ├── product/        # Product catalog
│   ├── cart/           # Shopping cart
│   ├── order/          # Orders
│   ├── review/         # Product reviews
│   ├── favorite/       # Favorites list
│   ├── email/          # Email verification
│   └── user/           # User profile management
└── test/               # Unit and integration tests
```

API docs:
- Local: http://localhost:8083/api/docs/swagger-ui/index.html
- Production: https://iced-latte.uk/backend/api/docs/swagger-ui/index.html
