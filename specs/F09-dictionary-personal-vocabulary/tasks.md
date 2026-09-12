# Tasks: F09 Dictionary and Personal Vocabulary

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/f09-openapi.yaml](contracts/f09-openapi.yaml), and [quickstart.md](quickstart.md)

**Prerequisites**: The F03 entitlement lifecycle and F10 schedule-command boundaries are part of this plan. F09 must not be released until their internal commands are implemented and tested together.

**Tests**: Required. The project constitution and feature acceptance criteria require JUnit 5 + Mockito, Spring Boot integration tests, Jest tests, and Playwright tests where stated below.

**Organization**: Tasks are grouped by user story. Phases 1–2 establish the shared schema, locks and cross-feature contracts. Both user stories are P1, but US1 can be delivered first after its dictionary foundation is complete; US2 adds private vocabulary and plan capacity.

## Format: `[ID] [P?] [Story?] Description`

- **[P]** marks tasks that use different files and can proceed concurrently once their stated dependency is available.
- **[US1]** and **[US2]** map directly to the stories in [spec.md](spec.md).

## Phase 1: Setup and migration safety

**Purpose**: Establish an approved, reproducible implementation baseline before code or schema changes.

- [X] T001 Inspect `DATA_short.md`, deployed Flyway history, and `supabase/migrations/` before reserving `supabase/migrations/20260911210000_f09_dictionary_vocabulary.sql`; do not modify an applied migration or create a second unmerged F09 migration.
- [X] T002 [P] Create deterministic published, withdrawn, unavailable-media, Free, Premium, and reviewed-word fixtures in `backend/src/test/java/net/pchinese/support/DictionaryVocabularyFixtureFactory.java`.
- [X] T003 [P] Add an isolated `performance` Playwright project and environment guard in `frontend/playwright.config.js` so the 100-user run cannot target a shared development database.

---

## Phase 2: Foundational contracts and persistence

**Purpose**: Build the schema, persistence, encryption and internal F03/F10 boundaries that block feature work.

**⚠️ CRITICAL**: Complete this phase before combining dictionary search or vocabulary mutations with real data.

- [X] T004 Create `supabase/migrations/20260911210000_f09_dictionary_vocabulary.sql` with `dictionary_search_keys`; its rows must be unique by `(dictionary_entry_id, query_kind, normalized_key)` and indexed beginning with `(query_kind, normalized_key, match_rank, dictionary_entry_id)`. Add the saved-word unique `(user_id, dictionary_entry_id)` constraint, the `(user_id, status, saved_at, saved_word_id)` capacity index, and reconciled partial uniqueness for the single active `user_entitlements` row.
- [X] T005 [P] Implement `DictionaryEntryEntity`, `DictionarySearchKeyEntity`, `DictionaryEntryRepository`, and `DictionarySearchKeyRepository` in `backend/src/main/java/net/pchinese/dictionary/persistence/`, including only `PUBLISHED` search projections and deterministic whole-field/prefix/substring rank ordering.
- [X] T006 [P] Implement `SavedWordEntity` and locked owner/capacity queries in `backend/src/main/java/net/pchinese/vocabulary/persistence/`; map “`personal_note_ciphertext` nullable encrypted plain text, maximum 500 characters”, `ACTIVE`/`DELETED`, `saved_at`, `deleted_at`, and optimistic `version` exactly as defined in `data-model.md`.
- [X] T007 [P] Add locked effective-plan resolution and idempotent Premium-expiry reconciliation in `backend/src/main/java/net/pchinese/entitlement/application/EffectiveEntitlementService.java`, `backend/src/main/java/net/pchinese/entitlement/application/EntitlementLifecycleService.java`, and `backend/src/main/java/net/pchinese/entitlement/persistence/UserEntitlementRepository.java`; F03 must establish Free state before requesting F09 capacity enforcement.
- [X] T008 [P] Define cross-feature application commands in `backend/src/main/java/net/pchinese/review/application/SrsScheduleCommands.java` and `backend/src/main/java/net/pchinese/vocabulary/application/VocabularyCapacityCommands.java` for initial schedule, suspend, restore, and Free-capacity enforcement; do not expose HTTP calls or repository access across modules.
- [X] T009 [P] Implement a vocabulary-specific note cipher facade in `backend/src/main/java/net/pchinese/vocabulary/application/PersonalNoteCipherService.java` and its user-key adapter in `backend/src/main/java/net/pchinese/security/crypto/UserDataCipherService.java`; encrypt before persistence and prohibit note plaintext/ciphertext in logs, errors, metrics and search indexes.
- [X] T010 Add clean-migration, uniqueness, key-projection, and active-entitlement constraint coverage in `backend/src/test/java/net/pchinese/vocabulary/F09SchemaMigrationIT.java`.

**Checkpoint**: F09 has one migration path, typed domain boundaries, safe persistence mappings, and a testable concurrency model.

---

## Phase 3: User Story 1 — Discover dictionary entries (Priority: P1) 🎯 MVP

**Goal**: A visitor can find and safely open only published Chinese dictionary entries using Hanzi, normalized pinyin, or Vietnamese keywords.

**Independent Test**: A visitor finds published summaries with each supported query form, receives a bounded empty state when appropriate, and cannot discover or open withdrawn content.

### Tests for User Story 1

- [X] T011 [P] [US1] Add public search/detail integration and contract tests in `backend/src/test/java/net/pchinese/dictionary/DictionaryControllerIT.java` covering simplified/traditional Hanzi, tone/case/space-normalized pinyin, Vietnamese keywords, whole/prefix/interior ranking, PUBLISHED-only results, withdrawal `404`, unavailable media omission, HSK 1–6, and the 1–120-code-point/page 0–50 validation boundaries.
- [X] T012 [P] [US1] Add query normalizer and generated-key ranking unit tests in `backend/src/test/java/net/pchinese/dictionary/DictionarySearchKeyProjectorTest.java`, including 1-code-point queries and stable dictionary-entry-ID ordering ties.
- [X] T013 [P] [US1] Add API-client and accessible UI-state tests in `frontend/src/api/dictionary.test.js` and `frontend/src/features/dictionary/DictionaryPage.test.jsx` for loading, bounded results, empty state, validation error, withdrawn detail, keyboard search, visible focus, and a 44×44px touch target.

### Implementation for User Story 1

- [X] T014 [US1] Implement normalization, contiguous-key generation, F04 publication-state projection refresh, and privacy-safe search metrics in `backend/src/main/java/net/pchinese/dictionary/application/DictionarySearchKeyProjector.java` and `backend/src/main/java/net/pchinese/dictionary/application/DictionarySearchProjectionService.java`; rebuild affected keys on F04 dictionary content or publication changes.
- [X] T015 [US1] Implement bound indexed search, server-owned rank/page validation, published-only detail projection, and available-media filtering in `backend/src/main/java/net/pchinese/dictionary/application/DictionarySearchService.java` and `backend/src/main/java/net/pchinese/dictionary/application/DictionaryEntryService.java`.
- [X] T016 [US1] Implement `GET /api/v1/dictionary` and `GET /api/v1/dictionary/{entryId}` with validated DTOs and standard envelopes in `backend/src/main/java/net/pchinese/dictionary/api/DictionaryController.java` and `backend/src/main/java/net/pchinese/dictionary/api/DictionaryDtos.java`, conforming to `specs/F09-dictionary-personal-vocabulary/contracts/f09-openapi.yaml`.
- [X] T017 [US1] Implement the contract-bound client in `frontend/src/api/dictionary.js` and the search, detail, media-omission, loading, empty and recoverable-error UI in `frontend/src/features/dictionary/DictionaryPage.jsx` and `frontend/src/features/dictionary/DictionaryEntryPanel.jsx` using semantic design tokens only.
- [X] T018 [US1] Add visitor journey coverage in `frontend/e2e/dictionary-discovery.spec.js` for normalized search, ranking, pagination, empty state, withdrawn detail, keyboard navigation and responsive controls.

**Checkpoint**: User Story 1 can be demonstrated without authentication or a personal-vocabulary implementation.

---

## Phase 4: User Story 2 — Build a private vocabulary list (Priority: P1)

**Goal**: A learner can save, restore, annotate and remove only their own words, while the backend preserves F10 history and enforces Free/Premium capacity correctly.

**Independent Test**: An authenticated learner saves one word, updates a note, deletes and restores the same record/schedule; an unowned request, stale note edit, full Free list, Premium list, and Premium expiry all produce the specified server-owned result.

### Tests for User Story 2

- [X] T019 [P] [US2] Add owner, duplicate, restore, withdrawal, note and stale-version integration tests in `backend/src/test/java/net/pchinese/vocabulary/SavedWordControllerIT.java`; verify “optional plain text of at most 500 characters”, formatted or attachment-shaped input rejection, owner-only `404` including ADMIN, `409 STATE_CONFLICT`, record-ID preservation, and no withdrawn dictionary detail.
- [X] T020 [P] [US2] Add transaction and concurrency integration tests in `backend/src/test/java/net/pchinese/vocabulary/VocabularyCapacityIT.java` for 20 active Free words, active unavailable words counting toward capacity, oldest `(saved_at, saved_word_id)` removal, restoration recency, duplicate active save, failed-save rollback, concurrent saves, Premium unlimited storage, Premium-to-Free retention of the 20 newest, and F10 schedule/history suspension/restoration.
- [X] T021 [P] [US2] Add service unit tests in `backend/src/test/java/net/pchinese/vocabulary/SavedWordServiceTest.java` and `backend/src/test/java/net/pchinese/entitlement/EntitlementLifecycleServiceTest.java` for lock-order decisions, effective plan reconciliation, optimistic note conflicts, encryption boundary, and expiry-command idempotency.
- [X] T022 [P] [US2] Add contract-bound vocabulary client and component tests in `frontend/src/api/dictionary.test.js`, `frontend/src/features/vocabulary/VocabularyPage.test.jsx`, and `frontend/src/features/vocabulary/SavedWordNoteEditor.test.jsx` for owner data, capacity display, automatic-removal notice, unavailable item, note clear, stale-note reload, loading/empty/error/retry states, and keyboard access.

### Implementation for User Story 2

- [X] T023 [US2] Implement save, owner read/list, delete, restore, note update and `expectedVersion` conflict handling in `backend/src/main/java/net/pchinese/vocabulary/application/SavedWordService.java` and `backend/src/main/java/net/pchinese/vocabulary/application/SavedWordCommands.java`; a restore must retain its note, update `saved_at`, and create no duplicate saved word or F10 schedule.
- [X] T024 [US2] Implement the shared user → entitlement → saved-word → F10 schedule lock order and capacity state transitions in `backend/src/main/java/net/pchinese/vocabulary/application/VocabularyCapacityService.java`, `backend/src/main/java/net/pchinese/entitlement/application/EntitlementLifecycleService.java`, and `backend/src/main/java/net/pchinese/review/application/SavedWordScheduleService.java`; Free replaces only the oldest active word, while Premium expiry retains only the 20 newest.
- [X] T025 [US2] Implement authenticated `POST/GET /api/v1/saved-words`, `GET/PATCH/DELETE /api/v1/saved-words/{savedWordId}`, validation, owner-safe `404`, and `409` error mapping in `backend/src/main/java/net/pchinese/vocabulary/api/SavedWordController.java`, `backend/src/main/java/net/pchinese/vocabulary/api/SavedWordDtos.java`, and `backend/src/main/java/net/pchinese/common/error/ApiExceptionHandler.java` according to `specs/F09-dictionary-personal-vocabulary/contracts/f09-openapi.yaml`.
- [X] T026 [US2] Extend `frontend/src/api/dictionary.js` with saved-word calls and implement saved vocabulary, note editor, automatic-capacity notice, unavailable state, stale-note reload and server-authoritative Free/Premium display in `frontend/src/features/vocabulary/VocabularyPage.jsx` and `frontend/src/features/vocabulary/SavedWordNoteEditor.jsx`.
- [X] T027 [US2] Add end-to-end learner coverage in `frontend/e2e/personal-vocabulary.spec.js` for save/delete/restore, two-device stale note conflict, capacity replacement, unavailable saved word, and owner isolation.

**Checkpoint**: User Story 2 works without exposing private data, resetting learning history, allowing an ADMIN bypass, or relying on client-side plan/capacity logic.

---

## Phase 5: Polish and cross-cutting validation

**Purpose**: Prove the complete feature meets performance, observability, security and delivery requirements.

- [X] T028 [P] Add privacy-safe timers/counters for search latency, query-type/length buckets, page size, result/empty outcome, validation failure, capacity removal, stale-note conflict and entitlement reconciliation in `backend/src/main/java/net/pchinese/dictionary/application/DictionarySearchMetrics.java` and `backend/src/main/java/net/pchinese/vocabulary/application/VocabularyMetrics.java`; never record raw query text, personal notes or ciphertext.
- [X] T029 [P] Implement the deterministic 100,000-entry seed and 100-browser-context performance journey in `backend/src/test/java/net/pchinese/dictionary/DictionaryPerformanceFixtureSeeder.java` and `frontend/e2e/dictionary-performance.spec.js`; run a 2-minute warm-up then 10-minute, one-search-per-5-seconds profile and fail below 95% correct visible outcomes under 2 seconds.
- [X] T030 Run the backend, frontend and feature validation commands recorded in `specs/F09-dictionary-personal-vocabulary/quickstart.md`; fix resulting test, lint, build, envelope, migration and accessibility failures before marking F09 complete.
- [X] T031 Record the reproducible performance report with backend/frontend build IDs, browser version, dataset version, network profile, query/page-size mix and percentile results in `specs/F09-dictionary-personal-vocabulary/performance-report.md`.

---

## Dependencies and execution order

### Phase dependencies

- **Phase 1** can start immediately.
- **Phase 2** depends on T001. T004 must complete before integration tests use the new schema. T005–T009 may proceed in parallel after T004's schema shape is agreed; T010 validates the completed foundation.
- **US1** depends on T004–T005. Its tests T011–T013 should be written before T014–T018.
- **US2** depends on T004 and T006–T009. It also requires the F10 schedule-command implementation used by T024. Its tests T019–T022 should be written before T023–T027.
- **Phase 5** depends on the desired US1 and US2 work. T029 requires both the search UI and the performance project from T003.

### User story dependencies

- **US1 — Discover dictionary entries**: independently releasable after T004–T005; it has no authentication or capacity dependency.
- **US2 — Build a private vocabulary list**: depends on foundational F03/F10 boundaries and the shared dictionary entry mapping, but its owner flows remain independently testable with seeded entries.

### Parallel opportunities

- T002 and T003 can run alongside migration-history inspection.
- T005–T009 use distinct modules/files and can proceed in parallel after the migration contract is settled.
- T011–T013 can be authored in parallel; T019–T022 can be authored in parallel.
- T028 and T029 can proceed independently after their implementation prerequisites complete.

## Parallel examples

### User Story 1

```text
Task: "T011 public search/detail integration tests in backend/src/test/java/net/pchinese/dictionary/DictionaryControllerIT.java"
Task: "T012 ranking unit tests in backend/src/test/java/net/pchinese/dictionary/DictionarySearchKeyProjectorTest.java"
Task: "T013 client/UI tests in frontend/src/api/dictionary.test.js and frontend/src/features/dictionary/DictionaryPage.test.jsx"
```

### User Story 2

```text
Task: "T019 saved-word owner and note integration tests in backend/src/test/java/net/pchinese/vocabulary/SavedWordControllerIT.java"
Task: "T020 capacity, expiry and F10 transaction tests in backend/src/test/java/net/pchinese/vocabulary/VocabularyCapacityIT.java"
Task: "T021 lifecycle and encryption unit tests in backend/src/test/java/net/pchinese/vocabulary/SavedWordServiceTest.java"
Task: "T022 vocabulary UI tests in frontend/src/features/vocabulary/VocabularyPage.test.jsx"
```

## Implementation strategy

### MVP first

1. Complete T001–T010.
2. Complete US1 through T018 and validate public search/detail independently.
3. Demonstrate US1 with published, empty, withdrawn and unavailable-media cases.

### Incremental delivery

1. Deliver US1 after its checkpoint.
2. Add US2 with F03/F10 coordination and validate its checkpoint.
3. Complete performance and release validation in Phase 5.
