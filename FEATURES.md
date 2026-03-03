# Extended Features

This document covers the infrastructure and optional integrations that power Iced Latte. Each section explains what the component does, how it is configured, and which free-tier providers work out of the box — so you can run the full stack without spending anything.

## Table of Contents

- [PostgreSQL](#postgresql)
- [Object Storage (AWS S3 / Supabase)](#object-storage-aws-s3--supabase)
- [Redis Cache](#redis-cache)
- [MongoDB (Idea)](#mongodb-idea)

---

## PostgreSQL

The app uses PostgreSQL as its primary database, managed via Liquibase migrations.

### Schema

All tables are created and seeded automatically on startup via Liquibase (`drop-first: true` — the schema is recreated fresh on every start, which is intentional for this open-source project).

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

Configured via environment variables (see `local.env`):

```
DATASOURCE_HOST=localhost
DATASOURCE_PORT=5432
DATASOURCE_NAME=iced-latte
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=postgres
```

Connection pool: HikariCP, min 2 / max 10 connections. SSL required in production (`sslmode=require` in the JDBC URL).

### Seed data

15 users are seeded on every startup. All share the password `p@ss1logic11`. Test login: `olivia@example.com`.

---

## Object Storage (AWS S3 / Supabase)

The app uses AWS S3 SDK v2 for file storage. Any S3-compatible provider works — including [Supabase Storage](https://supabase.com/docs/guides/storage), which is free-tier and S3-compatible.

### What is stored

| Bucket | Contents |
|---|---|
| `AWS_PRODUCT_BUCKET` | Product images (path: `./products`) |
| `AWS_USER_BUCKET` | User avatar images |

Presigned URLs are generated per request with a 1-hour expiry and cached in Redis (or Caffeine) for 50 minutes.

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
| Rate limiter | `rate:{ip}` | sliding window | Caffeine token bucket |

### How to enable

Set these environment variables (see `local.env` for defaults):

```
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-password   # leave empty if no auth
REDIS_SSL_ENABLED=false        # set true for TLS (e.g. Upstash)
```

When `REDIS_HOST` is not set, the app starts normally with in-memory caches — no configuration needed for local development.

### Upstash (free managed Redis)

[Upstash](https://upstash.com/) offers a free-tier Redis compatible with this setup. Use the TCP endpoint (not the REST URL):

```
REDIS_HOST=<your-db>.upstash.io
REDIS_PORT=6380
REDIS_PASSWORD=<your-upstash-password>
REDIS_SSL_ENABLED=true
```

---

## MongoDB (Idea)

> 💡 This is a future idea, not yet implemented.

All current data in Iced Latte is relational and tightly joined — orders, users, cart, reviews all rely on foreign keys and transactions, which makes PostgreSQL the right fit for them.

The one place MongoDB genuinely fits is the **audit log**. The existing `audit_log` PostgreSQL table is append-only, never joined, and has no fixed schema requirement — exactly what a document store is good at. Moving it to MongoDB would mean:

- No schema migrations when new auditable fields are added
- Append-only writes with no locking contention on the main DB
- Free tier on [MongoDB Atlas](https://www.mongodb.com/atlas) is well-suited for this volume

### What would change

- Add `spring-boot-starter-data-mongodb` dependency
- Replace the `audit_log` PostgreSQL table with a MongoDB collection
- Use `@Document` instead of `@Entity` for the audit entity
- Configure `MONGODB_URI` env var pointing to Atlas

Everything else (products, orders, users, cart, reviews) stays in PostgreSQL.
