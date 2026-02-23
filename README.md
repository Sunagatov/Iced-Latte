<div style="text-align: center;">
  <br>
  <img src="iced-latte-avatar.jpg" alt="">
  <h1>Iced-Latte</h1>

[![ci Status](https://github.com/Sunagatov/Iced-Latte/actions/workflows/dev-branch-pr-deployment-pipeline.yml/badge.svg)](https://github.com/Sunagatov/Iced-Latte/actions)
[![license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/danilqa/node-file-router/blob/main/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/Sunagatov/Iced-Latte)](https://github.com/Sunagatov/Iced-Latte/issues)
[![Total Lines of Code](https://tokei.rs/b1/github/Sunagatov/Iced-Latte?category=lines)](https://github.com/Sunagatov/Iced-Latte)
[![codecov](https://codecov.io/github/Sunagatov/Iced-Latte/branch/development/graph/badge.svg?token=515f0ca9-2c4d-4458-ba0b-baf1de67635e)](https://app.codecov.io/github/Sunagatov/Iced-Latte)

[![Docker Pulls](https://img.shields.io/docker/pulls/zufarexplainedit/iced-latte-backend.svg)](https://hub.docker.com/r/zufarexplainedit/iced-latte-backend/)
[![GitHub contributors](https://img.shields.io/github/contributors/Sunagatov/Iced-Latte)](https://github.com/Sunagatov/Iced-Latte/graphs/contributors)
[![GitHub stars](https://img.shields.io/github/stars/Sunagatov/Iced-Latte)](https://github.com/Sunagatov/Iced-Latte/stargazers)
[![Fork on GitHub](https://img.shields.io/github/forks/Sunagatov/Iced-Latte.svg?style=social)](https://github.com/Sunagatov/Iced-Latte/network/members)
</div>

## Table of Contents
- [Introduction](#introduction)
- [Recognition](#-recognition)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Deployment](#-deployment)
- [Forking and tweaking](#-forking-and-tweaking)
- [How to report a bug?](#-how-to-report-a-bug)
- [How to propose a new feature?](#-how-to-propose-a-new-feature)
- [Contributions](#-contributions)
- [Our top contributors](#-our-top-contributors)
- [License](#-license)
- [Contact](#-contact-)

## Introduction

**Iced-Latte (Backend)** is a non-profit open-source project where a team of IT enthusiasts are building a real-world [coffee marketplace](https://iced-latte.uk/) — to grow their engineering skills by working on a production-grade codebase.

Started in 2022 as a private pet project, it was opened to the community to give junior engineers, students, and mentees hands-on experience with real processes: code reviews, CI/CD, testing, and team collaboration. No financial compensation — just passion, learning, and good code.

> ⭐ If this project helps you learn or inspires you, please give it a star — it means a lot to the community!

## 🏆 Recognition

Iced Latte has grown beyond a pet project and earned recognition from the broader tech community.

**GitHub Trending — May 22, 2024**
The backend repository reached [GitHub's Trending page](https://archive.ph/DRsD8) — listed among the resources *"the GitHub community is most excited about today"* — gaining **85 stars in a single day** with 27 active contributors.

**Project stats across all three repositories:**

| Repository | Stars | Forks | Lines of Code |
|---|---|---|---|
| [Iced Latte Backend](https://github.com/Sunagatov/Iced-Latte) | 625 ⭐ | 97 | 19.3K |
| [Iced Latte Frontend](https://github.com/Sunagatov/Iced-Latte-Frontend) | 209 ⭐ | 46 | 8.4K |
| [Iced Latte QA](https://github.com/Sunagatov/Iced-Latte-QA) | 146 ⭐ | 32 | 8K |

**JetBrains Open Source License**
JetBrains recognized Iced Latte's impact on the open-source community and granted the project **8 free All Products Pack licenses** for non-commercial development (February 2024). See the [JetBrains Open Source support program](https://www.jetbrains.com/community/opensource/).

**KaiCode Festival Finalist**
Iced Latte reached the finals of [KaiCode 2024](https://www.kaicode.org/2024.html#jury) — an annual open-source festival by Huawei — selected among **412 applications** as one of the most promising open-source projects.

**Recommended by Eddie Jaoude**
The project was [recommended by Eddie Jaoude](https://www.linkedin.com/feed/update/urn:li:activity:7195685359710617602/) — one of the most influential open-source experts and a [GitHub Star](https://stars.github.com/) (174K followers on X, 17.6K on LinkedIn) — calling it a great example of a Java open-source project.

### 🔥 Support the project

Please support Iced Latte by giving it a ⭐ on GitHub — your stars help more engineers discover it!

> 📄 Full achievement details: [Iced Latte — Creating a Popular Open Source Project](https://drive.google.com/file/d/1J7bs69RvkXGlALBrdOBX63DxEQQ5dg5k/view)

## Tech Stack

- **Architecture:** Monolith.
- **Computer language:** Java 17.
- **Framework:** Spring Web, Spring Boot 3, Spring Data, Spring Security, Spring Actuator, Spring Web, Spring Retry, Lombok, Apache Commons, Spring Mail, Google Guava.
- **Security:** JWT, TLS.
- **Migration tool:** Liquabase.
- **Logging:** Log4j2, Slf4j.
- **Unit Tests:** JUnit 5.
- **E2E Tests**: Rest Assured, Test containers.
- **Converter:** Mapstruct.
- **Test coverage:** Jacoco.
- **API Specs:** Open API + Spring Docs.
- **Validation:** Javax validation.

## Quick Start

Follow the setup instructions in [START.MD](START.md) to get the project up and running.

## 🚢 Deployment

No k8s, no AWS, we ship dockers directly via ssh and it's beautiful!

The entire production configuration is described in the [docker-compose.local.yml](docker-compose.local.yml) file.

Then, [Github Actions](.github/workflows/dev-branch-pr-deployment-pipeline.yml) have to take all the dirty work. They build, test and deploy changes to production on every merge to master (only official maintainers can do it).

Explore the whole [.github](.github) folder for more insights.

We're open for proposals on how to improve our deployments.

## 🛤 Forking and tweaking

Forks are welcome.

Three huge requests for everyone:

- Please share new features you implement with us, so other folks can also benefit from them, and your own codebase minimally diverges from the original one (so you can sync updates and security fixes) .
- Do not use our issues and other official channels as a support desk. Use [chats](https://t.me/lucky_1uck).


## 🙋‍♂️ How to report a bug?

- 🆕 Open [a new issue](https://github.com/Sunagatov/Iced-Latte/issues/new).
- 🔦 Please, **use a search**, to check, if there is already existed issue!
- Explain your idea or proposal in all the details:
   - Make sure you clearly describe "observed" and "expected" behaviour. It will dramatically save time for our contributors and maintainers.
   - **For minor fixes** please just open a PR.

## 💎 How to propose a new feature?

- Go to our [Discussions](https://github.com/Iced-Latte/discussions)
- Check to see if someone else has already come up with the idea before
- Create a new discussion
- 🖼 If it's **UI/UX** related: attach a screenshot or wireframe

## 😍 Contributions

Contributions are welcome.

The main point of interaction is the [Issues page](https://github.com/Sunagatov/Iced-Latte/issues).

> The official development language at the moment is English, because 100% of our users speak it. We don't want to introduce unnecessary barriers for them. But we are used to writing commits and comments in Russian and we won't mind communicating with you in it.

The business docs are here [Docs](https://drive.google.com/drive/folders/1vvfXy6n4cz01JjNyTgoYG0g6EIRvyHDw?usp=share_link).

Swagger REST APIs contracts described [here](https://iced-latte.uk/backend/api/docs/swagger-ui/index.html).

### 😎 I want to write some code

- Open our [Issues page](https://github.com/Sunagatov/Iced-Latte/issues) to see the most important tickets at top.
- Pick one issue you like and **leave a comment** inside that you're getting it.

**For big changes** open an issues first or (if it's already opened) leave a comment with brief explanation what and why you're going to change. Many tickets hang open not because they cannot be done, but because they cause many logical contradictions that you may not know. It's better to clarify them in comments before sending a PR.

### 🚦Pay attention to issue labels!

#### 🟩 Ready to implement

- **good first issue** — good tickets **for first-timers**. Usually these are simple and not critical things that allow you to quickly feel the code and start contributing to it.
- **bug** — if **something is not working**, it needs to be fixed, obviously.
- **high priority** — the **first priority** tickets.
- **enhancement** — accepted improvements for an existing module. Like adding a sort parameter to the feed. If improvement requires UI, **be sure to provide a sketch before you start.**

#### 🟨 Discussion is needed

- **new feature** —  completely new features. Usually they're too hard for newbies, leave them **for experienced contributors.**
- **idea** — **discussion is needed**. Those tickets look adequate, but waiting for real proposals how they will be done. Don't implement them right away.

#### 🟥 Questionable

- [¯\\_(ツ)\_/¯](https://github.com/Sunagatov/Iced-Latte/labels/%C2%AF%5C_%28%E3%83%84%29_%2F%C2%AF) - special label for **questionable issues**. (should be closed in 60 days of inactivity)
- **[no label]** — ticket is new, unclear or still not reviewed. Feel free to comment it but **wait for our maintainers' decision** before starting to implement it.

## 👍 Our top contributors

Take some time to press F and give some respects to our [best contributors](https://github.com/Sunagatov/Iced-Latte/graphs/contributors), who spent their own time to make the club better.

## 👩‍💼 License

[MIT](LICENSE)

In other words, you can use the code for private and commercial purposes with an author attribution (by including the original license file or mentioning the Iced-Latte project).

## 📞 Contact (Community and Support)

Join our IT community [Zufar Explained IT](https://t.me/zufarexplained) on Telegram.

Feel free to contact us via email: [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com).

❤️
