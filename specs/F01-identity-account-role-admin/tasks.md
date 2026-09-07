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

**Goal**: Deliver exact-ID-only ADMIN role and account-access commands with immutable safe audit evidence.

**Independent Test**: A controlled ADMIN can manage an eligible exact-ID target, while self/final-ADMIN guards and private-data restrictions are enforced.

- [X] T016 [P] [US2] Add integration tests for exact-ID lookup, grant/revoke, lock/unlock, self-action and final-ADMIN concurrency guards in backend/src/test/java/net/pchinese/users/AccountRoleControllerIT.java.
- [X] T017 [P] Add service unit tests for role-history, authz-version, session invalidation and OTHER-note validation in backend/src/test/java/net/pchinese/users/AccountRoleServiceTest.java.
- [X] T018 [US2] Implement transactional ADMIN role/access commands and append-only safe audit writes in backend/src/main/java/net/pchinese/users/application/AccountRoleService.java.
- [X] T019 [US2] Implement exact-ID role/access DTOs and protected command routes in backend/src/main/java/net/pchinese/users/api/AccountRoleController.java.
- [x] T020 [US2] Implement the contract-bound exact-ID admin client, Account/Roles tab and accessible confirmation/conflict UI in frontend/src/api/adminUsers.js and frontend/src/features/admin/AccountRolesTab.jsx.
- [x] T021 [US2] Add Jest coverage that Admin UI has no directory/private-learning-data path in frontend/src/features/admin/AccountRolesTab.test.jsx.

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
- [ ] T033 Validate exact account UUIDs in the Admin client and map malformed path identifiers to the specified safe validation/not-found envelope instead of INTERNAL_ERROR per FR-007 and edge: invalid account identifier (partial)
- [ ] T034 Make simultaneous duplicate registration requests converge on the same neutral 202 outcome without a uniqueness race or account-existence signal, and add concurrency coverage per edge: duplicate registration (partial)
- [ ] T035 At final project stabilization, execute the PostgreSQL-backed backend acceptance suite for logout/reset, locked and unauthorized actors, CSRF/origin, audit durability, session invalidation, final-ADMIN concurrency and the applicable schema verification per plan: backend test sequence (partial)
- [ ] T036 Install and configure runnable Playwright E2E support and cover all five F01 quickstart journeys, including verification, refresh/reuse, logout, recovery, Admin mutations, conflicts and audit-relevant outcomes per plan: E2E coverage (partial)
- [ ] T037 Add guest, authenticated and ADMIN-aware frontend route guards and redirect to sign-in after failed refresh or revoked access without treating client state as authorization authority per plan: frontend flows and UI states (partial)
- [ ] T038 Strengthen OTHER-note validation so credentials, identifiers and learner-private content cannot enter audit details, and cover every standard reason plus unsafe-note rejection per FR-008 and SC-006 (partial)
- [ ] T039 Implement and test accessible error-summary focus, live status announcements, semantic modal behavior, focus trapping, Escape/cancel and trigger-focus restoration for F01 auth and Admin flows per plan: accessible frontend UX (partial)
- [ ] T040 Add contract-aligned throttling for enumeration-sensitive public registration, verification and recovery requests, return the documented safe 429 envelope, and map it to neutral frontend UX per plan: OpenAPI 429 contract (missing)
