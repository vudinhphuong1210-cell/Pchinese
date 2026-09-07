---
description: "Actionable implementation tasks for F04 Content Administration"
---

# Tasks: F04 Content Administration

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f04-openapi.yaml and quickstart.md.

**Tests**: Required for ADMIN authorization, lifecycle validation, optimistic concurrency and media safety.

## Phase 1: Setup

- [ ] T001 Create content-administration module packages in backend/src/main/java/net/pchinese/content/ and backend/src/main/java/net/pchinese/media/.
- [ ] T002 [P] Create the protected content-administration route and page shell in frontend/src/features/admin/content/ContentAdminPage.jsx and frontend/src/routes/adminContentRoutes.jsx.

## Phase 2: Foundational

- [ ] T003 Add approved topics, lessons, segments, media_assets and append-only content_audit_events migrations in backend/src/main/resources/db/migration/V004__f04_content_administration.sql.
- [ ] T004 [P] Implement JPA entities, publication-state enums and Spring Data repositories in backend/src/main/java/net/pchinese/content/persistence/ and backend/src/main/java/net/pchinese/media/persistence/.
- [ ] T005 [P] Implement ADMIN-only endpoint policy and expectedVersion conflict mapping in backend/src/main/java/net/pchinese/content/api/ContentAdminExceptionHandler.java.
- [ ] T006 Implement append-only safe content audit writing in backend/src/main/java/net/pchinese/content/application/ContentAuditService.java.

## Phase 3: User Story 1 - Prepare learning content (Priority: P1) MVP

**Goal**: Let ADMIN create, edit and inspect draft topics, Free lessons, ordered segments and controlled media metadata.

**Independent Test**: An ADMIN can prepare valid draft content; a non-ADMIN, stale editor or invalid parent/order/media reference cannot mutate it.

- [ ] T007 [P] [US1] Add draft CRUD, ADMIN authorization and stale expectedVersion integration tests in backend/src/test/java/net/pchinese/content/ContentAdminControllerIT.java.
- [ ] T008 [P] [US1] Add unit tests for topic/lesson/segment parent and unique-order validation in backend/src/test/java/net/pchinese/content/ContentDraftServiceTest.java.
- [ ] T009 [US1] Implement versioned topic, Free-lesson and segment draft services in backend/src/main/java/net/pchinese/content/application/ContentDraftService.java.
- [ ] T010 [US1] Implement controlled media upload/allowlisted metadata intake and scan-state service in backend/src/main/java/net/pchinese/media/application/MediaAssetService.java.
- [ ] T011 [US1] Implement validated ADMIN draft/list/create/update DTOs and controllers in backend/src/main/java/net/pchinese/content/api/ContentAdminController.java and backend/src/main/java/net/pchinese/media/api/MediaAdminController.java.
- [ ] T012 [US1] Implement contract-bound admin content client and accessible draft editors in frontend/src/api/adminContent.js and frontend/src/features/admin/content/ContentEditor.jsx.

## Phase 4: User Story 2 - Publish and retire safe content (Priority: P1)

**Goal**: Enforce content/media lifecycle commands and current publication prerequisites without exposing unsafe content.

**Independent Test**: An ADMIN can publish only a valid content chain; stale/unapproved/retired state is rejected and audited.

- [ ] T013 [P] [US2] Add lifecycle, publish-prerequisite, unpublish/archive and media-quarantine integration tests in backend/src/test/java/net/pchinese/content/ContentPublicationControllerIT.java.
- [ ] T014 [P] [US2] Add state-machine unit tests for terminal archive and media-dependent lesson withdrawal in backend/src/test/java/net/pchinese/content/ContentPublicationServiceTest.java.
- [ ] T015 [US2] Implement topic/lesson/segment publication, unpublication and archive transitions in backend/src/main/java/net/pchinese/content/application/ContentPublicationService.java.
- [ ] T016 [US2] Implement media approve/reject/quarantine transitions and dependent-content invalidation in backend/src/main/java/net/pchinese/media/application/MediaLifecycleService.java.
- [ ] T017 [US2] Implement the explicit publish, unpublish and archive endpoints for topics/lessons/segments plus approve, reject and quarantine endpoints for media, with recoverable conflict projections, in backend/src/main/java/net/pchinese/content/api/ContentLifecycleController.java.
- [ ] T018 [US2] Implement accessible publish prerequisites, confirmation dialogs and stale-state reload UI in frontend/src/features/admin/content/PublicationControls.jsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T019 [P] Add ADMIN content lifecycle E2E coverage in frontend/e2e/f04-content-administration.spec.js.
- [ ] T020 Run clean Flyway migration, F04 quickstart and backend/frontend test suites using specs/F04-content-administration/quickstart.md.

## Dependencies and Execution Order

- F04 requires F01 ADMIN authorization and audit/correlation foundations.
- Phase 2 blocks both stories.
- US2 depends on the draft entities/services from US1, especially segment/media validation.
- F05 consumes only the published safe projections produced by F04.

## Parallel Opportunities

- T002, T004 and T005 can run in parallel after T001.
- T007/T008 and T013/T014 are parallel test tasks.
- Frontend editor T012 can proceed after the draft DTO contract is stable.

## Implementation Strategy

Deliver draft preparation and safe media intake first. Add lifecycle enforcement only after the publication prerequisites and conflict contract are covered by integration tests.
