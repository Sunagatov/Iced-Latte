# 🚀 Getting Started with Iced Latte

This guide is for people who want to run Iced Latte locally without guessing what to install, which repository to open, or which command to copy.

Iced Latte has three moving parts:

| Part | What it is | Default URL |
|---|---|---|
| 🔧 Backend | This repository. Spring Boot API, database access, auth, products, cart, orders, payments | http://localhost:8083 |
| 🎨 Frontend | Separate Next.js repository: [`Iced-Latte-Frontend`](https://github.com/Sunagatov/Iced-Latte-Frontend) | http://localhost:3000 |
| 🧱 Infrastructure | PostgreSQL, Redis, MinIO, started by Docker Compose | local Docker containers |

---

## 🧭 Pick Your Setup

If you are unsure, choose **Option 3**.

| Option | What you run | Best for |
|---|---|---|
| **1** | Everything in Docker | Fast smoke test, no IDE setup |
| **2** | Backend + frontend locally, infrastructure in Docker | Full-stack development |
| **3** | Frontend locally, backend + infrastructure in Docker | Frontend development, easiest path for most contributors |
| **4** | Backend locally, frontend + infrastructure in Docker | Backend debugging in IntelliJ |

---

## ✅ Install First

| Tool | Required for | Version |
|---|---|---|
| Docker Desktop | Every option | latest |
| Java JDK | Backend locally: Options 2 and 4 | 25 |
| Maven | Backend locally: Options 2 and 4 | 3.9+ |
| Node.js | Frontend locally: Options 2 and 3 | 20+ |
| IntelliJ IDEA | Optional, recommended for backend debugging | any edition |

Check your machine:

```bash
docker --version
java -version
mvn -version
node -v
```

> 🪟 Windows note: backend terminal commands that use `set -a && source .env.example` are for macOS, Linux, and Git Bash. On Windows PowerShell / CMD, use IntelliJ and load `.env.example` into the run configuration.

---

## 📥 Clone Repositories

For backend-only work, clone this repository:

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

For frontend or full-stack work, clone both repositories as siblings:

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
git clone https://github.com/Sunagatov/Iced-Latte-Frontend.git
```

You should end up with:

```text
IdeaProjects/
├── Iced-Latte/
└── Iced-Latte-Frontend/
```

---

## 🔐 Local Environment

This backend guide uses `.env.example` directly.

That file is intentionally safe for local contributors:

- `SPRING_PROFILES_ACTIVE=dev`
- Swagger UI is enabled
- Stripe, Google OAuth, email, and AI integrations stay disabled unless you opt in
- local HTTP access logs run at `DEBUG`
- Liquibase re-seeds local data in the default dev profile

---

## 🟢 Option 1 — Everything in Docker

Use this when you want to see the whole app running with one command.

```bash
cd Iced-Latte
docker compose --env-file .env.example --profile backend --profile frontend up -d --build
```

This starts:

- PostgreSQL
- Redis
- MinIO
- Backend
- Frontend

First build can take several minutes because Docker builds both the Java backend and frontend image.

Verify:

| Service | URL |
|---|---|
| 🌐 Frontend | http://localhost:3000 |
| 🔌 Backend | http://localhost:8083 |
| 📚 Swagger UI | http://localhost:8083/api/docs/swagger-ui/index.html |
| 🪣 MinIO console | http://localhost:9001 |

Login with seed data:

```text
olivia@example.com / p@ss1logic11
```

---

## 🧑‍💻 Option 2 — Backend + Frontend Locally, Infrastructure in Docker

Use this when you are changing both backend and frontend code.

### Step 1 — Start Infrastructure

```bash
cd Iced-Latte
docker compose --env-file .env.example up -d postgres redis minio minio-init
```

Check containers:

```bash
docker ps
```

You should see PostgreSQL, Redis, and MinIO with status `Up`.

### Step 2 — Start Backend Locally

Recommended for beginners: use IntelliJ.

1. Open `Iced-Latte` in IntelliJ
2. Open Run / Debug Configurations
3. Select or create `IcedLatteApplication`
4. Load environment variables from `.env.example`
5. Click the green run button

Terminal alternative for macOS / Linux / Git Bash:

```bash
set -a && source .env.example && set +a && mvn spring-boot:run
```

Backend is ready when you see:

```text
Tomcat started on port 8083
```

### Step 3 — Start Frontend Locally

```bash
cd ../Iced-Latte-Frontend
cp .env.example .env.local
npm ci
npm run dev
```

Frontend is ready when you see:

```text
Local: http://localhost:3000
```

---

## 🎨 Option 3 — Frontend Locally, Backend + Infrastructure in Docker

Use this if you mostly want to work on frontend code. This is the easiest mode for many contributors because the backend runs in Docker.

### Step 1 — Start Backend + Infrastructure

```bash
cd Iced-Latte
docker compose --env-file .env.example --profile backend up -d --build
```

This starts PostgreSQL, Redis, MinIO, and the backend.

### Step 2 — Start Frontend Locally

```bash
cd ../Iced-Latte-Frontend
cp .env.example .env.local
npm ci
npm run dev
```

The default frontend `.env.local` points to:

```text
http://localhost:8083/api/v1
```

---

## 🔧 Option 4 — Backend Locally, Frontend + Infrastructure in Docker

Use this when you want to debug backend code in IntelliJ but still run the frontend in a container.

### Step 1 — Start Infrastructure

```bash
cd Iced-Latte
docker compose --env-file .env.example up -d postgres redis minio minio-init
```

### Step 2 — Start Backend Locally

Use IntelliJ with `.env.example`, or on macOS / Linux / Git Bash:

```bash
set -a && source .env.example && set +a && mvn spring-boot:run
```

### Step 3 — Start Frontend Container

```bash
FRONTEND_DOCKER_API_URL=http://host.docker.internal:8083/api/v1 \
docker compose --env-file .env.example --profile frontend up -d --build
```

The frontend container uses `host.docker.internal` to reach the backend running on your machine.

---

## ✅ Verify Everything

Use this checklist after starting any option:

| Check | Expected result |
|---|---|
| `docker ps` | Required containers show status `Up` |
| http://localhost:8083 | Backend responds |
| http://localhost:8083/api/docs/swagger-ui/index.html | Swagger UI opens |
| http://localhost:3000 | Frontend opens if you started it |
| http://localhost:9001 | MinIO console opens |

Seed login:

```text
olivia@example.com / p@ss1logic11
```

---

## 🛠️ Useful Docker Commands

```bash
# Backend logs
docker compose logs -f backend

# Frontend logs
docker compose logs -f frontend

# Stop app containers but keep database/files
docker compose --profile backend --profile frontend down

# Stop everything and delete local Docker volumes
docker compose --profile backend --profile frontend down -v
```

> ⚠️ `down -v` deletes local database and MinIO data.

---

## 🧪 Running Tests

Backend tests:

```bash
mvn test
```

Tests use Testcontainers, so Docker Desktop must be running.

Frontend checks live in the frontend repository:

```bash
cd ../Iced-Latte-Frontend
npm run lint
npm run tsc -- --noEmit
npm test
```

Frontend E2E tests require the frontend running on `http://localhost:3000`:

```bash
npm run test:e2e
```

---

## 🗄️ Database

Local PostgreSQL defaults:

| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `postgres` |
| Username | `postgres` |
| Password | `postgres` |

IntelliJ Ultimate: View → Tool Windows → Database → `+` → PostgreSQL

IntelliJ Community: install the free "Database Navigator" plugin.

> ⚠️ In the default `dev` profile, Liquibase uses local development behavior and re-seeds data on restart. Do not use the dev profile for real data.

---

## 🪣 MinIO

Open:

```text
http://localhost:9001
```

Login:

```text
minioadmin / minioadmin
```

Buckets are created automatically:

- `iced-latte-products`
- `iced-latte-users`

---

## 🔍 Observability Optional

Normal local development does not require observability tools.

The repository contains optional configuration for:

- Sentry
- Datadog-related setup
- Loki / Prometheus / Grafana
- ELK

Relevant files:

- `docker-compose.yml`
- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/resources/application-prod.yaml`
- `src/main/resources/logback-spring.xml`

---

## 🔐 Google OAuth Callback Note

The backend returns OAuth tokens in the URL fragment:

```text
http://localhost:3000/auth/google/callback#token=...&refreshToken=...
```

Frontend callback code should read:

```text
window.location.hash
```

not:

```text
window.location.search
```

---

## 🔧 Troubleshooting

| Problem | What to do |
|---|---|
| PostgreSQL connection refused on `5432` | Run `docker compose --env-file .env.example up -d postgres` |
| Redis connection refused on `6379` | Run `docker compose --env-file .env.example up -d redis` |
| MinIO does not open | Run `docker compose --env-file .env.example up -d minio minio-init` |
| Backend port `8083` already in use | Stop the process using the port, or run Docker with `BACKEND_HOST_PORT=8084` |
| Frontend port `3000` already in use | Stop the process using the port, or run Docker with `FRONTEND_HOST_PORT=3001` |
| `Could not resolve placeholder` | An env var is missing. Check the variable name in the error and compare with `.env.example` |
| Login returns `401` | Use seed login `olivia@example.com / p@ss1logic11` |
| Frontend container cannot reach local backend | Rebuild frontend with `FRONTEND_DOCKER_API_URL=http://host.docker.internal:8083/api/v1` |
| Windows says `export` or `source` not found | Use IntelliJ with `.env.example`, or run the command in Git Bash |
| Tests fail before starting | Make sure Docker Desktop is running |

Port override examples:

```bash
BACKEND_HOST_PORT=8084 docker compose --env-file .env.example --profile backend up -d --build
FRONTEND_HOST_PORT=3001 docker compose --env-file .env.example --profile frontend up -d --build
```

---

## 📁 Project Structure

```text
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
│   ├── email/          # Email verification and notifications
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
