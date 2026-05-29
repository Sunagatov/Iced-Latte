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

```text
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

```text
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

```text
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

```text
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

```text
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

```text
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

```text
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

The storage is scoped by token purpose and token identity:

```text
email:token:<purpose>:<token-identity>
```

The stored payload contains the normalized email and request data. Cooldown
storage is keyed separately by normalized email:

```text
email:rate:<normalized-email>
```

This makes guessing much harder, separates verification and reset flows by
purpose, and avoids the old short-code collision problem.

## 7. Turnstile Verification Had No Explicit HTTP Timeout

### What This Feature Is For

Cloudflare Turnstile helps detect bots during login/register.

During authentication, the backend calls Cloudflare to verify the submitted
Turnstile token.

### How the Old System Behaved

The old verifier used a default REST client:

```text
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

```text
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

```text
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

```text
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

```text
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

```text
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

```text
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

```text
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

## 14. Authentication Snapshot Exposed Encoded Credentials Too Generically

### What This Feature Is For

Spring Security needs an encoded password when it builds `UserDetails` for
username/password authentication.

The encoded password is not the plain password, but it is still credential
material. It should only be used by authentication code.

### How the Old System Behaved

The user module exposed an authentication snapshot with a field named
`passwordHash`:

```text
public record UserAuthenticationSnapshot(..., String passwordHash) {}
```

Security consumed that field here:

```text
snapshot.passwordHash()
```

### Why That Was a Smell

The value is sensitive, but the contract lived in the user API package. A future
developer could see the snapshot type and reuse it for profile or public user
responses by mistake.

In beginner terms: the backend still needs the encoded password to check a
login, but we do not want that value to look like normal user data.

### What the Fix Changed

The record field was renamed and documented as authentication-only:

```text
/**
 * Authentication-only user view for the security module.
 */
public record UserAuthenticationSnapshot(..., String encodedPassword) {}
```

Security now reads:

```text
snapshot.encodedPassword()
```

This does not expose the value to public API responses. It only makes the
internal contract clearer and harder to misuse.

## 15. Email Normalization Lived Under Security Instead of a Neutral Package

### What This Feature Is For

Security flows normalize emails before login, registration, password reset, and
OAuth account creation:

```text
" Alice@Example.COM " -> "alice@example.com"
```

The user module also needs the same rule when it checks or saves users.

### How the Old System Behaved

The shared normalizer lived in the security package:

```text
package com.zufar.icedlatte.security.util;
```

That was fine while only security used it. But once the user package needed the
same normalization rule, importing the security utility from user would have
created the wrong dependency direction.

The first user-side fix also briefly duplicated the logic in private helper
methods:

```text
private static String normalizeEmail(String email) {
    return Objects.requireNonNull(email, "email must not be null")
            .toLowerCase(Locale.ROOT)
            .trim();
}
```

### Why That Was a Smell

Security already depends on user APIs for authentication. The user package
should not depend back on security internals.

In beginner terms: if security and user both need the same generic email cleanup
rule, it belongs in a neutral shared package, not inside security.

### What the Fix Changed

The normalizer moved to:

```text
package com.zufar.icedlatte.common.util;
```

Security imports were updated from:

```text
import com.zufar.icedlatte.security.util.EmailNormalizer;
```

to:

```text
import com.zufar.icedlatte.common.util.EmailNormalizer;
```

The old `security.util.EmailNormalizer` file was deleted. The duplicated private
helpers in user services were removed.

The latest verification checked that no old `security.util.EmailNormalizer`
imports remain and that the broader user/security-focused test run passed:

```text
Tests run: 139, Failures: 0, Errors: 0, Skipped: 0
```

## 16. Prometheus Metrics Were Public

### What This Feature Is For

`/actuator/prometheus` exposes application metrics in a format that Prometheus
can scrape.

Those metrics are useful for operations because they can show things like:

- request counts
- response times
- error counts
- JVM memory usage
- database or HTTP client behavior

### How the Old System Behaved

The Spring Security configuration treated Prometheus metrics like a public
health endpoint:

```text
.requestMatchers(
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus",
        "/livez",
        "/readyz")
.permitAll()
```

The rate-limiting filter also skipped actuator paths:

```text
path.startsWith(ApiPaths.ACTUATOR_ROOT)
```

So an unauthenticated request could reach `/actuator/prometheus`, and that path
also bypassed the normal rate limit.

### Why That Was a Bug

Health endpoints are usually safe to expose because they answer a small
question:

```text
Is the application alive?
```

Prometheus metrics answer much more:

```text
Which routes exist?
How much traffic do they receive?
Which endpoints fail?
How long do requests take?
How does authentication behave under load?
```

That information is useful to engineers, but it can also help an attacker map
the system. The problem was not that Prometheus metrics are bad. The problem was
that they were available without authentication.

### Simple Example

Before the fix:

```text
GET /actuator/prometheus
Authorization: none

Result: 200 OK with internal metrics
```

A public user could inspect metrics that should normally be restricted to admins
or monitoring infrastructure.

### What the Fix Changed

The public actuator allow-list now keeps only lightweight public probes:

```text
.requestMatchers("/actuator/health", "/actuator/info", "/livez", "/readyz")
.permitAll()
```

All other actuator endpoints, including Prometheus metrics, require an admin
role:

```text
.requestMatchers(ApiPaths.ACTUATOR_ROOT + "**").hasRole("ADMIN")
```

Tests now cover the important cases:

- anonymous users cannot read `/actuator/prometheus`
- normal non-admin users cannot read `/actuator/prometheus`
- admin users can read `/actuator/prometheus`
- public health probes still work without login

## 17. Email Verification and Password Reset Tokens Were Visible in Cache Keys

### What This Feature Is For

Email verification and password reset flows create a temporary token.

The backend sends the real token to the user by email. It also stores a pending
request in temporary storage so it can validate the token later.

Simple flow:

```text
1. User asks to verify email or reset password.
2. Backend creates a temporary token.
3. Backend emails the token to the user.
4. User sends the token back.
5. Backend checks the temporary storage entry.
```

### How the Old System Behaved

The token was high entropy, which means it was hard to guess. But the token was
still used directly inside the temporary storage key:

```text
private static String tokenKey(TokenPurpose purpose, String token) {
    return TOKEN_KEY_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + token;
}
```

That created keys shaped like this:

```text
email:token:password_reset:<real-reset-token>
email:token:email_verification:<real-verification-token>
```

### Why That Was a Bug

The token is the secret. Whoever has a valid password reset token can complete
the password reset flow.

Using the raw token as a cache key does not make it easier to guess from the
outside, but it does expose the secret anywhere cache keys are visible.

Cache keys can appear in places such as:

- Redis admin tools
- cache debugging output
- operational screenshots
- metrics or logs around cache lookups

In beginner terms: the token was strong, but it was written on the label of the
storage box. The value inside the box may be protected, but the label itself was
still sensitive.

### Simple Example

Before the fix, an operator looking at Redis keys might see:

```text
email:token:password_reset:AbCDefGhIjK123...
```

That last part was the actual reset token. If copied, it could be submitted to
the password reset endpoint while the token was still valid.

### What the Fix Changed

The user still receives the real token by email. The frontend and API flow do
not need to change.

But the backend no longer uses the raw token as the storage key. It hashes the
token first:

```text
private static String tokenKey(TokenPurpose purpose, String token) {
    return TOKEN_KEY_PREFIX
            + purpose.name().toLowerCase(Locale.ROOT)
            + ":"
            + hashToken(token);
}
```

The hash is created with SHA-256 and encoded as URL-safe text:

```text
MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
```

Now the storage key looks more like this:

```text
email:token:password_reset:<sha-256-token-hash>
```

When the user submits the real token, the backend hashes the submitted token and
looks up the hashed key. That keeps the same user experience while avoiding raw
reset or verification tokens in cache keys.

## 18. JWT Access and Refresh Tokens Did Not Carry an Explicit Purpose

### What This Feature Is For

The application uses two JWT types:

```text
access token:
  short-lived token used to call normal API endpoints

refresh token:
  longer-lived token used only to get a new access/refresh token pair
```

Even when those tokens use different signing keys, the token itself should still
say what kind of token it is.

### How the Old System Behaved

Access tokens and refresh tokens were issued with normal JWT claims, but not
with an explicit purpose claim:

```java
claims.put(JwtClaimNames.JWT_ID, UUID.randomUUID().toString());
```

Refresh tokens had a version claim:

```java
claims.put(JwtClaimNames.VERSION, 2);
```

But `ver = 2` only described the refresh-token format version. It did not mean
"this token is allowed only in the refresh flow."

The parser checked signature, issuer, and audience, but did not require:

```text
purpose = access
purpose = refresh
```

### Why That Was a Bug

The access-token parser should only accept access tokens. The refresh-token
parser should only accept refresh tokens.

Before the fix, that separation mainly depended on using different signing keys.
That is good, but it is not defensive enough. If the access-token secret and
refresh-token secret were ever accidentally configured to the same value, a
longer-lived refresh token could be accepted where an access token was expected.

In beginner terms: the tokens had different keys, but they did not carry a clear
label saying "I am an access token" or "I am a refresh token."

### Simple Example

```text
Access-token parser:
  should accept only purpose=access

Refresh-token parser:
  should accept only purpose=refresh
```

Before the fix, the parser could not check that label because the label did not
exist.

### What the Fix Changed

JWT claim names now include explicit purpose constants:

```java
public static final String TOKEN_PURPOSE = "purpose";
public static final String ACCESS_TOKEN_PURPOSE = "access";
public static final String REFRESH_TOKEN_PURPOSE = "refresh";
```

Access-token generation writes:

```java
claims.put(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.ACCESS_TOKEN_PURPOSE);
```

Refresh-token generation writes:

```java
claims.put(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.REFRESH_TOKEN_PURPOSE);
```

The access-token parser now requires:

```java
require(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.ACCESS_TOKEN_PURPOSE)
```

The refresh-token parser now requires:

```java
require(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.REFRESH_TOKEN_PURPOSE)
```

Startup validation also rejects identical access and refresh signing keys.

## 19. Login Lockout Used Raw Email Input Before Normalization

### What This Feature Is For

Login lockout counts failed password attempts for a user email address. After
too many failures, the account can be temporarily locked.

### How the Old System Behaved

The login service used the request email directly:

```java
String userEmail = request.getEmail();

authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(userEmail, request.getPassword()));

loginAttemptService.recordFailure(userEmail);
```

The same real email could arrive in different textual forms:

```text
alice@example.com
Alice@Example.COM
" alice@example.com "
```

### Why That Was a Bug

Lockout must count attempts against the real account identity, not against the
exact string the attacker typed.

If raw email input is used, failed attempts can be split across multiple casing
or spacing variants. That can weaken lockout during burst attacks.

### Simple Example

```text
Failed attempt 1: alice@example.com
Failed attempt 2: Alice@Example.COM
Failed attempt 3: " alice@example.com "
```

Before the fix, those could be counted separately instead of being counted
against one normalized account.

### What the Fix Changed

`UserAuthenticationService` now normalizes the email once:

```java
String userEmail = EmailNormalizer.normalize(request.getEmail());
```

That normalized value is used for authentication and failed-attempt tracking:

```java
UsernamePasswordAuthenticationToken.unauthenticated(userEmail, request.getPassword())
loginAttemptService.recordFailure(userEmail);
```

`CustomUserDetailsService` also normalizes email before user lookup.

## 20. Password Reset Lookup Used Raw Email Input

### What This Feature Is For

Password reset starts when a user enters an email address. The backend looks up
that account and sends a temporary reset token.

### How the Old System Behaved

The reset service used the raw email string for lookup and reset-token sending:

```java
userLookupApi.findUserByEmail(email);
emailVerificationService.sendPasswordResetCode(email);
```

### Why That Was a Bug

The user lookup is exact-match at the repository layer. If the stored email is
normalized, then casing or surrounding spaces in the request can make a real
account look missing.

In beginner terms: the user typed the right email, but the backend compared the
wrong string.

### Simple Example

```text
Stored account email:
  alice@example.com

Reset request:
  Alice@Example.COM

Old lookup:
  find exactly "Alice@Example.COM"
```

That can fail even though the account exists.

### What the Fix Changed

`PasswordResetService` now normalizes first:

```java
String normalizedEmail = EmailNormalizer.normalize(email);
```

Then it uses the normalized value for both actions:

```java
userLookupApi.findUserByEmail(normalizedEmail);
emailVerificationService.sendPasswordResetCode(normalizedEmail);
```

So password reset now follows the same email identity rule as login and user
lookup.

## 21. Session Revocation Paths Were Still Race-Prone

### What This Feature Is For

Session revocation stops a refresh session from being used again.

It happens during flows such as:

- logout
- single-session revoke
- refresh-token replay cleanup

### How the Old System Behaved

Refresh-token rotation used pessimistic locks, but revocation paths still used
plain reads:

```java
sessionRepository.findByRefreshTokenHash(refreshTokenHash)
sessionRepository.findById(sessionId)
```

Then the service changed the session row.

### Why That Was a Bug

Plain reads do not coordinate with another transaction changing the same session
row. A refresh request and a logout/revoke request could both read the same
session as active and then save competing changes.

That is the same kind of last-write-wins race that refresh-token rotation had
already fixed.

### Simple Example

```text
Request A refreshes the token.
Request B logs out the same session.

Both read the active session.
Both change it.
The last save wins.
```

### What the Fix Changed

Revocation by refresh-token hash now uses:

```java
sessionRepository.findByRefreshTokenHashForUpdate(refreshTokenHash)
```

Revocation by session id now uses:

```java
sessionRepository.findByIdForUpdate(sessionId)
```

Replay cleanup also uses the locked id lookup.

Now all important session mutation paths acquire the row lock before changing
the session.
