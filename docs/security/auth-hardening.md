# Authentication security update — November 2026

This is a coordinated backend/frontend security release. Existing password sign-in, established provider identities and manual recovery codes remain supported. Passkeys are a separate feature.

## Intentional contract changes

- Public password registration requires email delivery and mailbox confirmation. It returns the same empty 200 response for new and existing addresses; disabled email returns 503. No registration session is created before confirmation.
- New passwords require 15–128 characters and the shared offline common-password policy. Login/current-password proof keep legacy password compatibility.
- OAuth initiation requires a SHA-256 browser challenge. Final handoff requires its matching verifier, delivered by the frontend proxy from an HttpOnly cookie.
- First-time OAuth identity linking by email is rejected. Existing password accounts must use their existing sign-in/recovery method; existing provider identities still work.
- Recovery links are intercepted before frontend rendering and stored in a five-minute HttpOnly cookie. Successful password changes revoke sessions and request a security notification.

The six exact compatibility-check exceptions in .github/oasdiff-auth-hardening.ignore record these approved security changes. They are matched by endpoint and full change description; the CI job still rejects unrelated incompatibilities. No broad check category is disabled.

The shared password policy is exposed as common :: password-policy because both security registration/recovery and user profile password changes enforce it. The blocklist source, revision, transformation and license are retained beside the resource under src/main/resources/security/. It is a periodically updated local snapshot, not an exhaustive breach database.

Mail dispatch uses a bounded executor. Forgot-password response semantics remain generic under cooldown and queue saturation; notification submission failure cannot change the result of an already committed password update.

Replay of an active rotated credential commits compromise containment. Credentials already revoked by logout or password change are rejected without revoking later sign-ins, so an old tab cannot invalidate a fresh login. Matched managed sessions never fall through the legacy-token migration path.
