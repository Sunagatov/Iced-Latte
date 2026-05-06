# 🔐 Security Policy

Iced Latte includes authentication, JWT sessions, Google OAuth2, file uploads, email flows, Stripe payments, webhooks, database access, and optional AI features. Please report security issues privately so they can be handled responsibly.

---

## ✅ Supported Scope

Security reports are accepted for the active `development` branch and the currently deployed public demo at:

- https://iced-latte.uk/

If you are testing locally, use the setup in [Getting Started](../docs/getting-started.md).

---

## 🚨 Report Privately

Please do **not** open a public GitHub issue for vulnerabilities.

Report vulnerabilities directly to the maintainer:

- 📧 **Email:** [zufar.sunagatov@gmail.com](mailto:zufar.sunagatov@gmail.com)
- 💬 **Telegram:** [@lucky_1uck](https://web.telegram.org/k/#@lucky_1uck)

Include as much detail as you can:

- affected URL, endpoint, or feature
- steps to reproduce
- expected vs actual behavior
- impact and severity estimate
- screenshots, logs, request/response examples, or proof-of-concept if safe
- whether the issue affects local setup, the public demo, or both

---

## 🧭 What To Report

Examples of useful security reports:

- authentication or authorization bypass
- JWT or refresh-token leakage
- account takeover paths
- privilege escalation
- SQL injection or unsafe query behavior
- stored or reflected XSS
- CSRF in sensitive flows
- unsafe file upload or file access
- sensitive data exposure
- payment or Stripe webhook validation issues
- broken rate limiting on login or sensitive endpoints
- insecure CORS, redirect, or OAuth callback behavior
- dependency vulnerabilities with a realistic exploit path

---

## 🔥 Severity Guide

| Severity | Examples |
|---|---|
| **Critical** | Account takeover, payment bypass, remote code execution, leaked production secrets |
| **High** | Auth bypass, privilege escalation, sensitive data exposure, exploitable file upload |
| **Medium** | Stored XSS, unsafe file access, webhook validation weakness, meaningful CSRF |
| **Low** | Minor information disclosure, missing headers with demonstrated impact |

---

## 🚫 Out of Scope

The following are usually not treated as security vulnerabilities unless they include a realistic exploit path:

- missing security headers without demonstrated impact
- dependency warnings without a working exploit path
- clickjacking on non-sensitive pages
- rate limits on non-sensitive public pages
- self-XSS
- spam reports without account, data, or security impact
- issues requiring physical access to a contributor machine
- social engineering against maintainers or contributors

---

## 🚫 Safe Testing Rules

Do not:

- attack, overload, or degrade the public demo
- access, modify, or delete data that does not belong to you
- run destructive scans against the live site
- publish exploit details before the issue is resolved
- test payment flows with real stolen or unauthorized payment data
- use social engineering, phishing, spam, or credential stuffing

If you accidentally access sensitive data, stop testing immediately. Include only the minimum evidence needed in your private report.

Use local Docker setup for deeper testing whenever possible.

---

## ⏱️ Response Timeline

Expected response:

| Step | Target |
|---|---|
| Acknowledgment | within 48 hours |
| Initial triage | within 7 days |
| Fix or mitigation plan | depends on severity and scope |
| Public disclosure | only after a fix or mitigation is available |

If the issue is critical and actively exploitable, say that clearly in the subject line.

---

## 🙏 Credit

Security researchers may be credited in release notes or project documentation if they want public acknowledgment and responsible disclosure rules were followed.
