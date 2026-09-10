---
description: "Actionable implementation tasks for F03 Plans, Entitlement, and AI Allowance"
---

# Tasks: F03 Plans, Entitlement, and AI Allowance

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f03-openapi.yaml and quickstart.md.

**Tests**: Required for entitlement uniqueness, quota races, idempotency and exact-once refund.

## Phase 1: Setup

- [x] T001 Create the entitlement and allowance package layout in backend/src/main/java/net/pchinese/entitlement/ and backend/src/main/java/net/pchinese/allowance/.
- [ ] T002 [P] Add the learner account allowance-summary placeholder to frontend/src/features/settings/AllowanceSummaryCard.jsx.

## Phase 2: Foundational

- [x] T003 Verify subscription_plans, user_entitlements and ai_usage_events schema in DATA_short.md and initial migration (no redundant migration file needed).
- [x] T004 [P] Implement JPA entities and repositories with active-entitlement locking in backend/src/main/java/net/pchinese/entitlement/persistence/ and backend/src/main/java/net/pchinese/allowance/persistence/.
- [x] T005 Implement automatic Free-entitlement provisioning from F01 activation in backend/src/main/java/net/pchinese/entitlement/application/EntitlementProvisioningService.java.

## Phase 3: User Story 1 - Receive and understand Free access (Priority: P1) MVP

**Goal**: Automatically provide one current Free entitlement and expose only the learner's safe summary.

**Independent Test**: An active learner receives one Free entitlement and sees only their 30-unit rolling-cycle summary.

- [x] T006 [P] [US1] Add entitlement-provisioning and canonical current-user safe-entitlement integration tests in backend/src/test/java/net/pchinese/profile/CurrentUserEntitlementIT.java.
- [x] T007 [US1] Implement rolling-cycle calculation and safe entitlement summary projection in backend/src/main/java/net/pchinese/entitlement/application/EntitlementService.java.
- [x] T008 [US1] After F02 T009, extend its canonical current-user projection with the safe F03 entitlement summary in backend/src/main/java/net/pchinese/profile/api/CurrentUserController.java.
- [ ] T009 [US1] Implement contract-bound allowance-summary client and learner card states in frontend/src/api/entitlement.js and frontend/src/features/settings/AllowanceSummaryCard.jsx.

## Phase 4: User Story 2 - Enforce AI allowance fairly (Priority: P1)

**Goal**: Give F08/F11 an internal Spring Boot-only reserve/reuse/succeed/refund service with exact-once outcomes.

**Independent Test**: Identical AI requests charge once, exhausted requests stop before AI work and post-reservation failures refund once.

- [x] T010 [P] [US2] Add transaction and concurrency tests for quota exhaustion, reused fingerprint, changed fingerprint and one refund in backend/src/test/java/net/pchinese/allowance/AiAllowanceServiceIT.java.
- [x] T011 [P] [US2] Add unit tests for usage-event state transitions and 30-day rollovers in backend/src/test/java/net/pchinese/allowance/AiAllowanceServiceTest.java.
- [x] T012 [US2] Implement reserveOrReuse, succeed and refundOnce under one owned entitlement lock in backend/src/main/java/net/pchinese/allowance/application/AiAllowanceService.java.
- [x] T013 [US2] Implement an internal typed allowance command interface for F08/F11 in backend/src/main/java/net/pchinese/allowance/application/AiAllowanceCommands.java.
- [x] T014 [US2] Ensure no public controller exposes allowance mutation by adding API-boundary tests in backend/src/test/java/net/pchinese/allowance/AllowanceApiBoundaryTest.java.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T015 [P] Add F03 E2E allowance-summary and exhausted-state coverage in frontend/e2e/f03-entitlement.spec.js.
- [ ] T016 Run clean-migration, lock/unlock-preservation and quickstart checks in specs/F03-plans-entitlement-ai-allowance/quickstart.md.

## Dependencies and Execution Order

- F03 requires F01 ACTIVE identity state; entitlement provisioning must be wired after F01 activation.
- T008 requires F02 T009, which owns the canonical GET and PATCH /me controller and response envelope.
- Phase 2 blocks both stories.
- US2 is an internal dependency for F08 and F11, but its UI-independent backend work can proceed alongside US1 after the schema exists.

## Parallel Opportunities

- T002 and T004 can proceed independently after T001.
- T006 and T009 are parallel owner-summary tests/client tasks.
- T010 and T011 are independent test suites before T012.

## Implementation Strategy

Release automatic Free entitlement and safe summary first. Then complete the internal allowance transaction boundary before implementing any F08 or F11 private AI call.
