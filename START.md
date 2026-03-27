# 🚀 Getting Started with Iced Latte

Pick the setup that fits you:

| Option | What runs where | Best for |
|--------|----------------|----------|
| **A** | Backend in IDE + Infrastructure (PostgreSQL database, Redis cache, MinIO file storage) in Docker | Active development, debugging |
| **B** | Backend + Infrastructure (PostgreSQL database, Redis cache, MinIO file storage) in Docker | Quick smoke test, no IDE needed |
| **C** | Backend + Frontend + Infrastructure (PostgreSQL database, Redis cache, MinIO file storage) in Docker | Testing the UI against the backend (requires both repos cloned side by side) |

---

## 📋 Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 25 | https://adoptium.net/ (Option A only) |
| Maven | 3.9+ | https://maven.apache.org/download.cgi (Option A only) |
| Docker Desktop | latest | https://www.docker.com/products/docker-desktop/ |
| IntelliJ IDEA | any edition | https://www.jetbrains.com/idea/download/ (Option A only) |

Verify your setup:
```bash
java -version       # openjdk 25...
mvn -version        # Apache Maven 3.9...
docker --version    # Docker version...
```

---

## Option A — Backend in IntelliJ, Infrastructure (database, cache, file storage) in Docker

> Best for: active development and debugging. You run the backend in your IDE and get hot reload, breakpoints, and full debugger support.

### Step 1 — Clone the repository

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

✅ You should see a new `Iced-Latte/` folder created locally.

### Step 2 — Start the infrastructure

```bash
docker compose up -d postgres redis minio minio-init
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

### Step 3 — Configure environment

The `.env` file in the project root is committed with local development values and placeholders — it works out of the box for local development. You don't need to change anything to get started.

> 🔒 Review `.env` before enabling real third-party integrations (email, OAuth, Stripe, AI). Never commit real secrets to git.

> 🪟 **Windows users:** use the IntelliJ EnvFile plugin (Step 4) to load `.env` — the `export` command only works on Linux/macOS/Git Bash.

✅ No output expected — just confirm `.env` exists in the project root.

### Step 4 — Load `.env` into IntelliJ

IntelliJ needs the environment variables from `.env` when it runs the app.

**Option 1 — EnvFile plugin (recommended)**
1. Settings → Plugins → search "EnvFile" → Install
2. Edit Configurations → `IcedLatteApplication` → check **Enable EnvFile** → add `.env`

**Option 2 — Manual**
1. Edit Configurations → `IcedLatteApplication` (create as Spring Boot if missing)
2. **Environment variables** field → click the folder icon → paste the contents of `.env`

✅ The run configuration should now show the `.env` file or the env vars listed.

### Step 5 — Run the backend

Click ▶ next to `IcedLatteApplication` or press **Shift+F10**.

✅ When you see `Tomcat started on port 8083` in the console, the app is ready.

### Step 6 — Verify the app is running

```bash
curl "http://localhost:8083/api/v1/products?page=0&size=3"
```

✅ You should get a JSON response with a list of coffee products.

📚 Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html

### Step 7 — Log in with a test user

```bash
curl -X POST http://localhost:8083/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email": "olivia@example.com", "password": "p@ss1logic11"}'
```

✅ You should get a JSON response containing `accessToken` and `refreshToken`.

> All 15 seed users share the password `p@ss1logic11`.

---

## Option B — Backend + Infrastructure (database, cache, file storage) in Docker, no frontend

> Best for: quick smoke test or if you don't want to set up an IDE. Everything runs in Docker — no Java or Maven installation needed.

### Step 1 — Clone the repository

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

✅ You should see a new `Iced-Latte/` folder created locally.

### Step 2 — Start everything

```bash
docker compose --profile backend up -d --build
```

This builds the backend image and starts:
- `iced-latte-postgresdb` — PostgreSQL on port `5432`
- `iced-latte-redis` — Redis on port `6379`
- `iced-latte-minio` — MinIO file storage on port `9000`
- `iced-latte-backend` — Spring Boot app on port `8083`

> ⏳ The first build takes a few minutes (Maven downloads dependencies). Subsequent builds are faster.

✅ When the build finishes, all 4 containers should be running. You can verify with `docker ps`.

### Step 3 — Verify the app is running

```bash
curl "http://localhost:8083/api/v1/products?page=0&size=3"
```

✅ You should get a JSON response with a list of coffee products.

📚 Swagger UI: http://localhost:8083/api/docs/swagger-ui/index.html

### 🛠️ Useful Docker commands

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

## Option C — Backend + Frontend + Infrastructure (database, cache, file storage) in Docker

> Best for: testing the UI against the backend. Builds the React frontend from source — no Node.js installation needed, but the first build takes several extra minutes.

### Step 1 — Clone both repositories (side by side)

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
git clone https://github.com/Sunagatov/Iced-Latte-Frontend.git
cd Iced-Latte
```

✅ Both folders must sit next to each other: `Iced-Latte/` and `Iced-Latte-Frontend/`.

### Step 2 — Start everything including the frontend

```bash
docker compose --profile backend --profile frontend up -d --build
```

This builds and starts everything from Option B plus:
- `iced-latte-frontend` — React app on port `3000`

> ⏳ The first build takes several minutes (Maven + npm). Subsequent builds are faster.

✅ When the build finishes, all 5 containers should be running. You can verify with `docker ps`.

### Step 3 — Verify everything is running

- 🌐 Frontend: http://localhost:3000 — you should see the Iced Latte shop UI
- 🔌 Backend API: http://localhost:8083 — Swagger UI at http://localhost:8083/api/docs/swagger-ui/index.html
- 🪣 MinIO console: http://localhost:9001 — login with `minioadmin` / `minioadmin`

### 🛠️ Useful Docker commands

```bash
# Live logs
docker compose logs -f frontend
docker compose logs -f backend

# Stop (keeps data)
docker compose --profile backend --profile frontend down

# Stop and wipe all data (fresh start)
docker compose --profile backend --profile frontend down -v

# Rebuild after code changes
docker compose --profile backend --profile frontend up -d --build
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
- `docker-compose.logging.yml`
- `src/main/resources/application.yaml`
- `src/main/resources/logback-spring.xml`

---

## 🧪 Running the tests

```bash
mvn test
```

Tests use Testcontainers — they spin up their own temporary containers. Docker Desktop must be running. No manual container setup needed.

---

## 🗄️ Connecting to the database

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

## 🪣 MinIO object storage console

Open http://localhost:9001 in your browser.

Login: `minioadmin` / `minioadmin` (matches the defaults in `.env`).

Two buckets are created automatically on first start: `iced-latte-products` and `iced-latte-users`.

---

## 🔧 Troubleshooting

**❌ "Connection refused" on port 5432**
→ PostgreSQL is not running. Run: `docker compose up -d postgres`

**❌ "Connection refused" on port 6379**
→ Redis is not running. Run: `docker compose up -d redis`

**❌ "Could not resolve placeholder"**
→ A required env var is missing. Check the console output for the variable name and add it to `.env`.

**❌ Login returns 401**
→ Use password `p@ss1logic11` and an email from the seed data (e.g. `olivia@example.com`).

**❌ Port 8083 already in use**
→ Stop the conflicting process or change `APP_SERVER_PORT` in `.env`.

**❌ Windows: `export` command not found**
→ The `export $(cat .env | xargs)` command only works on Linux/macOS/Git Bash. On Windows, use the IntelliJ EnvFile plugin (Step 4) or run the full Docker stack with `docker compose --profile backend up -d --build` (Option B) — no manual env loading needed.

**❌ Tests fail with "Connection refused"**
→ Docker Desktop must be running — Testcontainers needs it.

---

## 📁 Project structure

```
src/
├── main/java/com/zufar/icedlatte/
│   ├── security/       # JWT auth, Google OAuth2, registration, login
│   ├── auth/           # Google OAuth2 callback, auth redirects
│   ├── user/           # User profile management
│   ├── product/        # Product catalog
│   ├── cart/           # Shopping cart
│   ├── order/          # Orders
│   ├── payment/        # Stripe webhook & session handling
│   ├── review/         # Product reviews, ratings, AI moderation
│   ├── favorite/       # Favorites list
│   ├── email/          # Email verification & notifications
│   ├── filestorage/    # AWS S3 file upload/download
│   ├── observability/  # Telemetry, Sentry integration
│   ├── common/         # Shared utilities, validation, monitoring
│   └── astartup/       # Startup data migration
└── test/               # Unit and integration tests
```

API docs:
- 🖥️ Local: http://localhost:8083/api/docs/swagger-ui/index.html
- 🌐 Production: https://iced-latte.uk/backend/api/docs/swagger-ui/index.html

---

## 📞 Contact

- 💬 **Telegram community:** [Zufar Explained IT](https://t.me/zufarexplained)
- 👤 **Personal Telegram:** [@lucky_1uck](https://web.telegram.org/k/#@lucky_1uck)
- 📧 **Email:** [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)
- 🐛 **Issues:** [GitHub Issues](https://github.com/Sunagatov/Iced-Latte/issues)
