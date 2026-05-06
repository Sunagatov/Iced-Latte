# 🤝 Contributing to Iced Latte

Thanks for your interest in contributing. Iced Latte is built in the open so engineers can practice on a real Java/Spring Boot codebase with real product flows, infrastructure, reviews, tests, and pull requests.

> **Intellectual property notice:** By submitting any contribution, you irrevocably assign all rights to the author (Zufar Sunagatov). Contributors have no ownership, copyright, or other IP claim over the Iced Latte project or any related repository. These terms have been in effect since the project's creation in 2022. See [`LICENSE`](../LICENSE) Sections 7-8 for full details.

---

## 📜 License & Contribution Terms

Before contributing, read the [Iced Latte Personal Evaluation License 2026](../LICENSE).

Important points:

- Contributions are accepted only under the project's license terms.
- By opening a PR, issue patch, code suggestion, documentation change, design, test, or other contribution, you assign the contribution rights to the author.
- Contributors do not receive ownership, copyright, trademark, commercial, publication, hosting, sublicensing, or redistribution rights.
- Personal local evaluation is allowed.
- Public, educational, remote-hosted, commercial, or redistributed use requires explicit written permission from the author.
- Do not contribute code, text, images, assets, or designs you do not have the right to submit.

If you do not agree with these terms, do not submit a contribution.

---

## 🧭 Start Here

| I want to... | What to do |
|---|---|
| 🟢 Make my first contribution | Pick a [`good first issue`](https://github.com/Sunagatov/Iced-Latte/issues?q=is%3Aopen+label%3A%22good+first+issue%22) and comment "I'm on it" |
| 🐛 Report a bug | [Open an issue](https://github.com/Sunagatov/Iced-Latte/issues/new) with clear observed vs expected behavior |
| 💡 Suggest a feature | Start a [Discussion](https://github.com/Sunagatov/Iced-Latte/discussions) before implementation |
| 🔧 Make a larger change | Comment on the issue first so constraints can be clarified |
| 🔐 Report a vulnerability | Follow the [Security Policy](SECURITY.md) instead of opening a public issue |

---

## 🏷️ Issue Labels

| Label | Meaning |
|---|---|
| 🟢 `good first issue` | Simple, well-scoped, and good for first-time contributors |
| 🔴 `bug` | Something is broken |
| 🔵 `high priority` | Important work that should be handled first |
| 🟡 `enhancement` | Improvement to an existing feature or module |
| 🟠 `new feature` | New functionality; discuss before starting |
| ⚪ `idea` | Needs design discussion; do not implement yet |

---

## 🚀 Local Setup

Use the full [Getting Started Guide](../docs/getting-started.md). It covers:

- backend-only setup
- backend + frontend setup
- Docker modes
- IntelliJ setup
- environment variables
- tests
- troubleshooting

For backend-only work, the common local flow is:

```bash
git clone https://github.com/Sunagatov/Iced-Latte.git
cd Iced-Latte

docker compose --env-file .env.example up -d postgres redis minio minio-init
set -a && source .env.example && set +a && mvn spring-boot:run
```

> 🪟 Windows PowerShell / CMD users: use IntelliJ with `.env.example` loaded into the run configuration. The `set -a && source ...` command is for macOS, Linux, and Git Bash.

---

## ✅ Before Opening a PR

Run the backend test suite:

```bash
mvn test
```

If your change touches API contracts, generated DTOs, database migrations, authentication, payments, or shared error handling, also check the relevant docs and OpenAPI specs under:

```text
docs/
src/main/resources/api-specs/
src/main/resources/db/changelog/
```

Before submitting:

- 🎯 Keep the PR focused on one concern
- ✅ Make sure `mvn test` passes locally
- 🔗 Link the related issue in the PR description
- 📝 Explain what changed and how you tested it
- 📸 Add screenshots or API examples when they help reviewers understand behavior
- 🚫 Do not include unrelated refactors, formatting churn, or generated noise

---

## 🧩 Code Expectations

- Follow the existing package structure and naming style.
- Keep business code inside the owning feature package.
- Put shared utilities in `common` only when they are genuinely cross-feature.
- Prefer small, readable changes over clever abstractions.
- Add or update tests when behavior changes.
- Keep public API behavior backward-compatible unless the issue explicitly says otherwise.

Architecture reference:

- [Feature Packaging Rule](../docs/architecture/feature-packaging.md)

---

## 🐛 Bug Reports

Good bug reports include:

- what you did
- what you expected
- what actually happened
- logs, screenshots, request/response examples, or stack traces if available
- your setup mode from [Getting Started](../docs/getting-started.md)

Before opening a bug:

- Search existing issues first
- Try the latest `development` branch if practical
- For small obvious fixes, opening a PR directly is fine

---

## 💡 Feature Requests

Start with a Discussion for new behavior, especially if it changes:

- API contracts
- database schema
- security/auth behavior
- payments
- order flow
- frontend/backend integration

For larger changes, wait for agreement before implementation. Many tickets have hidden constraints.

---

## 🔄 Pull Request Review

Reviewers will usually check:

- correctness
- test coverage
- API compatibility
- database migration safety
- security impact
- whether the change fits the feature-package architecture
- whether the PR stays focused

Expect review comments. That is normal project work, not a rejection.

---

## 🍴 Forks

Forks are welcome. If you build something generally useful, consider sending it back via PR so the community benefits and your fork stays easier to sync.
