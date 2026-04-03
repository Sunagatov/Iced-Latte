# Idea: MongoDB for Audit Log

> 💡 Not yet implemented. Discuss in [GitHub Discussions](https://github.com/Sunagatov/Iced-Latte/discussions) before starting.

All current data in Iced Latte is relational and tightly joined — orders, users, cart, reviews all rely on foreign keys and transactions, which makes PostgreSQL the right fit for them.

The one place MongoDB genuinely fits is the **audit log**. The existing `audit_log` PostgreSQL table is append-only, never joined, and has no fixed schema requirement — exactly what a document store is good at. Moving it to MongoDB would mean:

- No schema migrations when new auditable fields are added
- Append-only writes with no locking contention on the main DB
- Free tier on [MongoDB Atlas](https://www.mongodb.com/atlas) is well-suited for this volume

## What would change

- Add `spring-boot-starter-data-mongodb` dependency
- Replace the `audit_log` PostgreSQL table with a MongoDB collection
- Use `@Document` instead of `@Entity` for the audit entity
- Configure `MONGODB_URI` env var pointing to Atlas

Everything else (products, orders, users, cart, reviews) stays in PostgreSQL.
