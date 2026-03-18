# API Test Changelog

## TODO

### Logout rate-limited in api-test.sh
- **[SKIP] Logout** — hits 429 (rate limit) before the logout request executes
- **[FAIL] Token rejected after logout** — cascading failure: logout was skipped so token was never blacklisted, follow-up check gets 200 instead of 401
- **Fix:** add a delay before the logout call in `api-test.sh`, or exempt the test IP from `RateLimitingFilter`
