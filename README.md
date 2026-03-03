<div align="center">
  <br>
  <img src="iced-latte-avatar.jpg" alt="Iced Latte" width="500">
  <h1>Iced Latte</h1>
  <p><strong>A production-grade Java coffee marketplace — built in the open, for engineers who want real experience.</strong></p>
  <p>
    <a href="https://iced-latte.uk/">🌐 Live Demo</a> ·
    <a href="https://iced-latte.uk/backend/api/docs/swagger-ui/index.html">📖 API Docs</a> ·
    <a href="https://github.com/Sunagatov/Iced-Latte/issues?q=is%3Aopen+label%3A%22good+first+issue%22">🟢 Good First Issues</a> ·
    <a href="https://t.me/zufarexplained">💬 Community</a>
  </p>

  [![CI Status](https://github.com/Sunagatov/Iced-Latte/actions/workflows/ci.yml/badge.svg)](https://github.com/Sunagatov/Iced-Latte/actions)
  [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Sunagatov_Iced-Latte&metric=alert_status)](https://sonarcloud.io/project/overview?id=Sunagatov_Iced-Latte)
  [![codecov](https://codecov.io/github/Sunagatov/Iced-Latte/branch/development/graph/badge.svg?token=515f0ca9-2c4d-4458-ba0b-baf1de67635e)](https://app.codecov.io/github/Sunagatov/Iced-Latte)
  [![License: CC BY-NC 4.0](https://img.shields.io/badge/license-CC%20BY--NC%204.0-lightgrey.svg)](LICENSE)

  [![GitHub Stars](https://img.shields.io/github/stars/Sunagatov/Iced-Latte)](https://github.com/Sunagatov/Iced-Latte/stargazers)
  [![GitHub Forks](https://img.shields.io/github/forks/Sunagatov/Iced-Latte?style=social)](https://github.com/Sunagatov/Iced-Latte/network/members)
  [![Contributors](https://img.shields.io/github/contributors/Sunagatov/Iced-Latte)](https://github.com/Sunagatov/Iced-Latte/graphs/contributors)
  [![Docker Pulls](https://img.shields.io/docker/pulls/zufarexplainedit/iced-latte-backend.svg)](https://hub.docker.com/r/zufarexplainedit/iced-latte-backend/)
</div>

---

**📊 Key stats across all three repositories:**

| Repository | ⭐ Stars | 🍴 Forks |
|---|---|---|
| [🔧 Backend](https://github.com/Sunagatov/Iced-Latte) | ![Stars](https://img.shields.io/github/stars/Sunagatov/Iced-Latte?style=flat) | ![Forks](https://img.shields.io/github/forks/Sunagatov/Iced-Latte?style=flat) |
| [🎨 Frontend](https://github.com/Sunagatov/Iced-Latte-Frontend) | ![Stars](https://img.shields.io/github/stars/Sunagatov/Iced-Latte-Frontend?style=flat) | ![Forks](https://img.shields.io/github/forks/Sunagatov/Iced-Latte-Frontend?style=flat) |
| [🧪 QA](https://github.com/Sunagatov/Iced-Latte-QA) | ![Stars](https://img.shields.io/github/stars/Sunagatov/Iced-Latte-QA?style=flat) | ![Forks](https://img.shields.io/github/forks/Sunagatov/Iced-Latte-QA?style=flat) |

> ⭐ If this project helps you learn or inspires you, please give it a star — it means a lot to the community!

---

## 🚀 Quick Start

**📋 Prerequisites:** Java 25, Maven 3.9+, Docker Desktop

```bash
# 1. 📥 Clone
git clone https://github.com/Sunagatov/Iced-Latte.git && cd Iced-Latte

# 2. 🐳 Start infrastructure (PostgreSQL, Redis, MinIO)
docker compose up -d postgres redis minio minio-init

# 3. ▶️ Run (uses committed .env with safe local defaults)
# Linux / macOS / Git Bash on Windows:
export $(cat .env | xargs) && mvn spring-boot:run
```

> 🪟 **Windows (PowerShell / CMD):** the `export` command above won't work. Use the IntelliJ EnvFile plugin or the full Docker path instead — see [START.md](START.md).

🌐 App runs at `http://localhost:8083` · 📚 Swagger UI at `http://localhost:8083/api/docs/swagger-ui/index.html`

**🔐 Test login:** `olivia@example.com` / `p@ss1logic11` (15 seed users, all share this password)

> 💡 Using IntelliJ? See [START.md](START.md) for IDE run configuration, Docker-only setup, and troubleshooting.

> 🎞️ **Want to run the frontend too?** Clone the frontend repo as a sibling and use Option C in [START.md](START.md):
> ```bash
> git clone https://github.com/Sunagatov/Iced-Latte-Frontend.git  # sibling of Iced-Latte/
> docker compose --profile backend --profile frontend up -d --build
> ```
> Frontend at http://localhost:3000 — built from source every time.

**🧪 Run the tests:**
```bash
mvn test
```
✅ Tests use Testcontainers — Docker must be running.

---

## 📸 Preview

<div align="center">
  <img src="Iced-Latte-Main-Page-Screenshot.png" alt="Iced Latte Main Page" width="800">
  <p><em>Live application interface</em></p>
</div>

---

## 🤔 What is this?

Iced Latte is a non-profit sandbox project started in 2022 as a private pet project, then opened to the community to give junior engineers, students, and mentees practical experience in a real tech project with processes similar to those in actual tech teams. The first participants were students, Telegram channel subscribers, and mentees from ADPList and Women In Tech. The project has since grown and earned recognition from both the open-source community and the wider tech community.

> ⭐ If this project helps you learn or inspires you, please give it a star — it means a lot to the community!

---

## 🏆 Recognition

Iced Latte has earned recognition from the broader tech community.

**🔥 GitHub Trending 🔥 — May 22, 2024**

  - The backend repository reached GitHub's Trending page — listed among resources *"the GitHub community is most excited about today"* — gaining **85 stars in a single day** with 27 active contributors. ([link to the archive](https://archive.ph/DRsD8))

**🥉 KaiCode 2024 Finalist 🥉** 

  - Iced Latte made it to the finals of [KaiCode](https://www.kaicode.org/2024.html#jury) — an annual open-source festival launched by Huawei, which positions itself as an incubator of open-source technologies and rewards the most promising projects. Iced Latte was selected among **412 applications** and placed in the third group of 26 finalist repositories considered for the prize. Jury members are not allowed to assess their own projects, so the selection was fully independent.

**🛠️JetBrains Open Source License 🛠**

  - Iced Latte was recognized by [JetBrains](https://www.jetbrains.com/community/opensource/) — a leading software company specializing in intelligent development tools. As an active participant in the open-source community, JetBrains supports recognized global open-source projects by providing complimentary licenses for non-commercial development. JetBrains granted Iced Latte **8 free All Products Pack licenses** (February 2024, License Reference No. D379769990).

**👨💻 Recommended by Eddie Jaoude 👨**

  - Iced Latte was [recommended by Eddie Jaoude](https://www.linkedin.com/feed/update/urn:li:activity:7195685359710617602/) — one of the most influential open-source experts, a [GitHub Star](https://stars.github.com/) with 174K followers on X and 17.6K on LinkedIn — who called it a great example of a Java open-source project. Many Iced Latte contributors shared their positive experience in the comments.

---

## 🛠️ Tech Stack

| 📂 Category | 🔧 Technology |
|---|---|
| 💻 Language | Java 25 |
| 🏗️ Framework | Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Retry, Spring Actuator |
| 🗄️ Database | PostgreSQL, Liquibase 4.32 |
| ⚡ Cache | Redis, Caffeine |
| 🔒 Security | JWT (JJWT 0.12), Google OAuth2, TLS |
| ☁️ Cloud | AWS S3 SDK 2.x |
| 📊 Monitoring | Micrometer, Prometheus, OpenTelemetry |
| 🧪 Testing | JUnit 5, Testcontainers, REST Assured, Instancio, Jacoco |
| 📝 Logging | Logback, Logstash encoder, SLF4J |
| 📋 API | OpenAPI 3, SpringDoc 2.8, OpenAPI Generator 7 |
| 🔄 Mapping | MapStruct 1.6, Lombok |
| 🚢 Deployment | Docker, GitHub Actions |

---

## 📚 Guides & Features

| 📄 Document | 📝 Description |
|---|---|
| [START.md](START.md) | IDE setup, Docker-only mode, troubleshooting |
| [FEATURES.md](FEATURES.md) | How the database, object storage, and Redis cache are wired together — with free-tier provider options (Supabase, Upstash) and all env vars explained |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to contribute, PR guidelines, branching |
| [SECURITY.md](SECURITY.md) | Security policy and vulnerability reporting |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Community standards and expected behaviour |
| [LICENSE](LICENSE) | CC BY-NC 4.0 — free for educational use, commercial use requires permission |

---

## 📁 Project Structure

```
src/main/java/com/zufar/icedlatte/
├── 🔒 security/       # JWT auth, Google OAuth2, registration, login
├── 🔑 auth/           # Google OAuth2 callback, auth redirects
├── 👤 user/           # User profile management
├── 📦 product/        # Product catalog
├── 🛒 cart/           # Shopping cart
├── 📋 order/          # Orders
├── ⭐ review/         # Product reviews & ratings
├── ❤️ favorite/       # Favorites list
├── 📧 email/          # Email verification & notifications
├── 📁 filestorage/    # AWS S3 file upload/download
├── 🔧 common/         # Shared utilities, validation, monitoring
└── 🚀 astartup/       # Startup data migration
```

---

## 🚢 Deployment

🚫 No Kubernetes, no cloud-managed services — the app ships as a Docker image to Render.

On every merge to `master`, the CD pipeline builds, pushes to Docker Hub, and deploys to Render automatically. Only maintainers can merge to `master`.

| Pipeline | Trigger | What it does |
|---|---|---|
| [CI](.github/workflows/ci.yml) | PR → `development` | Build, test, Codecov, SonarCloud, OWASP, OpenAPI breaking-change check |
| [CD](.github/workflows/cd.yml) | Push to `master` | Build Docker image, push to Docker Hub, deploy to Render, smoke test, Telegram notify |
| [Build deps image](.github/workflows/build-deps-image.yml) | `pom.xml` changed on `master` or `development` | Rebuilds the Maven deps base image to keep builds fast |
| [Stale](.github/workflows/stale.yml) | Every Monday | Marks issues/PRs stale after 60 days, closes after 30 |

---

## 🤝 Contributing

🎉 Contributions are welcome. Here's how to get involved:

| 🎯 Situation | 🚀 Action |
|---|---|
| 🐛 Found a bug | [Open an issue](https://github.com/Sunagatov/Iced-Latte/issues/new) with the `bug` label |
| 💡 Want a feature | Start a [Discussion](https://github.com/Sunagatov/Iced-Latte/discussions) first |
| 👨💻 Ready to code | Pick a [`good first issue`](https://github.com/Sunagatov/Iced-Latte/issues?q=is%3Aopen+label%3A%22good+first+issue%22), comment "I'm on it" |
| 🔧 Big change | Comment on the issue before writing code — many tickets have hidden constraints |

---

### 🏷️ Issue labels

| 🏷️ Label | 📝 Meaning |
|---|---|
| 🟢 `good first issue` | Simple, well-scoped — great for first-timers |
| 🔴 `bug` | Something is broken |
| 🔵 `high priority` | Do this first |
| 🟡 `enhancement` | Accepted improvement to an existing module |
| 🟠 `new feature` | New functionality — discuss before starting |
| ⚪ `idea` | Needs design discussion — don't implement yet |

---

### 🐛 Bug reports

- 🔍 Search existing issues before opening a new one
- 📝 Clearly describe **observed** vs **expected** behaviour
- 🚀 For minor fixes, just open a PR directly

---

### 🔄 Pull requests

- 🎯 Keep PRs focused — one concern per PR
- ✅ Make sure `mvn test` passes locally before pushing
- 🔗 Reference the issue number in your PR description

---

### 🍴 Forking

🤝 Forks are welcome. Please share useful features back via PR so the community benefits and your fork stays easy to sync.

---

## 📄 License

📜 [CC BY-NC 4.0](LICENSE) — free for educational and personal use with author attribution. Commercial use requires explicit written permission from the author ([zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)).

---

## 📞 Contact

- 💬 **Telegram community:** [Zufar Explained IT](https://t.me/zufarexplained)
- 👤 **Personal Telegram:** [@lucky_1uck](https://web.telegram.org/k/#@lucky_1uck)
- 📱 **WhatsApp:** [Message me](https://wa.me/447405503609)
- 📧 **Email:** [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)
- 🐛 **Issues:** [GitHub Issues](https://github.com/Sunagatov/Iced-Latte/issues)

❤️