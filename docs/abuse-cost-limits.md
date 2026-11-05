# AI and avatar limits

The backend uses PostgreSQL to reserve daily allowances before starting paid
operations. Limits are shared across users and application instances, survive
restarts, and reset at midnight UTC. Failed external attempts consume allowance
because a timeout can still be billable. If the allowance cannot be checked,
the external operation does not start.

Quota reservations use a separate pool of at most two connections to the same
PostgreSQL database. They commit independently of the business transaction and
cannot deadlock waiting for a connection held by an avatar request's user lock.

| Setting | Default | Meaning |
| --- | --- | --- |
| `AI_DAILY_CALLS` | 100 | Combined moderation and summary attempts per day |
| `AI_CONCURRENT_CALLS` | 2 | Active AI calls per backend process; extra calls are refused, not queued |
| `AI_MAX_INPUT_CHARS` | 20000 | Maximum supplied text; summaries use a bounded excerpt |
| `AI_MAX_OUTPUT_TOKENS` | 256 | Model output cap per request |
| `AVATAR_DAILY_UPLOADS` | 100 | New avatar attempts across the entire application per day |
| `AVATAR_DAILY_UPLOADS_PER_USER` | 10 | New avatar attempts per account per day |
| `AVATAR_LIVE_INTENTS_PER_USER` | 3 | Unexpired issued links, including cancelled/superseded links |

Set a daily allowance to `0` to stop new operations. `AI_ENABLED=false` still
disables the AI feature entirely. AI's HTTP client has a 20-second timeout and no
automatic SDK retries. Each background summary retry must reserve another call;
quota refusals do not trigger background retries.

These are usage limits, not a currency-denominated billing cap. A shared API key
used by other software is outside this backend's allowance. No email limits or
provider billing settings are changed by this feature.

## Avatar lifecycle

- Replacements use immutable keys. Metadata points to one current avatar and
  previous objects go through the existing deletion outbox. New URLs require no
  CloudFront invalidation, so an invalidation failure cannot undo avatar metadata.
- User-row locks serialize new intents, replacements, activation and deletion.
- A retry of the same pending intent keeps its original expiry and consumes no
  additional allowance. The storage signature binds its declared byte length.
- The browser supplies Content-Length automatically; it is not returned as a
  script-settable header. The configured maximum remains 5 MiB per avatar.
- Creating an intent schedules final source deletion for its original expiry
  plus the processing timeout (10 minutes by default). Cancelled links are still
  counted while they can write. Cleanup never deletes a READY active processed
  avatar.
- The existing retention job backfills final source cleanup for older rows and
  discards abandoned, inactive processed avatars. Late terminal completion
  objects are queued for deletion instead of becoming untracked files.
- Deferred source deletions have a separate deduplication identity, so an old
  completed early deletion cannot suppress final cleanup.

The defaults bound issued uploads; they do not make storage-provider URLs
single-use or limit direct provider request/egress charges. Keep an incoming
prefix lifecycle rule at the storage provider for uploads that finish unusually
late, and expire noncurrent versions if bucket versioning is enabled. This
backend change does not create or alter provider lifecycle rules.

## Release

Apply the additive Liquibase migration before starting code that uses the new
quota table and cleanup marker. Production runtime configuration and deployment
are owned by Vault. Existing AI provider/model credentials are unchanged by the
source changes; live AI tests are unnecessary for these safeguards.

When provisioning a native OpenAI key, pair it with
`AI_BASE_URL=https://api.openai.com/v1` and a compatible `AI_MODEL_NAME`.
The existing example uses an Azure/GitHub Models-compatible endpoint, whose
credentials are separate. Runtime credentials belong in Vault's encrypted
configuration, not source control.

Focused checks cover atomic admission on real PostgreSQL, allowance persistence
after transaction rollback, midnight rollover, concurrent upload intents,
provider retry/output limits, cleanup and the existing avatar flows.

Validation on 4 November 2026: 173 tests across 19 focused classes passed,
including 16 avatar endpoint integration tests and five PostgreSQL quota tests.
Targeted Palantir/Spotless formatting and `git diff --check` passed.

Repeat the main database/endpoint checks with:

```bash
mvn -Dtest=PersistentDailyOperationQuotaTest,UserAvatarEndpointIntegrationTest test
```

The security review also checked alternate upload entry points, terminal/replayed
completions, active-avatar preservation and quota reservations when the main
database pool is exhausted. Production storage lifecycle rules, billing settings
and live AI calls were not exercised or modified.
