# Security Remediation Log

This document explains the security issues found in
`com.zufar.icedlatte.security` and the fixes that were applied.

The goal is not only to say "what changed", but to make the bug understandable
for someone who is still learning backend security.

Each issue uses this structure:

- What this feature is for
- How the old system behaved
- Why that behavior was a bug
- A simple example
- What the fix changed

## 1. Refresh Tokens Were Blacklisted for Too Short a Time

### What This Feature Is For

The blacklist is a "do not accept this token anymore" list.

It is used when a token must stop working before its natural expiration time.
For example:

- user logs out
- refresh token is rotated
- old refresh token is replaced
- suspicious session is revoked

There are two different token lifetimes:

```text
Access token:  short life, about 15 minutes
Refresh token: longer life, about 24 hours
```

### How the Old System Behaved

The old blacklist method always used the access-token expiration:

```java
temporaryStore.put(namespacedKey(token), "true", jwtProperties.expiration());
```

That means every blacklisted token stayed in the blacklist for only the access
token TTL, even when the token was actually a refresh token.

### Why That Was a Bug

A refresh token can live much longer than an access token. If a refresh token is
blocked for only 15 minutes, but the refresh token is valid for 24 hours, then
there is a dangerous gap.

During that gap, the blacklist forgot the token, but the token itself had not
expired yet.

### Simple Example

```text
12:00  Refresh token is created. It expires tomorrow at 12:00.
12:10  User refreshes. Old refresh token is blacklisted.
12:25  Blacklist entry expires because access-token TTL is only 15 minutes.
12:30  Old refresh token is tried again.
```

Before the fix, the old refresh token could be accepted again because the
blacklist entry was gone.

### What the Fix Changed

The blacklist now has separate paths:

```java
blacklist(token);              // for access tokens
blacklistRefreshToken(token);  // for refresh tokens
```

Refresh-token callers use `blacklistRefreshToken(...)`, which stores the
blacklist entry for `jwtProperties.refreshExpiration()`.

Now the blacklist remembers a blocked refresh token for as long as the refresh
token could still be valid.

## 2. JWT Validation Did Not Check Issuer and Audience

### What This Feature Is For

A JWT has claims. Two important claims are:

```text
issuer   (iss): who created this token
audience (aud): who this token is meant for
```

Signature validation answers:

```text
Was this token signed with the correct key?
```

Issuer and audience validation answer:

```text
Was this token created by the expected issuer?
Was this token meant for this backend API?
```

### How the Old System Behaved

The parser checked only the signature:

```java
Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
```

Tokens were issued with issuer and audience, but the parser did not require
those values when reading a token.

### Why That Was a Bug

A valid signature is necessary, but not always enough.

If another environment or service accidentally shares the same JWT signing key,
then a token from that other place could have a valid signature. Without issuer
and audience checks, this API could accept a token that was not created for it.

### Simple Example

```text
Token A:
  signed correctly
  issuer = iced-latte-api
  audience = iced-latte-frontend

Token B:
  signed correctly
  issuer = some-other-service
  audience = some-other-client
```

Before the fix, both could pass if the signature was valid. After the fix,
`Token B` is rejected because issuer/audience do not match.

### What the Fix Changed

JWT parsing now requires the configured issuer and audience. Tests cover:

- token with wrong issuer is rejected
- token with wrong audience is rejected
- token with correct issuer and audience is accepted

## 3. OAuth Could Create Accounts with an Unverified Provider Email

### What This Feature Is For

OAuth login lets a user sign in with a provider such as Google.

When Google sends a profile, it can include:

```text
email = user@example.com
emailVerified = true or false
```

`emailVerified=true` means Google says the user has proved they own that email
address.

### How the Old System Behaved

The old check protected only one case: linking OAuth to an existing local user.

```java
if (existingUser && !profile.emailVerified()) {
    throw new UnauthorizedException(...);
}
```

But when there was no existing local user, the service could still create a new
local account from an unverified provider email.

### Why That Was a Bug

This application uses email as account identity. If we create an account for
`alice@example.com`, we are saying "this OAuth user owns alice@example.com".

If the provider has not verified the email, the backend should not trust that
email for identity.

### Simple Example

```text
Provider profile:
  email = alice@example.com
  emailVerified = false

Old behavior:
  Existing local user? no
  Create new local user anyway
```

That creates a local account based on an email that was not confirmed by the
OAuth provider.

### What the Fix Changed

`OAuthLoginService` now rejects unverified provider emails before both actions:

- linking OAuth to an existing local user
- creating a new local user

The rule is now simple:

```text
No verified provider email = no OAuth login/account creation
```

## 4. Refresh-Token Rotation Was Race-Prone

### What This Feature Is For

Refresh-token rotation means a refresh token should be used only once.

Normal flow:

```text
Client sends old refresh token.
Backend checks it.
Backend creates a new refresh token.
Backend marks the old one as rotated.
Client must use the new one next time.
```

This reduces damage if an old refresh token is stolen.

### How the Old System Behaved

The old code followed a read-change-save pattern:

```java
session = repository.findByRefreshTokenHash(hash);
session.rotateTo(newHash);
repository.save(session);
```

That is fine for one request, but unsafe when two requests happen at the same
time.

### Why That Was a Bug

Two concurrent requests could both see the same refresh token as active before
either request saved the rotated state.

### Simple Example

```text
Database says refresh token R1 is active.

Request A starts:
  reads R1 as active

Request B starts at almost the same time:
  reads R1 as active

Request A:
  creates R2
  saves session with R2

Request B:
  also creates R3
  saves session with R3
```

Both requests received valid token pairs, even though `R1` should have been
single-use.

### What the Fix Changed

The repository now uses pessimistic write locks:

```java
findByRefreshTokenHashForUpdate(refreshTokenHash)
```

This tells the database:

```text
Lock this session row while this transaction is working with it.
Other transactions must wait before changing or reading it for update.
```

Fixed flow:

```text
Request A locks row for R1.
Request B waits.
Request A rotates R1 to R2 and commits.
Request B continues, sees R1 is no longer active, and fails.
```

Now only one refresh request can win.

## 5. Login-Attempt Counting Was Race-Prone

### What This Feature Is For

Login-attempt counting protects accounts from password guessing. After too many
bad passwords, the account or email can be temporarily locked.

### How the Old System Behaved

The old logic used read-increment-save:

```java
attempt.setAttempts(attempt.getAttempts() + 1);
repository.save(attempt);
```

### Why That Was a Bug

When many failed login requests happen at the same time, some increments can be
lost.

### Simple Example

```text
Current failed attempts = 3

Request A reads 3
Request B reads 3

Request A adds 1 and saves 4
Request B adds 1 and saves 4
```

There were two failed attempts, so the correct value should be `5`. The database
ended at `4`, so one failed attempt disappeared.

This matters because an attacker often sends bursts of login attempts. Lost
counts make lockout weaker exactly when it is most needed.

### What the Fix Changed

The login-attempt row is locked during update. That makes the second request
wait until the first request has saved.

Fixed flow:

```text
Current failed attempts = 3

Request A locks row, saves 4
Request B waits, then reads 4, saves 5
```

First-insert collisions are also handled, so two first attempts for the same
email do not create duplicate/conflicting rows.

## 6. Email Verification and Password Reset Used Global Short Codes

### What This Feature Is For

Email verification and password reset both send a temporary secret to the user.
Whoever has that secret can confirm an email or reset a password.

That means the token should be:

- random
- hard to guess
- tied to the correct user/email
- tied to the correct purpose

### How the Old System Behaved

The old storage was effectively keyed by the code:

```text
email:token:<code> -> request data
```

The code was short and numeric.

### Why That Was a Bug

There were two problems.

First, short numeric codes have limited randomness. A 9-digit code is better
than a 6-digit code, but it is still much weaker than a long random opaque
token.

Second, if two generated codes collide, one pending request can overwrite
another because the code is the global key.

### Simple Example

```text
Alice requests email verification.
Generated code: 123456789
Stored key: email:token:123456789 -> Alice request

Bob requests password reset.
Generated code: 123456789
Stored key: email:token:123456789 -> Bob request
```

Now Alice's pending request can be overwritten by Bob's request.

### What the Fix Changed

The system now uses long URL-safe opaque tokens instead of short numeric codes.

The storage is scoped by:

- purpose, such as email verification or password reset
- normalized email
- token

Conceptually:

```text
email:token:<purpose>:<normalized-email>:<token>
```

This makes collisions much less likely and prevents different purposes/users
from sharing one global token namespace.

## 7. Turnstile Verification Had No Explicit HTTP Timeout

### What This Feature Is For

Cloudflare Turnstile helps detect bots during login/register.

During authentication, the backend calls Cloudflare to verify the submitted
Turnstile token.

### How the Old System Behaved

The old verifier used a default REST client:

```java
RestClient.create()
```

### Why That Was a Bug

External network calls can hang or become slow. Authentication endpoints are
sensitive because users are waiting and backend request threads are occupied.

Without explicit timeouts, a Cloudflare/network problem could make login or
registration wait too long.

### Simple Example

```text
User submits login.
Backend calls Cloudflare.
Network stalls.
Request thread waits too long.
More users try login.
More request threads wait.
```

This can turn an upstream slowdown into an auth availability problem.

### What the Fix Changed

`TurnstileVerifier` now builds `RestClient` with explicit connect/read timeouts.

That gives the backend a clear rule:

```text
If Cloudflare cannot be reached quickly enough, fail the verification instead
of hanging indefinitely.
```

## 8. Security Imported a User Feature Internal Exception

### What This Feature Is For

The project is a modular monolith. Packages should communicate through stable
feature APIs, not through each other's internal implementation classes.

### How the Old System Behaved

Security imported a user feature internal exception:

```java
import com.zufar.icedlatte.user.exception.UserNotFoundException;
```

### Why That Was a Bug

This is not a security exploit by itself. It is an architecture/code quality
issue that can cause future bugs.

Security should not need to know how the user feature internally represents
"user not found". If user changes that exception, security can break.

### Simple Example

```text
User feature renames UserNotFoundException.
User public API still works.
Security package fails to compile because it imported the internal class.
```

That means the boundary between features was not clean.

### What the Fix Changed

Security now uses `UserLookupApi`, which is the stable boundary for looking up
users from outside the user feature.

The dependency direction is now:

```text
security -> user.api
```

instead of:

```text
security -> user.exception/internal implementation
```

## 9. JWT Auth and Refresh Ignored Current Account State

### What This Feature Is For

Account state decides whether a user is currently allowed to authenticate.

Important flags include:

- enabled
- account non-locked
- account non-expired
- credentials non-expired

### How the Old System Behaved

The backend loaded the user from the token, but did not enforce these flags:

```java
UserDetails userDetails = userDetailsService.loadUserByUsername(email);
return authenticatedToken(userDetails);
```

Refresh-token handling had the same kind of problem: it could issue fresh tokens
without checking whether the account was now locked or disabled.

### Why That Was a Bug

A token can outlive an account state change.

For example, an admin might disable a user at 12:00, but the user might still
have an access token issued at 11:55. If JWT auth does not check current account
state, that old token can keep working.

### Simple Example

```text
11:55 User logs in and receives tokens.
12:00 Admin disables the user.
12:01 User calls API with old access token.
12:02 User refreshes and receives new tokens.
```

Before the fix, those actions could still succeed because token signature/expiry
were checked, but current account state was not enforced.

### What the Fix Changed

`JwtAccountStatusValidator.requireActive(...)` now checks the user flags during:

- access-token authentication
- refresh-token handling

If the account is disabled, locked, expired, or has expired credentials, the
request is rejected.

## 10. Google OAuth Code Exchange Had No Explicit HTTP Timeout

### What This Feature Is For

Google OAuth login requires a backend call to Google. The backend exchanges the
temporary OAuth `code` for Google tokens/profile data.

### How the Old System Behaved

The old Google flow used default HTTP request settings:

```java
new GoogleAuthorizationCodeFlow.Builder(transport, json, clientId, secret, scopes)
```

### Why That Was a Bug

This is similar to the Turnstile timeout issue. Google is an external service.
The backend should not let an external network call hold an auth request longer
than intended.

### Simple Example

```text
User clicks "Continue with Google".
Google redirects back with code.
Backend exchanges code with Google.
Google/network is slow.
Login request waits too long.
```

### What the Fix Changed

`GoogleTokenExchanger` now uses a Google `HttpRequestInitializer` to set:

- connect timeout
- read timeout

The values are configured in `application.yaml`.

## 11. CORS Allowed Credentials Without Rejecting Wildcards

### What This Feature Is For

CORS controls which browser origins are allowed to call the backend.

Credentials mean sensitive browser-managed data may be included, such as:

- cookies
- authorization headers
- client certificates

When credentials are allowed, origins must be trusted and explicit.

### How the Old System Behaved

The old configuration accepted configured origin patterns directly:

```java
configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
configuration.setAllowCredentials(corsProperties.allowCredentials());
```

### Why That Was a Bug

`allowedOriginPatterns` supports wildcard-style patterns. That is useful in some
cases, but dangerous when credentials are enabled.

If someone accidentally configures `*` or a broad wildcard, browsers may allow
credentialed cross-origin requests from places that should not be trusted.

### Simple Example

```text
allow-credentials = true
allowed-origins = *
```

That means the backend intended to allow credentials, but did not force the
origin list to be specific.

### What the Fix Changed

`AppCorsConfiguration` now validates configuration at startup.

When `allow-credentials=true`, origins are rejected if they are:

- blank
- `null`
- contain `*`

This makes bad CORS config fail fast instead of silently starting with unsafe
settings.

## 12. OAuth Returned Tokens in the Redirect URL Fragment

### What This Feature Is For

After successful OAuth login, the backend must give the frontend the app's own
access/refresh tokens.

### How the Old System Behaved

The old redirect put both tokens in the URL fragment:

```java
callbackBase + "#token=" + accessToken + "&refreshToken=" + refreshToken
```

Example browser URL:

```text
https://app.example.com/auth/google/callback#token=ACCESS&refreshToken=REFRESH
```

### Why That Was a Bug

The fragment is not sent to the backend, which is good, but it is still in the
browser URL. Frontend JavaScript can read it. It can also appear in browser
history or debugging tools.

This is especially bad for refresh tokens because refresh tokens live longer
than access tokens.

### Simple Example

```text
User completes Google login.
Browser URL contains refreshToken=...
Any frontend script running on that page can read location.hash.
```

Even if the current frontend is trusted, keeping long-lived secrets out of URLs
is a safer design.

### What the Fix Changed

The OAuth callback now returns only a short-lived one-time handoff code:

```text
#oauthCode=<one-time-code>
```

Then the frontend calls the backend:

```http
POST /api/v1/auth/oauth/token?code=<one-time-code>
```

The real token pair is stored server-side by `OAuthTokenHandoffStore`. The code
can be consumed once and expires quickly.

Fixed flow:

```text
Backend creates app tokens.
Backend stores tokens under one-time code.
Backend redirects browser with only oauthCode.
Frontend sends oauthCode back to backend.
Backend returns tokens once.
Backend removes oauthCode from store.
```

## 13. Email Token Length Config Was Not Bounded

### What This Feature Is For

Token length controls how hard a verification/reset token is to guess. Longer
random tokens are safer.

### How the Old System Behaved

The old numeric token generation depended on integer math:

```java
Math.pow(10, tokenLength)
```

### Why That Was a Bug

Configuration can be wrong. If `email.verification-token-length` is set too low,
tokens become weak. If it is set too high for the old numeric algorithm, integer
bounds can overflow or break generation.

### Simple Example

```text
Safe config: token length is long enough.
Bad config: token length = 4.
Result: token has only 10,000 possible values.
```

That is too easy to guess for a security token.

### What the Fix Changed

Token generation now uses secure random bytes encoded as URL-safe text. The code
also validates the configured length and rejects values below the minimum secure
size.

Bad config now fails fast instead of creating weak tokens.

