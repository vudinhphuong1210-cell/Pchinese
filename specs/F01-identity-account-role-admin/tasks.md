---
description: "Actionable implementation tasks for F01 Identity, Account, and Role Administration"
---

# Tasks: F01 Identity, Account, and Role Administration

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f01-openapi.yaml, quickstart.md and the project constitution.

**Tests**: Required by the constitution. Write contract/integration coverage before the corresponding implementation.

## Phase 1: Setup

- [ ] T001 Create the Java 21 Spring Boot Maven baseline in backend/pom.xml and backend/src/main/java/net/pchinese/PchineseApplication.java.
- [ ] T002 [P] Create the React 18 strict-TypeScript Vite baseline in frontend/package.json, frontend/tsconfig.json and frontend/src/main.tsx.
- [ ] T003 [P] Configure backend test, Flyway and PostgreSQL test profiles in backend/src/test/resources/application-test.yml.

## Phase 2: Foundational

- [ ] T004 Create the F01 Flyway schema for users, user_roles, auth_sessions, refresh_tokens, auth_action_tokens, refresh_idempotency and auth_audit_events in backend/src/main/resources/db/migration/V001__f01_identity.sql.
- [ ] T005 [P] Implement the standard success/data/error/meta envelope, correlation ID filter and stable exception mapping in backend/src/main/java/net/pchinese/common/api/ApiEnvelope.java and backend/src/main/java/net/pchinese/common/error/ApiExceptionHandler.java.
- [ ] T006 [P] Implement JWT validation, authenticated principal resolution, CSRF/origin checks and ADMIN authorization policy in backend/src/main/java/net/pchinese/security/SecurityConfiguration.java.
- [ ] T007 Implement F01 JPA entities and Spring Data repositories in backend/src/main/java/net/pchinese/auth/persistence/ and backend/src/main/java/net/pchinese/users/persistence/.
- [ ] T008 [P] Implement the typed API envelope, error mapping and memory-only access-session store in frontend/src/api/http.ts and frontend/src/features/auth/authSessionStore.ts.

## Phase 3: User Story 1 - Create and secure an account (Priority: P1) MVP

**Goal**: Deliver neutral registration, verification, login, refresh, logout, password reset and owned session lifecycle.

**Independent Test**: A learner registers without account enumeration, verifies, signs in, rotates/revokes sessions and resets credentials without exposing raw credentials.

- [ ] T009 [P] [US1] Add auth contract and integration tests for neutral registration, verification, login, refresh replay/reuse, reset and session ownership in backend/src/test/java/net/pchinese/auth/AuthControllerIT.java.
- [ ] T010 [P] [US1] Add unit tests for bcrypt credentials, action-token one-use expiry and refresh-family rotation in backend/src/test/java/net/pchinese/auth/AuthLifecycleServiceTest.java.
- [ ] T011 [US1] Implement registration, email verification and password-reset services with neutral responses and hashed single-use action tokens in backend/src/main/java/net/pchinese/auth/application/AccountLifecycleService.java.
- [ ] T012 [US1] Implement login, opaque refresh rotation/replay, logout and owned session revocation transactions in backend/src/main/java/net/pchinese/auth/application/SessionLifecycleService.java.
- [ ] T013 [US1] Implement validated AuthController DTOs for every F01 auth/session OpenAPI path in backend/src/main/java/net/pchinese/auth/api/AuthController.java.
- [ ] T014 [US1] Implement typed auth/session clients and accessible register, verify, login, reset and session-management screens in frontend/src/api/auth.ts and frontend/src/features/auth/.
- [ ] T015 [US1] Add Jest coverage for form errors, refresh single-flight and no browser credential storage in frontend/src/features/auth/authFlows.test.tsx.

## Phase 4: User Story 2 - Administer roles and account access (Priority: P1)

**Goal**: Deliver exact-ID-only ADMIN role and account-access commands with immutable safe audit evidence.

**Independent Test**: A controlled ADMIN can manage an eligible exact-ID target, while self/final-ADMIN guards and private-data restrictions are enforced.

- [ ] T016 [P] [US2] Add integration tests for exact-ID lookup, grant/revoke, lock/unlock, self-action and final-ADMIN concurrency guards in backend/src/test/java/net/pchinese/users/AccountRoleControllerIT.java.
- [ ] T017 [P] [US2] Add service unit tests for role-history, authz-version, session invalidation and OTHER-note validation in backend/src/test/java/net/pchinese/users/AccountRoleServiceTest.java.
- [ ] T018 [US2] Implement transactional ADMIN role/access commands and append-only safe audit writes in backend/src/main/java/net/pchinese/users/application/AccountRoleService.java.
- [ ] T019 [US2] Implement exact-ID role/access DTOs and protected command routes in backend/src/main/java/net/pchinese/users/api/AccountRoleController.java.
- [ ] T020 [US2] Implement the typed exact-ID admin client, Account/Roles tab and accessible confirmation/conflict UI in frontend/src/api/adminUsers.ts and frontend/src/features/admin/AccountRolesTab.tsx.
- [ ] T021 [US2] Add Jest coverage that Admin UI has no directory/private-learning-data path in frontend/src/features/admin/AccountRolesTab.test.tsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T022 [P] Add end-to-end identity, refresh, session and ADMIN journeys with stable selectors in frontend/e2e/f01-identity.spec.ts.
- [ ] T023 Synchronize the neutral auth.register contract in API.md, then run the F01 quickstart scenarios and document any remaining contract corrections in specs/F01-identity-account-role-admin/quickstart.md.
- [ ] T024 Run backend and frontend lint, test and build commands from backend/pom.xml and frontend/package.json, fixing all F01 failures.

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
