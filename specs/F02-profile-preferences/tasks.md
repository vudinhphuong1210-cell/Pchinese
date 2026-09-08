---
description: "Actionable implementation tasks for F02 Profile Preferences"
---

# Tasks: F02 Profile Preferences

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f02-openapi.yaml and quickstart.md.

**Tests**: Required by the constitution for ownership and stale-write behaviour.

## Phase 1: Setup

- [X] T001 Verify F01 identity integration and add the F02 profile module package layout in backend/src/main/java/net/pchinese/profile/.
- [X] T002 [P] Add the Profile Settings route shell in frontend/src/features/settings/SettingsPage.jsx and frontend/src/routes/settingsRoutes.jsx.

## Phase 2: Foundational

- [X] T003 Verify the profile-preference columns already exist in the canonical Supabase migration from `DATA_short.md`; no duplicate backend migration is created.
- [X] T004 [P] Extend the authenticated user projection and version-aware user repository queries in backend/src/main/java/net/pchinese/users/persistence/UserRepository.java.

## Phase 3: User Story 1 - Manage personal learning preferences (Priority: P1) MVP

**Goal**: Let a learner read and update only approved profile preferences using optimistic versioning.

**Independent Test**: A learner saves valid preferences, receives a conflict after a concurrent edit and cannot access another learner profile.

- [X] T005 [P] [US1] Add canonical current-user profile validation, ownership, ADMIN-prohibition and stale-version integration tests in backend/src/test/java/net/pchinese/profile/CurrentUserControllerIT.java.
- [X] T006 [P] [US1] Add ProfileService unit tests for approved fields, unsupported fields and no-overwrite conflict behaviour in backend/src/main/java/net/pchinese/profile/ProfileServiceTest.java.
- [X] T007 [US1] Implement versioned own-profile read/update transaction and safe DTO projection in backend/src/main/java/net/pchinese/profile/application/ProfileService.java.
- [X] T008 [US1] Implement the canonical F02 GET and PATCH /me profile projection, DTOs and controller validation in backend/src/main/java/net/pchinese/profile/api/CurrentUserController.java. F03 later contributes the entitlement fragment.
- [X] T009 [US1] Implement contract-bound profile client, preferences form, reload-on-conflict and accessible validation UI in frontend/src/api/profile.js and frontend/src/features/settings/ProfilePreferencesForm.jsx.

## Phase 4: Polish and Cross-Cutting Concerns

- [X] T010 [P] Add F02 E2E profile ownership and conflict journeys in frontend/e2e/f02-profile-preferences.spec.js.
- [X] T011 Run F02 quickstart validation and backend/frontend test suites recorded in specs/F02-profile-preferences/quickstart.md.

> Validation record (2026-09-07): `mvn verify`, frontend Jest, ESLint and production build passed. The
> Testcontainers integration suite was invoked by Maven but skipped because Docker is unavailable in this environment.

## Phase 5: User Story 2 - Review system activity history (Priority: P2)

**Goal**: Let an ADMIN review a paginated, minimized system activity history while learners have no
audit-history access or UI.

**Independent Test**: A learner profile change creates one safe system event visible to ADMIN; a
learner receives `403` and has no activity navigation entry.

- [X] T012 [P] [US2] Synchronize F02 specification, plan, audit-event data model, OpenAPI contract, API registry and quickstart for ADMIN-only system activity history in specs/F02-profile-preferences/ and API.md.
- [X] T013 [P] [US2] Add failing backend unit/integration coverage for ADMIN authorization, cross-account pagination, account-name minimization and learner-forbidden behaviour in backend/src/test/java/net/pchinese/auth/ and backend/src/test/java/net/pchinese/users/.
- [X] T014 [US2] Replace the owner activity reader with an ADMIN-authorized safe system audit projection and `GET /api/v1/admin/audit-events` controller through backend/src/main/java/net/pchinese/auth/ and backend/src/main/java/net/pchinese/users/.
- [X] T015 [US2] Remove learner activity API/UI/navigation and implement the ADMIN-only system activity client, sidebar entry and accessible loading/empty/error/pagination UI in frontend/src/api/, frontend/src/components/, frontend/src/features/admin/ and frontend/src/App.jsx.
- [X] T016 [US2] Run F02 backend/frontend validation, verify no migration is introduced, and record corrected-system-history results in specs/F02-profile-preferences/quickstart.md.

## Phase 6: User Story 2 - Structured audit-event organization (Priority: P2)

**Goal**: Record F02 and existing account/security events with a centralized, privacy-minimized
taxonomy while retaining the same safe ADMIN history projection.

**Independent Test**: A profile update and an account/security operation each produce an immutable
event with an approved category/type and bounded outcome/reason/changed-field codes. Neither the
stored safe details nor the ADMIN response contains a prohibited raw value or free-text note.

- [X] T017 [P] [US2] Add failing audit-taxonomy and redaction unit tests for approved categories, bounded codes, changed-field allowlists and prohibited detail values in backend/src/test/java/net/pchinese/auth/application/AuthAuditServiceTest.java.
- [X] T018 [P] [US2] Add API/UI regression tests proving `/api/v1/admin/audit-events` still exposes only type, time and account-name labels in backend/src/test/java/net/pchinese/users/AccountRoleServiceTest.java and frontend/src/api/adminAuditEvents.test.js.
- [X] T019 [US2] Create the centrally controlled event-type/category and bounded-code policy in backend/src/main/java/net/pchinese/auth/application/AuditEventTaxonomy.java without changing `DATA_short.md` or adding a migration.
- [X] T020 [US2] Apply the taxonomy and safe-detail allowlist to all F02 profile and account/security audit writers in backend/src/main/java/net/pchinese/auth/application/AuthAuditService.java, backend/src/main/java/net/pchinese/profile/application/ProfileService.java and backend/src/main/java/net/pchinese/users/application/AccountRoleService.java.
- [X] T021 [US2] Preserve the ADMIN-safe projection and private incident-trace boundary in backend/src/main/java/net/pchinese/users/application/AccountRoleService.java, backend/src/main/java/net/pchinese/users/api/AdminAuditEventController.java and frontend/src/features/admin/SystemActivityTab.jsx.
- [X] T022 [US2] Run the F02 backend/frontend audit privacy and contract validations described in specs/F02-profile-preferences/quickstart.md and record the result there.

## Dependencies and Execution Order

- F02 requires completed F01 authentication and user-row foundations.
- Phase 2 blocks the profile-preferences story.
- User Story 2 depends on the completed F02 owner-profile foundation, F01 server-derived ADMIN role and reuses the existing audit store.
- The structured-audit work starts with T017/T018; T019 blocks T020, and T021/T022 follow the
  updated writers. It does not require a schema migration.

## Parallel Opportunities

- T002 and T004 are independent after T001.
- T005/T006 and the frontend T009 can be developed alongside the profile service contract.
- T012/T013 can proceed in parallel; T014 precedes T015 and T016.
- T017 and T018 can proceed in parallel; T020 follows T019, while T021 can be prepared alongside
  T020 but must validate against its final behavior.

## Implementation Strategy

Ship profile preferences without widening authentication authority.
