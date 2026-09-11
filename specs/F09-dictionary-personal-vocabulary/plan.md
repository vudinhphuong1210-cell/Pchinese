# Implementation Plan: F09 Dictionary and Personal Vocabulary

**Branch**: `F09-dictionary-personal-vocabulary` | **Date**: 2026-09-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification at `specs/F09-dictionary-personal-vocabulary/spec.md`

## Summary

Deliver public discovery of published Chinese dictionary entries and an authenticated learner-owned vocabulary. The backend normalizes and ranks dictionary search, prevents publication leaks, encrypts personal notes, and owns every saved-word transition. It also enforces the 20-word Free capacity, retains unlimited Premium vocabulary, and coordinates Premium expiry with F03 and schedule suspension/restoration with F10. React displays server-authoritative results and state only.

## Technical Context

| Area | Decision |
| --- | --- |
| Language / version | Java 21 with Spring Boot 3.4.5; JavaScript/JSX with React 18 and Vite |
| Primary dependencies | Spring Data JPA, Jakarta Validation, Spring Security, PostgreSQL 18; Tailwind CSS 3.x, Jest and Playwright |
| Storage | PostgreSQL 18. The canonical MVP schema source is `DATA_short.md`; migrations are Flyway schema changes only. |
| Testing | JUnit 5 + Mockito unit tests, Spring Boot/Testcontainers integration tests, Jest component/client tests, Playwright E2E and isolated performance tests |
| Target platform | Spring Boot REST API and modern desktop/mobile browsers |
| Project type | React SPA plus Spring Boot modular monolith |
| Performance goal | With 100,000 published entries and 100 concurrent browser users, each searching every 5 seconds, at least 95% of searches render a correct result or empty state in under 2 seconds during a 10-minute window after a 2-minute warm-up. |
| Search constraints | Query length is 1–120 Unicode code points after trimming; default page size is 20, allowed range 1–50, and pages start at 0. Hanzi and normalized pinyin match contiguous substrings and rank whole-field, prefix, then other substring matches. |
| Privacy constraints | Notes are optional 500-character plaintext values encrypted at rest with the learner key, excluded from search and logs, and retained with the saved-word record until the applicable learner-data deletion lifecycle. |
| Scale / scope | Public lookup over 100,000 published entries; Free learners retain at most 20 active words; Premium learners have no vocabulary-count cap. |

## Constitution Check

| Gate | Status | Evidence |
| --- | --- | --- |
| Fixed architecture and layers | Pass | React consumes contract-bound clients; Spring controllers delegate authorization, transactions, lifecycle and persistence to services and repositories. |
| Server authority | Pass | Spring derives publication state, ownership, plan, capacity, recency and schedule actions from persisted state; React never supplies them as authority. |
| Privacy and access | Pass | Public routes filter `PUBLISHED`; owner-only routes return `404` for absent or unowned records; notes use the existing user-key encryption boundary and are not logged. |
| Data integrity and concurrency | Pass | A user-scoped pessimistic lock serializes capacity transitions; unique ownership constraint prevents duplicate relations; JPA versioning rejects stale note edits. |
| Contract and migration safety | Pass | Public routes remain below `/api/v1` with the standard envelope. `DATA_short.md` records the relevant tables; implementation verifies deployed migration history and consolidates the unmerged schema change before adding Flyway work. |
| Quality and accessibility | Pass | The plan requires JUnit, integration, Jest, Playwright, keyboard/focus/touch behavior, loading, empty, error and retry states. |
| AI boundary | Pass | F09 has no AI request, provider call, `ai-service`, allowance reservation or browser-held provider credential. |

Post-design re-check: all gates remain passed. F03's future Premium lifecycle is consumed only through an internal backend command; F09 neither creates a payment flow nor lets an administrator alter entitlements.

## Design Decisions

### Module boundaries

| Module | Owns | Integration boundary |
| --- | --- | --- |
| `dictionary` | Public published-entry search, detail projection, query normalization and search-key projection | Reads F04-owned dictionary entry and media publication state; never publishes or edits shared content. |
| `vocabulary` | Saved-word identity, private note, owner access, vocabulary recency, Free capacity and capacity-removal result | Calls F10 application commands; asks F03 for the effective plan under the same transaction. |
| `entitlement` (F03) | Effective Free/Premium state and automated Premium-to-Free transition | Invokes the vocabulary capacity command after the entitlement transition; no browser endpoint is added. |
| `review` (F10) | Initial, suspend and restore schedule operations and immutable review history | Exposes application commands only, never lets F09 query or alter review repositories. |
| React dictionary feature | Search/detail/vocabulary presentation, client validation and recoverable UI | Calls only `src/api/dictionary.js`; renders server outcomes and does not calculate plan, capacity, recency or schedule state. |

### Search and publication projection

`dictionary_search_keys` is a derived, indexed projection of F04-authoritative dictionary fields. It stores normalized simplified-Hanzi, traditional-Hanzi and pinyin contiguous query keys plus normalized Vietnamese meaning keywords. Each key records whether it is a whole-field, prefix or other-substring match. Searching uses an exact, bound repository lookup on a normalized key and joins the current `dictionary_entries` row filtered to `PUBLISHED`; it never accepts a client sort expression or returns a stale withdrawn entry.

This dedicated key projection makes one- and two-code-point queries indexable. A trigram-only design is rejected because a pattern with no extractable trigrams can degenerate into a full-index scan, which is unsuitable for the accepted performance target. F04 content changes rebuild keys in the same content transaction. The public read still checks current publication and asset availability, so a delayed or stale projection cannot disclose withdrawn content.

### Saved-word lifecycle and plan capacity

`saved_at` is vocabulary recency: set on the first successful save and each restoration to `ACTIVE`; it is not changed by note updates or duplicate active saves. The oldest/newest tie-breaker is `saved_word_id`, providing a stable order when timestamps match.

All saved-word mutations use this lock order: learner `users` row, effective `user_entitlements` row, target/victim saved words in UUID order, then their F10 schedules in UUID order. The user lock is the capacity serialization gate. It prevents two devices or an entitlement-expiry worker from exceeding the Free limit or removing different words for one slot.

For a new or restored Free word when 20 active records already exist, the service selects the oldest active record by `saved_at ASC, saved_word_id ASC`, marks it `DELETED`, suspends its F10 schedule, and activates the requested record in one transaction. The removal preserves the old record, note and review history. A duplicate active save returns the existing record and changes nothing. Premium has no active-word limit.

F03 owns automated expiry. Its lifecycle service locks the same learner, changes the expired Premium entitlement to its terminal state, establishes the current Free entitlement, then calls the F09 capacity command in the same transaction. The command retains the 20 newest active records and removes older records with the same F10 suspension path. A bounded scheduled reconciliation invokes the same F03 lifecycle command so capacity is applied even when the learner is inactive; the command is also called before a vocabulary mutation to reconcile an already-expired entitlement. No upgrade, billing, or manual entitlement endpoint is part of F09.

### Note conflict and privacy

`SavedWord.version` is returned on each owner-visible representation. A note update supplies `expectedVersion`; the vocabulary service verifies it under the saved-word lock and returns `409 STATE_CONFLICT` for stale input without returning the latest private note. The client reloads the owner resource before another edit. Encryption/decryption is implemented behind a vocabulary-facing service that uses the existing per-user envelope key management; raw notes, ciphertext and decrypted values are excluded from application logs, errors, metrics and search indexes.

### Observability and performance validation

Emit privacy-safe metrics for search duration, result/empty outcome, query-type and length buckets, page size, validation failures, capacity removals, note conflicts and entitlement reconciliation. Correlation IDs and opaque record IDs may be recorded; query text and note content may not. Use a Playwright performance project with 100 browser contexts against a seeded 100,000-entry environment. It measures from search submission through visible results/empty state under the accepted warm-up and measurement profile. Its report records browser version, backend/frontend build identifiers, dataset, network profile, page/query mix and raw percentile summary.

## Project Structure

### Documentation

```text
specs/F09-dictionary-personal-vocabulary/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── performance-report.md
├── contracts/
│   └── f09-openapi.yaml
└── tasks.md                         # generated by speckit-tasks
```

### Source code

```text
backend/src/main/java/net/pchinese/
├── dictionary/
│   ├── api/
│   ├── application/
│   └── persistence/
├── vocabulary/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── persistence/
├── entitlement/application/         # effective-plan and expiry lifecycle command
└── review/application/              # existing F10 application-command boundary

backend/src/test/java/net/pchinese/
├── dictionary/
├── vocabulary/
└── entitlement/

frontend/src/
├── api/dictionary.js
├── features/dictionary/
└── features/vocabulary/

frontend/e2e/
└── dictionary-performance.spec.js
```

**Structure Decision**: use the existing package-by-feature modular monolith. Cross-feature operations go through public application services, never through another module's entity or repository. React components remain feature-local; reusable controls stay in `src/components/` only when they contain no dictionary or vocabulary business state.

## Implementation Phases

1. Verify `DATA_short.md` against the deployed migration history, then add the consolidated dictionary-search-key and saved-word constraints/indexes plus any required entitlement uniqueness support.
2. Build the published-only dictionary projection, normalization, indexed search, detail and safe media projection.
3. Build encrypted owner-only saved-word creation, list/read, deletion, restoration and optimistic note updates.
4. Implement the F03 effective-plan/expiry command and F09 20-word capacity operation, coordinating F10 schedule commands in the documented transaction and lock order.
5. Add contract-bound React search, detail, saved vocabulary, capacity-removal notice and stale-note reload flow with accessible state handling.
6. Add unit, integration, contract, UI, E2E and isolated performance validation; run the quickstart checks and the approved 12-minute performance profile.

## Complexity Tracking

No constitution exception is required.
