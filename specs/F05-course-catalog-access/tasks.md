---
description: "Actionable implementation tasks for F05 Course Catalog and Access"
---

# Tasks: F05 Course Catalog and Access

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f05-openapi.yaml and quickstart.md.

**Tests**: Required for public publication filtering, safe projections and changed-content access.

## Phase 1: Setup

- [ ] T001 Create catalog module packages in backend/src/main/java/net/pchinese/catalog/ and public catalog screens in frontend/src/features/catalog/.
- [ ] T002 [P] Add visitor and signed-in catalog routes in frontend/src/routes/catalogRoutes.tsx.

## Phase 2: Foundational

- [ ] T003 Add indexed published-content query support approved by DATA_short.md in backend/src/main/resources/db/migration/V005__f05_catalog_indexes.sql.
- [ ] T004 [P] Implement published-Free query specifications and safe summary projections in backend/src/main/java/net/pchinese/catalog/persistence/CatalogRepository.java.
- [ ] T005 [P] Implement public-cache-safe response configuration without media/transcript data in backend/src/main/java/net/pchinese/catalog/api/CatalogResponsePolicy.java.

## Phase 3: User Story 1 - Discover published learning content (Priority: P1) MVP

**Goal**: Let visitors and learners search only current published Free topics and lesson cards.

**Independent Test**: A visitor finds matching published summaries in admin sort order; draft, retired, unclassified-filtered and private fields never appear.

- [ ] T006 [P] [US1] Add catalog search/filter/publication integration tests in backend/src/test/java/net/pchinese/catalog/CatalogControllerIT.java.
- [ ] T007 [P] [US1] Add repository tests for title, HSK, unclassified and sort-order queries in backend/src/test/java/net/pchinese/catalog/CatalogRepositoryTest.java.
- [ ] T008 [US1] Implement published-Free topic/lesson search and summary service in backend/src/main/java/net/pchinese/catalog/application/CatalogService.java.
- [ ] T009 [US1] Implement public GET topics and lessons endpoints with bounded pagination in backend/src/main/java/net/pchinese/catalog/api/CatalogController.java.
- [ ] T010 [US1] Implement typed catalog client, search/filter controls, cards and accessible empty/error states in frontend/src/api/catalog.ts and frontend/src/features/catalog/CatalogPage.tsx.

## Phase 4: User Story 2 - Enter a permitted lesson (Priority: P1)

**Goal**: Show safe lesson detail and hand a signed-in learner to F06 without exposing playback or practice to visitors.

**Independent Test**: A visitor sees only a permitted lesson summary and sign-in path; a changed/unavailable lesson returns safely to the catalog with existing progress retained.

- [ ] T011 [P] [US2] Add lesson-detail publication-change and visitor-access integration tests in backend/src/test/java/net/pchinese/catalog/LessonDetailControllerIT.java.
- [ ] T012 [US2] Implement current-state lesson-detail access decision in backend/src/main/java/net/pchinese/catalog/application/LessonAccessService.java.
- [ ] T013 [US2] Implement safe lesson detail endpoint and unavailable response in backend/src/main/java/net/pchinese/catalog/api/LessonDetailController.java.
- [ ] T014 [US2] Implement lesson-detail sign-in handoff, unavailable return and no-playback visitor UI in frontend/src/features/catalog/LessonDetailPage.tsx.
- [ ] T015 [US2] Add Jest coverage for summary-only visitor detail and recoverable unavailable state in frontend/src/features/catalog/LessonDetailPage.test.tsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T016 [P] Add public catalog and changed-content E2E coverage in frontend/e2e/f05-catalog.spec.ts.
- [ ] T017 Run F05 quickstart validation and backend/frontend test suites using specs/F05-course-catalog-access/quickstart.md.

## Dependencies and Execution Order

- F05 requires F04 published content/media lifecycle and the F03 Free-only policy.
- Phase 2 blocks both stories.
- US2 depends on US1 safe catalog projections and becomes F06's entry point.

## Parallel Opportunities

- T002, T004 and T005 are independent after T001.
- T006 and T007 can run in parallel before T008.
- T011 and frontend T014 can proceed once the detail contract is fixed.

## Implementation Strategy

Release public discovery first, then add safe lesson-detail handoff. Do not add playback, practice, Premium prompts or private learner data to this feature.

