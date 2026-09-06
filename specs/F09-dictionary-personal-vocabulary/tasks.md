---
description: "Actionable implementation tasks for F09 Dictionary and Personal Vocabulary"
---

# Tasks: F09 Dictionary and Personal Vocabulary

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f09-openapi.yaml and quickstart.md.

**Tests**: Required for published-only search, owner notes, withdrawal safety and F10 schedule preservation.

## Phase 1: Setup

- [ ] T001 Create dictionary and personal-vocabulary module packages in backend/src/main/java/net/pchinese/dictionary/ and backend/src/main/java/net/pchinese/vocabulary/.
- [ ] T002 [P] Create dictionary search/detail and saved-word screen shells in frontend/src/features/dictionary/.

## Phase 2: Foundational

- [ ] T003 Add normalized dictionary search fields/indexes and saved_words lifecycle/note columns in backend/src/main/resources/db/migration/V009__f09_dictionary_vocabulary.sql.
- [ ] T004 [P] Implement dictionary and saved-word entities/repositories with one user-entry relation in backend/src/main/java/net/pchinese/dictionary/persistence/ and backend/src/main/java/net/pchinese/vocabulary/persistence/.
- [ ] T005 Implement encryption/decryption boundary for private notes with no note search/logging in backend/src/main/java/net/pchinese/vocabulary/application/PersonalNoteCipherService.java.

## Phase 3: User Story 1 - Discover dictionary entries (Priority: P1) MVP

**Goal**: Let visitors search only PUBLISHED entries by Hanzi, normalized pinyin or Vietnamese keyword.

**Independent Test**: Search returns only intended published entries and safe available assets; a withdrawn entry returns no source content.

- [ ] T006 [P] [US1] Add Simplified, Traditional, normalized pinyin, Vietnamese keyword and published-only integration tests in backend/src/test/java/net/pchinese/dictionary/DictionaryControllerIT.java.
- [ ] T007 [P] [US1] Add normalized lookup/ranking repository tests in backend/src/test/java/net/pchinese/dictionary/DictionarySearchRepositoryTest.java.
- [ ] T008 [US1] Implement normalized published-entry search/detail and available-asset projection in backend/src/main/java/net/pchinese/dictionary/application/DictionaryService.java.
- [ ] T009 [US1] Implement public dictionary search/detail controllers with bounded pagination in backend/src/main/java/net/pchinese/dictionary/api/DictionaryController.java.
- [ ] T010 [US1] Implement typed dictionary client, search forms, detail view and accessible empty/unavailable states in frontend/src/api/dictionary.ts and frontend/src/features/dictionary/DictionaryPage.tsx.

## Phase 4: User Story 2 - Build a private vocabulary list (Priority: P1)

**Goal**: Let a learner save, update, delete and restore one private vocabulary relation without losing F10 review state.

**Independent Test**: A learner saves one word/note, deletes it, restores the same identity/note/schedule; other learners and withdrawn details remain inaccessible.

- [ ] T011 [P] [US2] Add saved-word ownership, note limit, withdrawal, delete/restore and no-duplicate integration tests in backend/src/test/java/net/pchinese/vocabulary/SavedWordControllerIT.java.
- [ ] T012 [P] [US2] Add transaction tests for F10 ensure/suspend/restore commands in backend/src/test/java/net/pchinese/vocabulary/SavedWordServiceTest.java.
- [ ] T013 [US2] Implement one-per-entry save, encrypted note update, delete and restore service with F10 command calls in backend/src/main/java/net/pchinese/vocabulary/application/SavedWordService.java.
- [ ] T014 [US2] Implement owner-only saved-word list/create/update/delete endpoints in backend/src/main/java/net/pchinese/vocabulary/api/SavedWordController.java.
- [ ] T015 [US2] Implement typed saved-word client, private note editor and due-list entry UI in frontend/src/api/savedWords.ts and frontend/src/features/dictionary/SavedWordsPanel.tsx.
- [ ] T016 [US2] Add Jest coverage for note validation, unavailable source projection and restore UI in frontend/src/features/dictionary/SavedWordsPanel.test.tsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T017 [P] Add public dictionary and private vocabulary lifecycle E2E coverage in frontend/e2e/f09-dictionary-vocabulary.spec.ts.
- [ ] T018 Run F09 quickstart and clean-migration/backend/frontend test suites using specs/F09-dictionary-personal-vocabulary/quickstart.md.

## Dependencies and Execution Order

- F09 requires F01 identity and F04 published dictionary content.
- F09 US2 requires the F10 internal ensure/suspend/restore schedule command before its endpoint is released.
- Phase 2 blocks both stories; US1 is independent of F10.

## Parallel Opportunities

- T002, T004 and T005 can proceed after T001.
- T006 and T007 are independent search test suites.
- T011 and T012 can run in parallel before T013.

## Implementation Strategy

Ship public PUBLISHED dictionary search/detail first. Release private saved-word mutations only with the completed F10 state-preservation transaction boundary.

