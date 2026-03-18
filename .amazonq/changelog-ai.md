# AI / LangChain4j Changelog

## 2026-03-04 — AiProductSummaryGenerator refactor (SonarCloud)

### `AiProductSummaryGenerator`
- Replaced all `System.err.println` with `org.slf4j.Logger` (SLF4J) — `log.error` / `log.info` / `log.warn`
- Removed unused `args` parameter from `main` → renamed to `ignored`
- Fixed regex stack overflow: added possessive quantifier `*+` on inner group `(?:[^']|'')*+` to prevent catastrophic backtracking
- Reduced cognitive complexity from 27 → ~10 by extracting: `buildAiService`, `collectReviews`, `loadAlreadyDone`, `processProduct`, `summariseWithRetry`
- Extracted `REVIEW_PATTERN` and `OUT_FILE` as static constants

### `GoogleTokenExchanger`
- Replaced deprecated `GoogleNetHttpTransport.newTrustedTransport()` with `new NetHttpTransport()` (no TLS customisation needed; standard JVM trust store used)
- Removed `GeneralSecurityException` from constructor `throws` clause (no longer thrown); kept on `exchange()` where `verifier.verify()` still declares it
