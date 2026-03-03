# Contributing to Iced Latte

Thanks for your interest in contributing! Here's everything you need to get started.

## How to get involved

| Situation | Action |
|---|---|
| Found a bug | [Open an issue](https://github.com/Sunagatov/Iced-Latte/issues/new) with the `bug` label |
| Want a feature | Start a [Discussion](https://github.com/Sunagatov/Iced-Latte/discussions) first |
| Ready to code | Pick a [`good first issue`](https://github.com/Sunagatov/Iced-Latte/issues?q=is%3Aopen+label%3A%22good+first+issue%22), comment "I'm on it" |
| Big change | Comment on the issue before writing code — many tickets have hidden constraints |

## Issue labels

| Label | Meaning |
|---|---|
| 🟢 `good first issue` | Simple, well-scoped — great for first-timers |
| 🔴 `bug` | Something is broken |
| 🔵 `high priority` | Do this first |
| 🟡 `enhancement` | Accepted improvement to an existing module |
| 🟠 `new feature` | New functionality — discuss before starting |
| ⚪ `idea` | Needs design discussion — don't implement yet |

## Pull requests

- Keep PRs focused — one concern per PR
- Run `mvn test` locally before pushing — all 154 tests must pass
- Reference the issue number in your PR description

## Bug reports

- Search existing issues before opening a new one
- Clearly describe **observed** vs **expected** behaviour
- For minor fixes, just open a PR directly

## Local setup

See [START.md](START.md) for full setup instructions.

Quick version:
```bash
cp local.env .env
docker-compose -f docker-compose.local.yml up -d iced-latte-postgresdb iced-latte-redis
export $(cat .env | xargs) && mvn spring-boot:run
```
