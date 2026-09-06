---
description: "Actionable implementation tasks for F02 Profile and Session Control"
---

# Tasks: F02 Profile and Session Control

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f02-openapi.yaml and quickstart.md.

**Tests**: Required by the constitution for ownership, stale-write and session-revocation behaviour.

## Phase 1: Setup

- [ ] T001 Verify F01 identity/session modules and add the F02 module package layout in backend/src/main/java/net/pchinese/profile/ and backend/src/main/java/net/pchinese/sessioncontrol/.
- [ ] T002 [P] Add Settings and device-management route shells in frontend/src/features/settings/SettingsPage.tsx and frontend/src/routes/settingsRoutes.tsx.

## Phase 2: Foundational

- [ ] T003 Add profile-preference columns and any approved session-flow persistence only if required by DATA_short.md in backend/src/main/resources/db/migration/V002__f02_profile_session_control.sql.
- [ ] T004 [P] Extend the authenticated user projection and version-aware user repository queries in backend/src/main/java/net/pchinese/users/persistence/UserRepository.java.
- [ ] T005 [P] Add restricted-flow credential validation that cannot create an ordinary principal in backend/src/main/java/net/pchinese/sessioncontrol/application/SessionManagementFlowService.java.

## Phase 3: User Story 1 - Manage personal learning preferences (Priority: P1) MVP

**Goal**: Let a learner read and update only approved profile preferences using optimistic versioning.

**Independent Test**: A learner saves valid preferences, receives a conflict after a concurrent edit and cannot access another learner profile.

- [ ] T006 [P] [US1] Add canonical current-user profile validation, ownership and stale-version integration tests in backend/src/test/java/net/pchinese/profile/CurrentUserControllerIT.java.
- [ ] T007 [P] [US1] Add ProfileService unit tests for approved fields, unsupported fields and no-overwrite conflict behaviour in backend/src/test/java/net/pchinese/profile/ProfileServiceTest.java.
- [ ] T008 [US1] Implement versioned own-profile read/update transaction and safe DTO projection in backend/src/main/java/net/pchinese/profile/application/ProfileService.java.
- [ ] T009 [US1] Implement the canonical GET and PATCH /me CurrentUserProjection (profile plus safe entitlement fragment), DTOs and controller validation in backend/src/main/java/net/pchinese/profile/api/CurrentUserController.java.
- [ ] T010 [US1] Implement typed profile client, preferences form, reload-on-conflict and accessible validation UI in frontend/src/api/profile.ts and frontend/src/features/settings/ProfilePreferencesForm.tsx.

## Phase 4: User Story 2 - Control active devices (Priority: P1)

**Goal**: Let a learner safely list/revoke owned sessions, including the transient device-limit flow.

**Independent Test**: A learner revokes one owned device or all devices, while unowned/current-session and closed-flow rules remain enforced.

- [ ] T011 [P] [US2] Add integration tests for owned single/all session revoke, current-session confirmation and flow expiry on reload in backend/src/test/java/net/pchinese/sessioncontrol/SessionControlControllerIT.java.
- [ ] T012 [US2] Implement own-session list/revoke composition over F01 session lifecycle and transient device-limit flow transactions in backend/src/main/java/net/pchinese/sessioncontrol/application/SessionControlService.java.
- [ ] T013 [US2] Implement standard and restricted-flow session endpoints in backend/src/main/java/net/pchinese/sessioncontrol/api/SessionControlController.java.
- [ ] T014 [US2] Implement typed session-control client, device list, revoke confirmations and memory-only restricted-flow screen in frontend/src/api/sessionControl.ts and frontend/src/features/settings/SessionManagementPanel.tsx.
- [ ] T015 [US2] Add Jest tests for current-session logout, cancel handling and reload-clears-flow behaviour in frontend/src/features/settings/SessionManagementPanel.test.tsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T016 [P] Add F02 E2E profile conflict and device-limit management journeys in frontend/e2e/f02-profile-session-control.spec.ts.
- [ ] T017 Run F02 quickstart validation and backend/frontend test suites recorded in specs/F02-profile-session-control/quickstart.md.

## Dependencies and Execution Order

- F02 requires completed F01 authentication, active-session and user-row foundations.
- Phase 2 blocks both stories.
- US1 and US2 can proceed in parallel after Phase 2 because they use separate modules.

## Parallel Opportunities

- T002, T004 and T005 are independent after T001.
- T006/T007 and the frontend T010 can be developed alongside the profile service contract.
- T011 can run in parallel with the client work after T012 defines the endpoint projection.

## Implementation Strategy

Ship profile preferences first, then add self-service device control and the restricted device-limit flow without widening authentication authority.
