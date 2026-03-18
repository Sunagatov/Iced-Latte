# Agent Rules

## Execution
- Batch all file reads into one step — never read files one by one
- Batch all bash commands into one script — never run them one by one
- Batch all edits to the same file into one call — never make incremental edits
- Before editing, identify ALL files that need changing and edit them together
- Do not ask for confirmation on obvious next steps — proceed and report

## Tests
- Run: `cd /Users/zufar/IdeaProjects/Iced-Latte && mvn test 2>&1 | tee /tmp/test-output.txt; echo "EXIT_CODE: $?"`
- Never re-run tests just to retrieve output — read `/tmp/test-output.txt`
- Only re-run after a fix has been applied
- Integration tests MUST share a single Spring context via `IntegrationTestBase`
- Never add to individual test classes: `@Import`, `@MockitoBean`, `@MockBean`, `@TestPropertySource`, `@DirtiesContext`, `@ActiveProfiles`

## API Tests
- Run: `cd /Users/zufar/IdeaProjects/Iced-Latte && bash api-test.sh 2>&1 | tee /tmp/api-test-output.txt; echo "EXIT_CODE: $?"`
- Never re-run just to retrieve output — read `/tmp/api-test-output.txt`

## API Specs (`src/main/resources/api-specs/`)
- Read all 9 specs in one batch before touching any
- Check `false-positives.md` first — if listed there, say "false positive" and stop
- Cross-reference the Java controller before changing any response code or schema
- Apply all changes to a file in one edit
- Review order: security correctness → schema correctness → response codes → consistency → dead content
- Track every fix in the matching changelog

## Changelogs
After every fix, append to the correct file in `.amazonq/`:

| File | Covers |
|---|---|
| `changelog-security.md` | Security, auth, JWT |
| `changelog-logging.md` | Logging levels, messages, MDC |
| `changelog-api-specs.md` | OpenAPI spec fixes |
| `changelog-startup.md` | Startup performance |
| `changelog-ai.md` | AI / LangChain4j |

## Code Analysis
- Read all callers before changing any method
- Check library contracts before adding guards — don't add redundant checks
- Trace all execution paths before concluding something is broken
- If a finding has no valid fix after full context analysis: say "false positive" and explain why
- If a fix is correct, defend it with specific technical reasoning — only revert if a concrete flaw is identified
