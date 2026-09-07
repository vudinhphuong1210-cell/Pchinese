---
description: "Actionable implementation tasks for F02 Profile Preferences"
---

# Tasks: F02 Profile Preferences

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f02-openapi.yaml and quickstart.md.

**Tests**: Required by the constitution for ownership and stale-write behaviour.

## Phase 1: Setup

- [ ] T001 Verify F01 identity integration and add the F02 profile module package layout in backend/src/main/java/net/pchinese/profile/.
- [ ] T002 [P] Add the Profile Settings route shell in frontend/src/features/settings/SettingsPage.jsx and frontend/src/routes/settingsRoutes.jsx.

## Phase 2: Foundational

- [ ] T003 Add profile-preference columns only if required by DATA_short.md in backend/src/main/resources/db/migration/V002__f02_profile_preferences.sql.
- [ ] T004 [P] Extend the authenticated user projection and version-aware user repository queries in backend/src/main/java/net/pchinese/users/persistence/UserRepository.java.

## Phase 3: User Story 1 - Manage personal learning preferences (Priority: P1) MVP

**Goal**: Let a learner read and update only approved profile preferences using optimistic versioning.

**Independent Test**: A learner saves valid preferences, receives a conflict after a concurrent edit and cannot access another learner profile.

- [ ] T005 [P] [US1] Add canonical current-user profile validation, ownership, ADMIN-prohibition and stale-version integration tests in backend/src/test/java/net/pchinese/profile/CurrentUserControllerIT.java.
- [ ] T006 [P] [US1] Add ProfileService unit tests for approved fields, unsupported fields and no-overwrite conflict behaviour in backend/src/main/java/net/pchinese/profile/ProfileServiceTest.java.
- [ ] T007 [US1] Implement versioned own-profile read/update transaction and safe DTO projection in backend/src/main/java/net/pchinese/profile/application/ProfileService.java.
- [ ] T008 [US1] Implement the canonical GET and PATCH /me CurrentUserProjection (profile plus safe entitlement fragment), DTOs and controller validation in backend/src/main/java/net/pchinese/profile/api/CurrentUserController.java.
- [ ] T009 [US1] Implement contract-bound profile client, preferences form, reload-on-conflict and accessible validation UI in frontend/src/api/profile.js and frontend/src/features/settings/ProfilePreferencesForm.jsx.

## Phase 4: Polish and Cross-Cutting Concerns

- [ ] T010 [P] Add F02 E2E profile ownership and conflict journeys in frontend/e2e/f02-profile-preferences.spec.js.
- [ ] T011 Run F02 quickstart validation and backend/frontend test suites recorded in specs/F02-profile-preferences/quickstart.md.

## Dependencies and Execution Order

- F02 requires completed F01 authentication and user-row foundations.
- Phase 2 blocks the profile-preferences story.

## Parallel Opportunities

- T002 and T004 are independent after T001.
- T005/T006 and the frontend T009 can be developed alongside the profile service contract.

## Implementation Strategy

Ship profile preferences without widening authentication authority.
