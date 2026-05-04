# Infrastructure & Integrations

> **Production deployment** — production environment files, server configuration, and operations are managed outside this repository. This repository owns source code and local development workflows only. Do not add production deployment logic here.

This document covers the infrastructure and optional integrations that power Iced Latte. Each section explains what the component does, how it is configured, and which free-tier providers work out of the box — so you can run the full stack without spending anything.

## Table of Contents

- [PostgreSQL](#postgresql)
- [Object Storage (MinIO / AWS S3 / Supabase)](#object-storage-minio--aws-s3--supabase)
- [Redis Cache](#redis-cache)

---

## PostgreSQL

The app uses PostgreSQL as its primary database, managed via Liquibase migrations.

### Schema

All tables are managed through Liquibase migrations, but startup behavior depends on the active Spring profile:

- `dev` uses `drop-first: true`, so the schema is recreated and re-seeded on every startup.
- `prod` uses `drop-first: false`, so the schema is preserved across restarts.

| Table | Purpose |
|---|---|
| `user_details` | User accounts and profiles |
| `user_granted_authority` | Roles (ROLE_USER, ROLE_ADMIN) |
| `address` | User addresses |
| `delivery_address` | Per-order delivery addresses |
| `product` | Product catalog |
| `shopping_cart` / `shopping_cart_item` | Cart state per user |
| `order` / `order_item` | Placed orders |
| `product_review` / `product_review_likes` | Reviews and likes |
| `favorite_product` / `favorite_product_item` | Favorites list |
| `payment` | Stripe payment records |
| `file_metadata` | S3 file references |
| `login_attempts` | Brute-force tracking |
| `audit_log` | Audit trail (created/updated timestamps) |

### Connection

Configured via environment variables (see `.env`):

```
DATASOURCE_HOST=localhost
DATASOURCE_PORT=5432
DATASOURCE_NAME=postgres
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=postgres
```

Connection pool: HikariCP, minimum idle `1`, maximum pool size `5`. SSL is controlled through the JDBC URL's `sslmode` parameter.

### Seed data

The seed dataset is loaded through Liquibase. On a fresh database you get 15 users, all sharing the password `p@ss1logic11`. Test login: `olivia@example.com`.

With the `dev` profile, that data is recreated on every startup because the schema is dropped first. With the `prod` profile, it is loaded only when the database is empty or the relevant changesets have not run yet.

---

## Object Storage (MinIO / AWS S3 / Supabase)

The app uses AWS S3 SDK v2 for file storage. The local Docker stack uses MinIO, and any S3-compatible provider works for hosted environments — including AWS S3 and [Supabase Storage](https://supabase.com/docs/guides/storage).

### What is stored

| Bucket | Contents |
|---|---|
| `AWS_PRODUCT_BUCKET` | Product images (path: `./products`) |
| `AWS_USER_BUCKET` | User avatar images |

Presigned URLs are generated per request with a 1-hour expiry and cached in Redis (or Caffeine) for 50 minutes.

### Local Docker defaults (MinIO)

When you start `minio` and `minio-init` through `docker compose`, the app uses:

```
AWS_ENABLED=true
AWS_ENDPOINT_URL=http://localhost:9000
AWS_PUBLIC_URL_BASE=http://localhost:9000/iced-latte-products
AWS_ACCESS_KEY=minioadmin
AWS_SECRET_KEY=minioadmin
AWS_REGION=us-east-1
AWS_PRODUCT_BUCKET=iced-latte-products
AWS_USER_BUCKET=iced-latte-users
AWS_DEFAULT_PRODUCT_IMAGES_PATH=./seed/products
```

MinIO console: http://localhost:9001

### How to configure (Supabase)

1. Create a project at [supabase.com](https://supabase.com)
2. Go to **Storage → Buckets** and create two buckets: one for products, one for user avatars
3. Go to **Project Settings → API** and copy the S3 credentials

Set these environment variables:

```
AWS_ACCESS_KEY=<supabase-s3-access-key>
AWS_SECRET_KEY=<supabase-s3-secret-key>
AWS_REGION=<your-supabase-region>         # e.g. eu-west-1
AWS_ENDPOINT_URL=https://<project-ref>.supabase.co/storage/v1/s3
AWS_PUBLIC_URL_BASE=https://<project-ref>.supabase.co/storage/v1/object/public
AWS_PRODUCT_BUCKET=<your-products-bucket-name>
AWS_USER_BUCKET=<your-avatars-bucket-name>
AWS_DEFAULT_PRODUCT_IMAGES_PATH=./products
```

To disable S3 entirely (e.g. for local dev without file uploads):

```
AWS_ENABLED=false
```

---

## Redis Cache

Redis is an optional dependency. The app runs fully without it — all caches fall back to in-memory alternatives automatically.

### What is cached

| Cache | Key | TTL | Fallback |
|---|---|---|---|
| Single product | `productById::{uuid}` | 10 min | Caffeine |
| Product brands | `brands::brandNames` | 24 h | Caffeine |
| Product sellers | `sellers::sellerNames` | 24 h | Caffeine |
| S3 image URL | `imageUrl::{s3key}` | 50 min | Caffeine |
| Email verification token | `email:token:{email}` | per token TTL | Guava |
| Email send rate | `email:expiry:{email}` | per token TTL | Guava |
| JWT blacklist | `jwt:blacklist:{token}` | remaining token TTL | ConcurrentHashMap |
| Rate limiter (pre-auth) | `rate:pre-auth:ip:{ip}` | fixed window | Caffeine fixed window |
| Rate limiter (post-auth) | `rate:{category}:user:{userId}` or `rate:{category}:ip:{ip}` | fixed window | Caffeine fixed window |

### How to enable

Set these environment variables (see `.env` for local defaults):

```
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-password   # leave empty if no auth
REDIS_SSL_ENABLED=false        # set true for TLS (e.g. Upstash)
```

When Redis is unavailable or disabled, the app falls back to in-memory caches for the supported cache types. For the default local setup, `.env.example` points Redis to `localhost:6379`, which matches the `redis` Docker service.

### Upstash (free managed Redis)

[Upstash](https://upstash.com/) offers a free-tier Redis compatible with this setup. Use the TCP endpoint (not the REST URL):

```
REDIS_HOST=<your-db>.upstash.io
REDIS_PORT=6380
REDIS_PASSWORD=<your-upstash-password>
REDIS_SSL_ENABLED=true
```
