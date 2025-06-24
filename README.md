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
- [Github-Stars](#Github-Stars)

- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Deployment](#-deployment)
- [Forking and tweaking](#-forking-and-tweaking)
- [How to report a bug?](#-how-to-report-a-bug)
- [Now to propose a new feature?](#-now-to-propose-a-new-feature)
- [Contributions](#-contributions)
- [Code of Conduct](#code-of-conduct)
- [Our top contributors](#-our-top-contributors)
- [License](#-license)
- [Contact](#-contact-)

## Introduction

**🟥 Iced-Latte (Backend)** is a non-profit sandbox project where a team of IT enthusiasts are working on creating a modern marketplace (https://iced-latte.uk/) for selling coffee in order to pump up their soft and hard skills and have fun working on an interesting project.

🟥 It's crucial to note that we operate without financial support from any party, and we don't compensate anyone financially either. Our efforts are fuelled solely by passion and dedication.

### 🔥 Github-Stars 🔥

Please support Iced Latte project by Giving Stars 🌟 on Github repositories - your ratings mean a lot to us!🙂

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

## 💎 Now to propose a new feature?

- Go to our [Discussions](https://github.com/Iced-Latte/discussions)
- Check to see if someone else has already come up with the idea before
- Create a new discussion
- 🖼 If it's **UI/UX** related: attach a screenshot or wireframe
  
## Mock Server

You can mock and interact with the Payment API using the OpenAPI spec without setting up a backend. This is useful for quickly testing request/response flows, debugging client-side logic, or integrating during early development stages. The mock server generates structured, realistic responses based on the schema and examples.

<a href="https://beeceptor.com/openapi-mock-server/?utm_source=github&utm_campaign=iced-latte&url=https://raw.githubusercontent.com/Sunagatov/Iced-Latte/130565d657d813d7ee692b59c32f9f4abc03a0c3/src/main/resources/api-specs/payment-openapi.yaml" target="_blank"><img src="https://cdn.beeceptor.com/assets/images/buttons/mock-openapi-with-beeceptor.png" alt="Mock These APIs Instantly" style="height: 60px;"></a>


## 😍 Contributions

Contributions are welcome.

The main point of interaction is the [Issues page](https://github.com/Sunagatov/Iced-Latte/issues).

> The official development language at the moment is English, because 100% of our users speak it. We don't want to introduce unnecessary barriers for them. But we are used to writing commits and comments in Russian and we won't mind communicating with you in it.

The bussiness docs are here [Docs](https://drive.google.com/drive/folders/1vvfXy6n4cz01JjNyTgoYG0g6EIRvyHDw?usp=share_link).

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

#### 😎 Iced Latte project creator / Product owner / Tech Lead

- [@Sunagatov](https://github.com/Sunagatov)

#### 😇 Project manager
- [@oonovikova](https://github.com/oonovikova)

#### 🕵️‍♀️ QA engineers
- [@TetianaPerinha](https://github.com/TetianaPerinha)
- [@ilsinyakov](https://github.com/ilsinyakov)
- [@Diana-Smolnikova](https://github.com/Diana-Smolnikova)

#### ⚙️ Backend developers
- [@yevr19](https://github.com/yevr19) (co-founder)
- [@annstriganova](https://github.com/annstriganova)
- [@M437A](https://github.com/M437A)
- [@Sunagatov](https://github.com/Sunagatov)
- [@vitaliibredun](https://github.com/vitaliibredun)
- [@plakhov](https://github.com/plakhov)
- [@shpali4](https://github.com/shpali4)
- [@andrew13pol](https://github.com/andrew13pol)
- [@nnick44](https://github.com/nnick44)

#### 🖥️ Frontend developers
- [@iakivpekarskyi](https://github.com/iakivpekarskyi)
- [@anksuunamun-govorov](https://github.com/anksuunamun)
- [@DmitriyUshkvarok](https://github.com/DmitriyUshkvarok)
- [@Wdyffs](https://github.com/Wdyffs)
- [@Gerasko-Vadim](https://github.com/Gerasko-Vadim)
- [@FMajesty](https://github.com/Lightness322)
- [@freecree](https://github.com/freecree)

#### 🎨 UX/UI designers
- [@vilena1me](https://github.com/vilena1me)
- [@monsoonsiren](https://github.com/monsoonsiren)
- [@ilyasgaifullin](https://github.com/ilyasgaifullin)

#### 📊 Business and system analysts
- [@nemeziss](https://github.com/nemeziss)
- [@anastasiiaerokhina](https://github.com/anastasiiaerokhina)
- [@Umk_aa](https://github.com/Umk_aa)
- [@dianarudometova](https://github.com/dianarudometova)

Let's press F to pay respects to these awesome contributors!

## 👩‍💼 License

[MIT](LICENSE)

In other words, you can use the code for private and commercial purposes with an author attribution (by including the original license file or mentioning the Iced-Latte project).

## 📞 Contact (Community and Support)

Join our IT community [Zufar Explained IT](https://t.me/zufarexplained) on Telegram.

Feel free to contact us via email: [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com).

❤️
