<div align="center">
  <br>
  <img src="iced-latte-avatar.jpg" alt="Iced Latte" width="120">
  <h1>Iced Latte</h1>
  <p><strong>A production-grade Java coffee marketplace — built in the open, for engineers who want real experience.</strong></p>
  <p>
    <a href="https://iced-latte.uk/">🌐 Live Demo</a> ·
    <a href="https://iced-latte.uk/backend/api/docs/swagger-ui/index.html">📖 API Docs</a> ·
    <a href="https://github.com/Sunagatov/Iced-Latte/issues?q=is%3Aopen+label%3A%22good+first+issue%22">🟢 Good First Issues</a> ·
    <a href="https://t.me/zufarexplained">💬 Community</a>
  </p>

  [![CI Status](https://github.com/Sunagatov/Iced-Latte/actions/workflows/dev-branch-pr-deployment-pipeline.yml/badge.svg)](https://github.com/Sunagatov/Iced-Latte/actions)
  [![codecov](https://codecov.io/github/Sunagatov/Iced-Latte/branch/development/graph/badge.svg?token=515f0ca9-2c4d-4458-ba0b-baf1de67635e)](https://app.codecov.io/github/Sunagatov/Iced-Latte)
  [![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

  [![GitHub Stars](https://img.shields.io/github/stars/Sunagatov/Iced-Latte)](https://github.com/Sunagatov/Iced-Latte/stargazers)
  [![GitHub Forks](https://img.shields.io/github/forks/Sunagatov/Iced-Latte?style=social)](https://github.com/Sunagatov/Iced-Latte/network/members)
  [![Contributors](https://img.shields.io/github/contributors/Sunagatov/Iced-Latte)](https://github.com/Sunagatov/Iced-Latte/graphs/contributors)
  [![Docker Pulls](https://img.shields.io/docker/pulls/zufarexplainedit/iced-latte-backend.svg)](https://hub.docker.com/r/zufarexplainedit/iced-latte-backend/)
</div>

---

## What is this?

Iced Latte is a non-profit open-source project where a team of engineers are building a real-world coffee marketplace — to grow their skills by working on a production-grade codebase.

Started in 2022, it gives junior engineers, students, and mentees hands-on experience with real processes: code reviews, CI/CD, testing, and team collaboration.

> ⭐ If this project helps you learn or inspires you, please give it a star — it means a lot to the community!

---

## Table of Contents

- [Quick Start](#quick-start)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [Recognition](#recognition)
- [License](#license)
- [Contact](#contact)

---

## Quick Start

**Prerequisites:** Java 21, Maven 3.9+, Docker Desktop

```bash
# 1. Clone
git clone https://github.com/Sunagatov/Iced-Latte.git && cd Iced-Latte

# 2. Start PostgreSQL + Redis
docker-compose -f docker-compose.local.yml up -d iced-latte-postgresdb iced-latte-redis

# 3. Create your .env file (copy values from START.md), then run
./mvnw spring-boot:run
```

App runs at `http://localhost:8083` · Swagger UI at `http://localhost:8083/api/docs/swagger-ui/index.html`

**Test login:** `olivia@example.com` / `p@ss1logic11` (15 seed users, all share this password)

📄 Full setup guide (IntelliJ, Docker-only, troubleshooting): [START.md](START.md)

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Retry, Spring Actuator |
| Database | PostgreSQL 42.7, Liquibase 4.32 |
| Cache | Redis, Caffeine |
| Security | JWT (JJWT 0.12), Google OAuth2, TLS |
| Cloud | AWS S3 SDK 2.x |
| Payments | Stripe |
| Monitoring | Micrometer, Prometheus, OpenTelemetry |
| Testing | JUnit 5, Testcontainers, REST Assured, Instancio, Jacoco |
| Logging | Logback, Logstash encoder, SLF4J |
| API | OpenAPI 3, SpringDoc 2.8, OpenAPI Generator 7 |
| Mapping | MapStruct 1.6, Lombok |
| Deployment | Docker, GitHub Actions |

---

## Project Structure

```
src/main/java/com/zufar/icedlatte/
├── security/       # JWT auth, registration, login
├── product/        # Product catalog
├── cart/           # Shopping cart
├── order/          # Orders
├── review/         # Product reviews
├── favorite/       # Favorites list
├── payment/        # Stripe integration
├── email/          # Email verification
└── user/           # User profile management
```

---

## Contributing

Contributions are welcome. Here's how to get involved:

| Situation | Action |
|---|---|
| Found a bug | [Open an issue](https://github.com/Sunagatov/Iced-Latte/issues/new) with the `bug` label |
| Want a feature | Start a [Discussion](https://github.com/Sunagatov/Iced-Latte/discussions) first |
| Ready to code | Pick a [`good first issue`](https://github.com/Sunagatov/Iced-Latte/issues?q=is%3Aopen+label%3A%22good+first+issue%22), comment "I'm on it" |
| Big change | Comment on the issue before writing code — many tickets have hidden constraints |

### Issue labels

| Label | Meaning |
|---|---|
| 🟢 `good first issue` | Simple, well-scoped — great for first-timers |
| 🔴 `bug` | Something is broken |
| 🔵 `high priority` | Do this first |
| 🟡 `enhancement` | Accepted improvement to an existing module |
| 🟠 `new feature` | New functionality — discuss before starting |
| ⚪ `idea` | Needs design discussion — don't implement yet |

### Bug reports

- Search existing issues before opening a new one
- Clearly describe **observed** vs **expected** behaviour
- For minor fixes, just open a PR directly

### Pull requests

- Keep PRs focused — one concern per PR
- Make sure `mvn test` passes locally before pushing
- Reference the issue number in your PR description

### Forking

Forks are welcome. Please share useful features back via PR so the community benefits and your fork stays easy to sync.

---

## Recognition

Iced Latte has earned recognition from the broader tech community.

**GitHub Trending — May 22, 2024**
Reached [GitHub's Trending page](https://archive.ph/DRsD8) — gaining **85 stars in a single day** with 27 active contributors.

**Project stats:**

| Repository | Stars | Forks |
|---|---|---|
| [Backend](https://github.com/Sunagatov/Iced-Latte) | 590 ⭐ | 97 |
| [Frontend](https://github.com/Sunagatov/Iced-Latte-Frontend) | 209 ⭐ | 46 |
| [QA](https://github.com/Sunagatov/Iced-Latte-QA) | 146 ⭐ | 32 |

**JetBrains Open Source License** — granted 8 free All Products Pack licenses for non-commercial development (February 2024).

**KaiCode 2024 Finalist** — selected among 412 applications at the [KaiCode Festival](https://www.kaicode.org/2024.html#jury) by Huawei.

**Recommended by [Eddie Jaoude](https://www.linkedin.com/feed/update/urn:li:activity:7195685359710617602/)** — GitHub Star, calling it a great example of a Java open-source project.

> 📄 [Full achievement details](https://drive.google.com/file/d/1J7bs69RvkXGlALBrdOBX63DxEQQ5dg5k/view)

---

## License

[MIT](LICENSE) — use freely for private or commercial purposes with author attribution.

---

## Contact

- 💬 Telegram community: [Zufar Explained IT](https://t.me/zufarexplained)
- 📧 Email: [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)
- 🐛 Issues: [GitHub Issues](https://github.com/Sunagatov/Iced-Latte/issues)

❤️
