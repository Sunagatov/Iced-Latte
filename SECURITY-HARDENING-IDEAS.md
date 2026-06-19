# Security Hardening Ideas

Last updated: 2026-06-15

This note tracks practical backend hardening ideas for bot abuse, hacking, and DDoS resilience.

## Already In Place

- Cloudflare proxy and WAF in front of the origin.
- Turnstile on high-risk write flows.
- Stateless JWT auth with refresh-token/session revocation.
- Password hashing with Argon2.
- Route-based rate limiting with repeat-offender bans.
- Strict webhook signature checks and idempotency for payment webhooks.
- Multipart size limits and request timeout settings.
- Security headers like HSTS, frame denial, and referrer policy.

## Still Worth Doing

### 1. Tighten rate limiting by route and identity

- Keep the current IP-based limiter, but add stricter budgets for:
  - login
  - password reset
  - signup
  - checkout
  - review posting
  - support-chat message sending
- Add per-account limits where it makes sense, not only per-IP.
- Keep separate limits for public browsing and sensitive write paths.

### 2. Add stronger abuse signals

- Record and alert on:
  - repeated 429 responses
  - repeated login failures
  - Turnstile failures
  - webhook signature failures
  - suspicious spikes in public write traffic
- Use these signals for manual review and Cloudflare rule tuning.

### 3. Lock down the origin

- Ensure only Cloudflare can reach the public origin directly.
- Keep admin and actuator surfaces as narrow as possible.
- Keep `/actuator` limited to health/readiness where possible.

### 4. Keep high-risk flows challenge-first

- Continue requiring Turnstile for:
  - registration
  - login
  - password reset
  - checkout
  - reviews
  - avatar uploads
  - support-chat writes
- Apply the same rule to any new write endpoint that can be abused at scale.

### 5. Reduce attack surface on public endpoints

- Fail fast on invalid input.
- Keep request bodies and multipart uploads small.
- Avoid expensive work before validation and authentication.
- Keep webhook handlers idempotent and narrow.

### 6. Harden credentials and secrets

- Rotate JWT, Stripe, and webhook secrets on a schedule.
- Keep secrets out of logs and source control.
- Review dependency updates and supply-chain risk regularly.

## Notes

- Cloudflare/WAF already covers the biggest volumetric DDoS layer.
- App-side rate limiting helps with abuse shaping, but it is not a replacement for edge protection.
- Full bot scoring or anomaly detection is possible later, but it is more complex than the measures above.
