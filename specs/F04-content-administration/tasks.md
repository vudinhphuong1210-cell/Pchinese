---
description: "Actionable implementation tasks for F04 Content Administration"
---

# Tasks: F04 Content Administration

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f04-openapi.yaml and quickstart.md.

**Tests**: Required for ADMIN authorization, lifecycle validation, optimistic concurrency and media safety.

## Phase 1: Setup

- [x] T001 Create content-administration module packages in backend/src/main/java/net/pchinese/content/ and backend/src/main/java/net/pchinese/media/.
- [x] T002 [P] Create the protected content-administration route and page shell in frontend/src/features/admin/content/ContentAdminPage.jsx and frontend/src/routes/adminContentRoutes.jsx.

## Phase 2: Foundational

- [x] T003 Add approved topics, lessons, segments, media_assets and append-only content_audit_events migrations in backend/src/main/resources/db/migration/V004__f04_content_administration.sql.
- [x] T004 [P] Implement JPA entities, publication-state enums and Spring Data repositories in backend/src/main/java/net/pchinese/content/persistence/ and backend/src/main/java/net/pchinese/media/persistence/.
- [x] T005 [P] Implement ADMIN-only endpoint policy and expectedVersion conflict mapping in backend/src/main/java/net/pchinese/content/api/ContentAdminExceptionHandler.java.
- [x] T006 Implement append-only safe content audit writing in backend/src/main/java/net/pchinese/content/application/ContentAuditService.java.

## Phase 3: User Story 1 - Prepare learning content (Priority: P1) MVP

**Goal**: Let ADMIN create, edit and inspect draft topics, Free lessons, ordered segments and controlled media metadata.

**Independent Test**: An ADMIN can prepare valid draft content; a non-ADMIN, stale editor or invalid parent/order/media reference cannot mutate it.

- [x] T007 [P] [US1] Add draft CRUD, ADMIN authorization and stale expectedVersion integration tests in backend/src/test/java/net/pchinese/content/ContentAdminControllerIT.java.
- [x] T008 [P] [US1] Add unit tests for topic/lesson/segment parent and unique-order validation in backend/src/test/java/net/pchinese/content/ContentDraftServiceTest.java.
- [x] T009 [US1] Implement versioned topic, Free-lesson and segment draft services in backend/src/main/java/net/pchinese/content/application/ContentDraftService.java.
- [x] T010 [US1] Implement controlled media upload/allowlisted metadata intake and scan-state service in backend/src/main/java/net/pchinese/media/application/MediaAssetService.java.
- [x] T011 [US1] Implement validated ADMIN draft/list/create/update DTOs and controllers in backend/src/main/java/net/pchinese/content/api/ContentAdminController.java and backend/src/main/java/net/pchinese/media/api/MediaAdminController.java.
- [x] T012 [US1] Implement contract-bound admin content client and accessible draft editors in frontend/src/api/adminContent.js and frontend/src/features/admin/content/ContentEditor.jsx.

## Phase 4: User Story 2 - Publish and retire safe content (Priority: P1)

**Goal**: Enforce content/media lifecycle commands and current publication prerequisites without exposing unsafe content.

**Independent Test**: An ADMIN can publish only a valid content chain; stale/unapproved/retired state is rejected and audited.

- [x] T013 [P] [US2] Add lifecycle, publish-prerequisite, unpublish/archive and media-quarantine integration tests in backend/src/test/java/net/pchinese/content/ContentPublicationControllerIT.java.
- [x] T014 [P] [US2] Add state-machine unit tests for terminal archive and media-dependent lesson withdrawal in backend/src/test/java/net/pchinese/content/ContentPublicationServiceTest.java.
- [x] T015 [US2] Implement topic/lesson/segment publication, unpublication and archive transitions in backend/src/main/java/net/pchinese/content/application/ContentPublicationService.java.
- [x] T016 [US2] Implement media approve/reject/quarantine transitions and dependent-content invalidation in backend/src/main/java/net/pchinese/media/application/MediaLifecycleService.java.
- [x] T017 [US2] Implement the explicit publish, unpublish and archive endpoints for topics/lessons/segments plus approve, reject and quarantine endpoints for media, with recoverable conflict projections, in backend/src/main/java/net/pchinese/content/api/ContentLifecycleController.java.
- [x] T018 [US2] Implement accessible publish prerequisites, confirmation dialogs and stale-state reload UI in frontend/src/features/admin/content/PublicationControls.jsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [x] T019 [P] Add ADMIN content lifecycle E2E coverage in frontend/e2e/f04-content-administration.spec.js.
- [x] T020 Run clean Flyway migration, F04 quickstart and backend/frontend test suites using specs/F04-content-administration/quickstart.md.

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

## Phase 6: Convergence

- [x] T021 CRITICAL Add the approved `content_audit_events` schema to `DATA_short.md` and the canonical `supabase/migrations` Flyway flow, enforce append-only storage there, and extend clean-migration coverage per Constitution IV and plan: migration safety (contradicts).
- [x] T022 CRITICAL Replace the F04 `localStorage` access-token fallback with the in-memory authenticated API-client/session mechanism per Constitution III (contradicts).
- [x] T023 CRITICAL Replace direct F04 theme colour utilities with semantic design tokens while preserving accessible modal and button contrast per Constitution V (contradicts).
- [x] T024 CRITICAL Add the missing F04 Admin content E2E spec and complete backend integration/unit coverage for authorization, validation/error envelopes, optimistic conflicts, terminal archive, media invalidation, and audit events per Constitution V and T019 (missing).
- [x] T025 Require lesson publication to have one or more segments and verify every associated segment is `PUBLISHED` with `APPROVED` media before publishing per FR-004 and SC-003/SC-005 (contradicts).
- [x] T026 Reject learner-visible segment/order/media changes while their lesson is `PUBLISHED`, and preserve valid parent/child lifecycle state for unpublish/archive operations per FR-003 and SC-007 (contradicts).
- [x] T027 Enforce the specified media approval transition matrix in service and UI, disallowing re-approval or other invalid transitions of rejected/quarantined assets per data-model: media lifecycle (partial).
- [x] T028 Implement a controlled backend media intake adapter with provider allowlisting, file type/size validation, persisted scan state, and scan-before-approval semantics per plan: controlled media adapter (missing).
- [x] T029 Align F04 controller request/response DTOs, create status codes, media multipart intake, pagination/meta, and frontend client with `contracts/f04-openapi.yaml` instead of exposing JPA entities per plan: contract-bound API (partial).
- [x] T030 Record safe before/after lifecycle state and request correlation in content audits, and prevent update/delete paths for audit events per plan: audit decision (partial).

## Phase 7: Post-Implementation Validation

- [ ] T031 Run the canonical clean-and-upgrade migration checks and the updated F04 backend, frontend, and E2E validation sequence in `quickstart.md`; resolve any failures before review per SC-010 through SC-014 (missing).

## Phase 8: YouTube-only Media Intake Revision

**Goal**: Let an ADMIN register a supported YouTube learning video by pasting its URL or video ID,
without a local file upload or video download, while retaining the existing review/lifecycle safety.

**Independent Test**: An ADMIN can submit a valid watch, share, embed link, or bare video ID; each is
stored as the same canonical ID and enters review. Invalid links and any file submission are rejected
without state change.

- [x] T032 [P] [US1] Update `DATA_short.md` and add one new canonical forward migration under `supabase/migrations/` that constrains new F04 media assets to unique `YOUTUBE` `VIDEO` canonical IDs without altering applied migrations.
- [x] T033 [P] [US1] Add unit tests for accepted YouTube ID/watch/share/embed inputs and rejected HTTP, non-YouTube, playlist/channel/short, malformed, duplicate, and file inputs in `backend/src/test/java/net/pchinese/media/MediaIntakePolicyTest.java`.
- [x] T034 [US1] Replace local-upload/multi-provider intake with backend YouTube URL/ID parsing, canonicalization, and immutable-source enforcement in `backend/src/main/java/net/pchinese/media/application/MediaIntakePolicy.java`, `MediaAssetService.java`, and related configuration.
- [x] T035 [P] [US1] Add ADMIN controller integration coverage for JSON YouTube media creation, safe response projection, immutable source update rejection, and local multipart/file rejection in `backend/src/test/java/net/pchinese/content/ContentAdminControllerIT.java`.
- [x] T036 [US1] Align the media DTOs and controllers with JSON `youtubeVideoReference` creation and display-only updates in `backend/src/main/java/net/pchinese/content/api/ContentDtos.java` and `backend/src/main/java/net/pchinese/media/api/MediaAdminController.java`.
- [x] T037 [US1] Replace multipart/file submission with an accessible “YouTube URL or Video ID” field, client-side guidance, and safe server-error recovery in `frontend/src/api/adminContent.js` and `frontend/src/features/admin/content/ContentEditor.jsx`.
- [x] T038 [P] [US1] Update frontend unit and F04 E2E tests for URL/ID normalization feedback and absence of local file upload in `frontend/src/features/admin/content/ContentAdminPage.test.jsx` and `frontend/e2e/f04-content-administration.spec.js`.
- [x] T039 [US1] Run the updated F04 backend, frontend, and E2E validation sequence from `specs/F04-content-administration/quickstart.md`, excluding Docker checks while they remain deferred.
