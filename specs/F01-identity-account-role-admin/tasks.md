---
description: "Actionable implementation tasks for F01 Identity, Account, and Role Administration"
---

# Tasks: F01 Identity, Account, and Role Administration

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f01-openapi.yaml, quickstart.md and the project constitution.

**Tests**: Required by the constitution. Write contract/integration coverage before the corresponding implementation.

## Phase 1: Setup

- [X] T001 Create the Java 21 Spring Boot Maven baseline in backend/pom.xml and backend/src/main/java/net/pchinese/PchineseApplication.java.
- [x] T002 [P] Create the React 18 JavaScript/JSX Vite baseline in frontend/package.json, frontend/vite.config.js, frontend/eslint.config.js and frontend/src/main.jsx.
- [X] T003 [P] Configure backend test, Flyway and PostgreSQL test profiles in backend/src/test/resources/application-test.yml.

## Phase 2: Foundational

- [X] T004 Create the F01 Flyway schema for users, user_roles, auth_sessions, refresh_tokens, auth_action_tokens, refresh_idempotency and auth_audit_events in backend/src/main/resources/db/migration/V001__f01_identity.sql.
- [X] T005 [P] Implement the standard success/data/error/meta envelope, correlation ID filter and stable exception mapping in backend/src/main/java/net/pchinese/common/api/ApiEnvelope.java and backend/src/main/java/net/pchinese/common/error/ApiExceptionHandler.java.
- [X] T006 [P] Implement JWT validation, authenticated principal resolution, CSRF/origin checks and ADMIN authorization policy in backend/src/main/java/net/pchinese/security/SecurityConfiguration.java.
- [X] T007 Implement F01 JPA entities and Spring Data repositories in backend/src/main/java/net/pchinese/auth/persistence/ and backend/src/main/java/net/pchinese/users/persistence/.
- [x] T008 [P] Implement the standard API envelope, error mapping and memory-only access-session store in frontend/src/api/http.js and frontend/src/features/auth/authSessionStore.js.

## Phase 3: User Story 1 - Create and secure an account (Priority: P1) MVP

**Goal**: Deliver neutral registration, verification, login, refresh, logout, password reset and a secure authentication lifecycle.

**Independent Test**: A learner registers without account enumeration, verifies, signs in, refreshes, signs out and resets credentials without exposing raw credentials.

- [X] T009 [P] [US1] Add auth contract and integration tests for neutral registration, verification, login, refresh replay/reuse, logout and reset in backend/src/test/java/net/pchinese/auth/AuthControllerIT.java.
- [X] T010 [P] [US1] Add unit tests for bcrypt credentials, action-token one-use expiry and refresh-family rotation in backend/src/test/java/net/pchinese/auth/AuthLifecycleServiceTest.java.
- [X] T011 [US1] Implement registration, email verification and password-reset services with neutral responses and hashed single-use action tokens in backend/src/main/java/net/pchinese/auth/application/AccountLifecycleService.java.
- [X] T012 [US1] Implement login, opaque refresh rotation/replay and current-session logout transactions in backend/src/main/java/net/pchinese/auth/application/SessionLifecycleService.java.
- [X] T013 [US1] Implement validated AuthController DTOs for every F01 auth OpenAPI path in backend/src/main/java/net/pchinese/auth/api/AuthController.java.
- [x] T014 [US1] Implement contract-bound auth client and accessible register, verify, login, reset and sign-out flows in frontend/src/api/auth.js and frontend/src/features/auth/.
- [x] T015 [US1] Add Jest coverage for form errors, refresh single-flight and no browser credential storage in frontend/src/features/auth/authFlows.test.jsx.

## Phase 4: User Story 2 - Administer roles and account access (Priority: P1)

**Goal**: Deliver a safe paginated User Management directory plus ADMIN role and account-access
commands with immutable safe audit evidence.

**Independent Test**: A controlled ADMIN can select an eligible target from a safe user directory,
then manage role/access while self/final-ADMIN guards and private-data restrictions are enforced.

- [X] T016 [P] [US2] Add integration tests for the safe directory, selected-user projection, grant/revoke, lock/unlock, self-action and final-ADMIN concurrency guards in backend/src/test/java/net/pchinese/users/AccountRoleControllerIT.java.
- [X] T017 [P] Add service unit tests for role-history, authz-version, session invalidation and OTHER-note validation in backend/src/test/java/net/pchinese/users/AccountRoleServiceTest.java.
- [X] T018 [US2] Implement transactional ADMIN role/access commands and append-only safe audit writes in backend/src/main/java/net/pchinese/users/application/AccountRoleService.java.
- [X] T019 [US2] Implement protected selected-user role/access DTOs and command routes in backend/src/main/java/net/pchinese/users/api/AccountRoleController.java.
- [x] T020 [US2] Implement the contract-bound admin client, User Management tab and accessible confirmation/conflict UI in frontend/src/api/adminUsers.js and frontend/src/features/admin/AccountRolesTab.jsx.
- [x] T021 [US2] Add Jest coverage for safe User Management UI with no private learner-data path in frontend/src/features/admin/AccountRolesTab.test.jsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [x] T022 [P] Add end-to-end identity, refresh and ADMIN journeys with stable selectors in frontend/e2e/f01-identity.spec.js.
- [X] T023 Synchronize the neutral auth.register contract in API.md, then run the F01 quickstart scenarios and document any remaining contract corrections in specs/F01-identity-account-role-admin/quickstart.md.
- [X] T024 Add a clean-PostgreSQL Flyway migration integration test that starts from an empty database, applies every F01 migration, and verifies the required schema constraints in backend/src/test/java/net/pchinese/MigrationIT.java.
- [X] T025 Run the clean-database migration test plus backend and frontend lint, test and build commands from backend/pom.xml and frontend/package.json, fixing all F01 failures.

## Dependencies and Execution Order

- Phase 1 precedes Phase 2.
- Phase 2 blocks both user stories.
- US1 and US2 can start after Phase 2; US2 requires the authenticated ADMIN policy from T006.
- F02, F03, F04, F06, F07, F08, F09, F10 and F11 depend on completed F01 identity/security foundations.

## Parallel Opportunities

- T002 and T003 can run alongside T001.
- T005, T006 and T008 can run in parallel after the schema task starts.
- T009 and T010, then T016 and T017, are parallel test tasks in separate files.

## Implementation Strategy

Deliver US1 first as the deployable security MVP. Add US2 only after the protected identity/session lifecycle has passed integration and E2E checks.

## Phase 6: Convergence

- [ ] T026 CRITICAL: Remove plaintext/default database and SMTP credentials from backend/src/main/resources/application.yml, require deployment-managed secrets, and rotate every exposed credential per Constitution III (contradicts)
- [ ] T028 CRITICAL: Make refresh-token reuse revocation and its high-severity audit commit durably even when the API returns REFRESH_TOKEN_INVALID, with database assertions for family/session revocation per Constitution III and plan: refresh replay/reuse decision (contradicts)
- [ ] T029 Serialize final-active-ADMIN checks across concurrent revoke and lock commands so no interleaving can remove or disable every active administrator per FR-005 and US2/AC2 (partial)
- [ ] T030 Implement and document a deployment-controlled, idempotent initial ADMIN bootstrap that cannot be reached through public registration per plan: controlled bootstrap ADMIN (missing)
- [ ] T031 Persist immutable actor, target, correlation and outcome audit evidence for every accepted or rejected role/lock/unlock attempt, including authorization, validation, unknown-target, self and final-ADMIN failures, without rolling the rejection audit back per SC-002 and plan: audit decision (partial)
- [ ] T032 Revoke the authenticated current server session on logout even when its refresh cookie is missing, expired or already invalid, while keeping the response safe and clearing browser credentials per FR-001 and US1/AC2 (partial)
- [X] T033 Retire manually entered exact account UUIDs from the Admin client; selected command targets originate only from the safe directory response, so malformed user-supplied path identifiers have no UI path per FR-007.
- [ ] T034 Make simultaneous duplicate registration requests converge on the same neutral 202 outcome without a uniqueness race or account-existence signal, and add concurrency coverage per edge: duplicate registration (partial)
- [ ] T035 At final project stabilization, execute the PostgreSQL-backed backend acceptance suite for logout/reset, locked and unauthorized actors, CSRF/origin, audit durability, session invalidation, final-ADMIN concurrency and the applicable schema verification per plan: backend test sequence (partial)
- [ ] T036 Install and configure runnable Playwright E2E support and cover all five F01 quickstart journeys, including verification, refresh/reuse, logout, recovery, Admin mutations, conflicts and audit-relevant outcomes per plan: E2E coverage (partial)
- [ ] T037 Add guest, authenticated and ADMIN-aware frontend route guards and redirect to sign-in after failed refresh or revoked access without treating client state as authorization authority per plan: frontend flows and UI states (partial)
- [ ] T038 Strengthen OTHER-note validation so credentials, identifiers and learner-private content cannot enter audit details, and cover every standard reason plus unsafe-note rejection per FR-008 and SC-006 (partial)
- [ ] T039 Implement and test accessible error-summary focus, live status announcements, semantic modal behavior, focus trapping, Escape/cancel and trigger-focus restoration for F01 auth and Admin flows per plan: accessible frontend UX (partial)
- [ ] T040 Add contract-aligned throttling for enumeration-sensitive public registration, verification and recovery requests, return the documented safe 429 envelope, and map it to neutral frontend UX per plan: OpenAPI 429 contract (missing)

## Phase 7: User Management revision — 2026-09-08

**Goal**: Replace the Exact-ID entry UX with a server-paginated, safe User Management list while
retaining the protected commands for a selected account.

**Independent Test**: An ADMIN sees UUID, lifecycle state and ADMIN role only, selects a user and
can issue the existing protected commands; unauthenticated and non-ADMIN callers cannot list users.

- [X] T041 [P] [US2] Synchronize the F01 OpenAPI contract, API registry and quickstart for `GET /users` safe pagination in specs/F01-identity-account-role-admin/contracts/f01-openapi.yaml, API.md and specs/F01-identity-account-role-admin/quickstart.md.
- [X] T042 [P] [US2] Add unit and integration coverage for Admin-only safe pagination, validation and no-private-field responses in backend/src/test/java/net/pchinese/users/.
- [X] T043 [US2] Add a Spring Data projection and transactional service/controller directory route in backend/src/main/java/net/pchinese/users/persistence/UserRepository.java, backend/src/main/java/net/pchinese/users/application/AccountRoleService.java and backend/src/main/java/net/pchinese/users/api/UserDirectoryController.java.
- [X] T044 [P] [US2] Add the paginated user-directory client method in frontend/src/api/adminUsers.js.
- [X] T045 [US2] Replace the Exact-ID Admin UI with the accessible “Quản lý người dùng” list and selected-user command panel in frontend/src/features/admin/AccountRolesTab.jsx and frontend/src/components/Sidebar.jsx.
- [X] T046 [US2] Update Jest coverage, run backend tests plus frontend lint, test and build, and record validation in specs/F01-identity-account-role-admin/quickstart.md.

## Phase 8: Account name in User Management — 2026-09-08

**Goal**: Show the owner-set account name rather than a UUID as the user label in the directory and
selected-user dialog, while preserving UUID as a secondary reference and keeping email/profile data private.

**Independent Test**: An ADMIN receives the selected account's `accountName` in the directory and
detail projection; the UI displays that name, never falls back to UUID or email, and uses “Chưa đặt tên” only when the owner has not set one.

- [X] T047 [P] [US2] Synchronize F01 specification, plan, projection model, OpenAPI contract, API registry and quickstart for permitted display-only `accountName` in specs/F01-identity-account-role-admin/, API.md.
- [X] T048 [P] [US2] Add backend unit/integration assertions for ADMIN-only accountName decryption, neutral missing-name fallback and no email/other-profile disclosure in backend/src/test/java/net/pchinese/users/.
- [X] T049 [US2] Extend the JPA projection and AccountRoleService response DTOs with server-derived accountName from `display_name_ciphertext` in backend/src/main/java/net/pchinese/users/.
- [X] T050 [US2] Render only the contract `accountName` (or “Chưa đặt tên”) as the account label and add Jest coverage in frontend/src/features/admin/AccountRolesTab.jsx and frontend/src/features/admin/AccountRolesTab.test.jsx.
- [X] T051 [US2] Run backend tests plus frontend lint, test and build; record the validation result in specs/F01-identity-account-role-admin/quickstart.md.

## Phase 9: Concurrent browser accounts — 2026-09-09

**Goal**: Allow separate browser tabs to keep different verified accounts signed in without moving
access or refresh credentials into browser storage.

**Independent Test**: Two tabs use different account sessions through refresh and reload; logging
out one tab leaves the other session valid.

- [X] T052 [P] [US1] Synchronize F01 specification, plan, session model, OpenAPI contract, API registry and quickstart for per-tab browser-session routing in specs/F01-identity-account-role-admin/ and API.md.
- [X] T053 [P] [US1] Add backend integration coverage for two named browser cookie pairs, per-tab refresh/logout isolation, legacy-cookie migration and invalid selector handling in backend/src/test/java/net/pchinese/auth/AuthControllerIT.java.
- [X] T054 [US1] Route refresh/logout through the validated session selector and matching per-session cookie pair in backend/src/main/java/net/pchinese/auth/ and backend/src/main/java/net/pchinese/security/.
- [X] T055 [US1] Store only the non-credential tab selector, propagate it through the contract-bound auth client, and provide the accessible additional-account tab entry point in frontend/src/features/auth/, frontend/src/api/, frontend/src/components/Sidebar.jsx and frontend/src/App.jsx.
- [X] T056 [US1] Add Jest coverage, run backend validation plus frontend lint, test and build, and record results in specs/F01-identity-account-role-admin/quickstart.md.
