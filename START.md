# Getting Started with Iced Latte

This guide walks you through running the Iced Latte backend on your local machine.
There are two ways to run it — pick the one that fits you:

- **Option A — IntelliJ + Docker (recommended for development)**: Run the app from IntelliJ IDE, with only PostgreSQL and Redis in Docker. Best for writing and debugging code.
- **Option B — Full Docker**: Run everything (app + database + Redis) in Docker containers. Best for a quick smoke test without an IDE.

---

## Prerequisites

Make sure you have all of the following installed before you start:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 21 | https://adoptium.net/ |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |
| Docker Desktop | latest | https://www.docker.com/products/docker-desktop/ |
| IntelliJ IDEA | any edition | https://www.jetbrains.com/idea/download/ (for Option A) |

To verify your installations, run these commands in a terminal:
```bash
java -version       # should print: openjdk 21...
mvn -version        # should print: Apache Maven 3.9...
docker --version    # should print: Docker version...
```

---

## Option A — Run in IntelliJ (recommended for development)

This is the recommended approach for contributors. The app runs directly in IntelliJ so you can set breakpoints, inspect variables, and iterate quickly.

### Step 1 — Clone the repository

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

### Step 2 — Start PostgreSQL and Redis with Docker

The app needs a database and a cache. Start them with a single command:

```bash
docker-compose -f docker-compose.local.yml up -d iced-latte-postgresdb iced-latte-redis
```

To verify they are running:
```bash
docker ps
```
You should see two containers: `iced-latte-postgresdb` and `iced-latte-redis`.

### Step 3 — Create your local environment file

The app reads its configuration from environment variables. Create a file named `.env` in the project root:

```bash
# Copy the example below into a new file called .env in the project root
```

Create the file `.env` with this content:

```properties
# Server
APP_SERVER_PORT=8083

# Database (matches the Docker container started in Step 2)
DATASOURCE_HOST=localhost
DATASOURCE_PORT=5432
DATASOURCE_NAME=testdb
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=postgres

# JWT secrets (these values are safe to use locally)
APP_JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
APP_JWT_REFRESH_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# AWS (disabled locally — no real AWS account needed)
AWS_ENABLED=false
AWS_ACCESS_KEY=local
AWS_SECRET_KEY=local
AWS_REGION=us-east-1
AWS_PRODUCT_BUCKET=products
AWS_USER_BUCKET=users
AWS_DEFAULT_PRODUCT_IMAGES_PATH=./products

# Redis (matches the Docker container started in Step 2)
REDIS_HOST=localhost
REDIS_PORT=6379

# Google OAuth (not needed locally, use placeholders)
GOOGLE_AUTH_CLIENT_ID=local
GOOGLE_AUTH_CLIENT_SECRET=local
GOOGLE_AUTH_REDIRECT_URI=http://localhost:8083

# Stripe (not needed locally, use placeholders)
STRIPE_SECRET_KEY=sk_test_placeholder
STRIPE_WEBHOOK_SECRET=whsec_placeholder

# Spring AI (not needed locally, use placeholder)
SPRING_AI_OPENAI_API_KEY=sk-placeholder
```

> ⚠️ **Important**: `.env` is listed in `.gitignore` and will never be committed. Never put real secrets in this file.

### Step 4 — Load the .env file into IntelliJ

IntelliJ needs to know about your `.env` file so it passes the variables to the app when you run it.

1. Open IntelliJ and open the `Iced-Latte` project folder
2. In the top-right corner, click the run configuration dropdown → **Edit Configurations...**
3. Find `IcedLatteApplication` in the list (or click `+` → `Spring Boot` to create it)
4. In the **Environment variables** field, click the folder icon on the right
5. Click the `+` button → select **Load from .env file** → choose the `.env` file you created
6. Click **OK** and **Apply**

Alternatively, install the **EnvFile** plugin (Settings → Plugins → search "EnvFile") which makes this easier.

### Step 5 — Run the application

Click the green **Run** button (▶) next to `IcedLatteApplication`, or press **Shift+F10**.

Watch the console. When you see this line, the app is ready:

```
Tomcat started on port 8083
```

### Step 6 — Verify it works

Open your browser or run this command:

```bash
curl http://localhost:8083/api/v1/products?page=0&size=3
```

You should get a JSON response with a list of coffee products.

You can also open the interactive API docs (Swagger UI):
```
http://localhost:8083/api/docs/swagger-ui/index.html
```

### Step 7 — Log in with a test user

The database is pre-seeded with 15 test users. Use any of them to log in:

```bash
curl -X POST http://localhost:8083/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email": "olivia@example.com", "password": "p@ss1logic11"}'
```

You will receive a JWT token in the response. Use it as a Bearer token for authenticated endpoints.

> All 15 seed users share the same password: `p@ss1logic11`

---

## Option B — Run everything in Docker

Use this if you just want to run the app without setting up IntelliJ.

### Step 1 — Clone the repository

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte
```

### Step 2 — Start all services

```bash
docker-compose -f docker-compose.local.yml up -d --build
```

This builds the app image and starts three containers:
- `iced-latte-backend` — the Spring Boot app on port `8083`
- `iced-latte-postgresdb` — PostgreSQL on port `5432`
- `iced-latte-redis` — Redis on port `6379`

The first build takes a few minutes (Maven downloads dependencies). Subsequent builds are faster.

### Step 3 — Verify it works

```bash
curl http://localhost:8083/api/v1/products?page=0&size=3
```

### Useful Docker commands

```bash
# View live logs from the backend
docker-compose -f docker-compose.local.yml logs -f iced-latte-backend

# Stop all containers (keeps data)
docker-compose -f docker-compose.local.yml down

# Stop all containers and delete all data (fresh start)
docker-compose -f docker-compose.local.yml down -v

# Rebuild after code changes
docker-compose -f docker-compose.local.yml up -d --build
```

---

## Running the tests

```bash
mvn test
```

Expected result: **154 tests pass, 0 failures, 0 errors, 0 skipped**.

> The tests use Testcontainers — they spin up their own temporary PostgreSQL and Redis containers automatically. You do not need to start Docker containers manually for tests.

---

## Connecting to the database

You can inspect the database directly using IntelliJ's built-in database tool or the free **Database Navigator** plugin.

**Connection details:**

| Field | Value |
|-------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `testdb` |
| Username | `postgres` |
| Password | `postgres` |

**Using IntelliJ Ultimate**: View → Tool Windows → Database → `+` → PostgreSQL

**Using Database Navigator plugin** (free, works in Community edition):
1. Install it: Settings → Plugins → search "Database Navigator"
2. View → Tool Windows → DB Browser
3. Add a new PostgreSQL connection with the values above

> ⚠️ The app runs Liquibase with `drop-first: true` on every startup, which wipes and re-seeds the database. Any manual changes you make to the DB will be lost on the next app restart.

---

## Remote debugging (Docker)

If you want to debug the app running inside Docker:

1. In IntelliJ: Run → Edit Configurations → `+` → **Remote JVM Debug**
2. Set host `localhost`, port `5005`, mode **Attach to Remote JVM**
3. Start the containers: `docker-compose -f docker-compose.local.yml up -d --build`
4. Click the **Debug** button in IntelliJ
5. Set a breakpoint anywhere in the code

---

## Troubleshooting

**App fails to start with "Connection refused" on port 5432**
→ PostgreSQL container is not running. Run: `docker-compose -f docker-compose.local.yml up -d iced-latte-postgresdb`

**App fails to start with "Could not resolve placeholder"**
→ A required environment variable is missing from your `.env` file. Check the console for which variable is missing and add it.

**Login returns 401 Unauthorized**
→ Make sure you are using the correct password `p@ss1logic11` and the email exists in the seed data.

**Port 8083 already in use**
→ Another process is using the port. Either stop it or change `APP_SERVER_PORT` in your `.env` file and update the run configuration.

**Tests fail with "Connection refused" to database**
→ Tests use Testcontainers and manage their own containers. Make sure Docker Desktop is running.

---

## Project structure overview

```
src/
├── main/java/com/zufar/icedlatte/
│   ├── security/       # JWT auth, registration, login
│   ├── product/        # Product catalog
│   ├── cart/           # Shopping cart
│   ├── order/          # Orders
│   ├── review/         # Product reviews
│   ├── favorite/       # Favorites list
│   ├── payment/        # Stripe payment integration
│   ├── email/          # Email verification
│   └── user/           # User profile management
└── test/               # Unit and integration tests
```

API documentation is available at:
- **Local**: http://localhost:8083/api/docs/swagger-ui/index.html
- **Production**: https://iced-latte.uk/backend/api/docs/swagger-ui/index.html
