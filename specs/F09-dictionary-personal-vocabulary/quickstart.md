# Quickstart: F09 Dictionary and Personal Vocabulary

Use this guide after implementation to validate the user journeys and cross-feature boundaries described in [spec.md](spec.md), [data-model.md](data-model.md), and [the API contract](contracts/f09-openapi.yaml).

## Prerequisites

- Apply the approved F09 schema migration to a clean PostgreSQL-compatible test database. Confirm it aligns with `DATA_short.md` and has not been split into duplicate unmerged migrations.
- Start the Spring Boot backend and React frontend with no real provider credentials.
- Seed published dictionary entries containing simplified and traditional Hanzi, normalized pinyin and Vietnamese meaning keywords. Include a withdrawn entry, a published entry with unavailable media, and entries that demonstrate whole-field, prefix and interior-substring matches.
- Create two authenticated learners and verify ownership isolation. Prepare one effective Free entitlement and one active Premium entitlement through the internal F03 test fixture; no browser upgrade flow is needed.
- Make F10 schedule commands available in the test context. Seed at least one reviewed word so restore can prove that review history is preserved.

## Functional validation

1. As a visitor, search the same published entry using simplified Hanzi, traditional Hanzi, tone-insensitive/case/space-normalized pinyin, and a Vietnamese keyword. Confirm only published summaries appear.
2. Confirm a Hanzi/pinyin whole-field match is ordered before a prefix, which is ordered before an interior substring. Verify a one-code-point query and an interior pinyin query.
3. Confirm query boundaries: trim surrounding whitespace; accept 1 and 120 Unicode code points; reject missing, whitespace-only and 121-code-point input. Confirm default page 0/size 20, valid range 1–50, invalid page/size rejection, and a valid out-of-range page's empty state.
4. Open a published entry. Verify required text fields render; verify unavailable media is omitted. Withdraw the entry and confirm detail returns `404` and it disappears from search.
5. As learner A, save an entry, add a note, list/read it, delete it, and restore it. Confirm its stable ID and note remain and F10 restores its original schedule/history. Confirm learner B and an `ADMIN` cannot read or mutate it.
6. Load the same saved word on two browser sessions. Save a note from the first session, then submit an edit with the stale version from the second. Expect `409 STATE_CONFLICT`, unchanged first note, and a reload before another edit.
7. Give a Free learner 20 active words. Save a new word and restore a removed word in separate checks. Each must remove the oldest active word based on `savedAt`, retain exactly 20 active words, preserve the removed record/note/history, and suspend its F10 schedule. Re-saving an active word must remove nothing.
8. Give a Premium learner more than 20 active words and save another. Confirm no capacity removal. Trigger the internal F03 Premium-expiry transition: retain the 20 newest by `savedAt`, preserve older records/notes/history as `DELETED`, and suspend their schedules. With 20 or fewer active words, expiry removes none.
9. Race two saves/restorations into a full Free vocabulary and race one save with an F03 expiry reconciliation. Confirm exactly one consistent capacity result, no duplicate saved word, no extra F10 schedule, and no failed request that removed a word.

## Required automated checks

Run the relevant suites after starting required local services:

```powershell
Set-Location backend
mvn verify

Set-Location ../frontend
npm test
npm run lint
npm run build
npm run test:e2e
```

The backend integration suite must cover published-only search, validation/pagination, ranking, owner isolation, encryption boundary, stale-note conflict, Free/Premium capacity, concurrent mutation, F03 expiry and F10 preservation. The frontend suite must cover loading, empty, validation, `404`, `409`, capacity-removal notice, keyboard access and visible focus behavior.

## Performance acceptance run

Run the isolated Playwright `performance` project only against a dedicated performance environment, never the shared development database. Seed 100,000 published entries and use the documented browser/network/database configuration.

```powershell
Set-Location frontend
npm run test:e2e -- --project=performance
```

The project starts 100 browser contexts. After a 2-minute warm-up, each context submits one valid search every 5 seconds for 10 minutes. Measure from UI search submission to visible correct results or visible correct empty state. At least 95% of submitted searches must complete in under 2 seconds; every timeout, error or incorrect visible result is a failure. Archive the percentile report with build IDs, browser version, dataset version, query/page-size mix and network profile.

## Safety checks

- Public results and details contain no draft, unpublished, archived or unavailable asset content.
- Application logs, error envelopes, metrics and test fixtures contain no personal-note plaintext/ciphertext or raw query text.
- Every protected route uses the standard envelope and owner-only `404` behavior; React does not infer plan, capacity, ownership or schedule decisions.
- The F09 code calls F03/F10 application services only. It does not access their repositories, invoke their HTTP routes, call `ai-service`, or consume AI allowance.
