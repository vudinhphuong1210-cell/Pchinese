---

description: "Actionable implementation tasks for F12 AI Plan Administration and Monitoring"
---

# Tasks: AI Plan Administration and Monitoring

**Input**: [spec.md](./spec.md), [plan.md](./plan.md), [research.md](./research.md), [data-model.md](./data-model.md), [f12-openapi.yaml](./contracts/f12-openapi.yaml), [f12-private-telemetry.md](./contracts/f12-private-telemetry.md) and [quickstart.md](./quickstart.md).

**Prerequisites**: F01 server-managed ADMIN authorization; approved F12 feature-map and F03/DATA_short amendments; the F03 allowance/cycle foundation; F08/F11 private AI telemetry producers when those capabilities are active.

**Tests**: Required by the project constitution: JUnit 5/Mockito, PostgreSQL-backed integration, contract tests for the public/private boundaries, Jest frontend tests and a deterministic Admin E2E journey. Provider adapters remain mocked outside a controlled environment.

**Organization**: Tasks are grouped by user story. Foundational work establishes the shared policy/cycle and schema boundary; US1 and US2 then provide separately demonstrable Admin policy and reporting value; US3 depends on final F12 measurements from US2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Different files with no unfinished prerequisite task.
- **[US#]**: User story traceability label.

## Phase 1: Setup and Governance

**Purpose**: Authorize the expanded Admin scope and make contracts authoritative before code or migration work begins.

- [X] T001 Amend `specs/000a-mvp-feature-map/spec.md`, `specs/F03-plans-entitlement-ai-allowance/spec.md`, `specs/F03-plans-entitlement-ai-allowance/data-model.md`, `specs/F03-plans-entitlement-ai-allowance/contracts/f03-openapi.yaml` and `API.md` to authorize F12's prospective global policy management, relax F03's fixed Free constants, and preserve the no-individual-entitlement/no-billing boundary.
- [X] T002 Update `DATA_short.md` and create the single consolidated approved migration `backend/src/main/resources/db/migration/V003__f03_f12_ai_operations.sql` for F03/F12 plan-policy versions, allowance cycles, operational measurements, monitoring records and AI-admin audit records with constraints/indexes from `data-model.md`. (The active Flyway location is `supabase/migrations`; the equivalent canonical migration and its immutable follow-up correction live there.)
- [X] T003 Create the F12 module/package and test layout in `backend/src/main/java/net/pchinese/aiops/{api,application,domain,persistence}/`, `backend/src/test/java/net/pchinese/aiops/`, `frontend/src/features/admin/ai/` and `frontend/src/api/adminAi.js` without adding a new runtime service or client-side authority.

---

## Phase 2: Foundational F03/F12 Boundary

**Purpose**: Implement the server-owned version/cycle/telemetry primitives that all F12 stories rely on.

**⚠️ CRITICAL**: Complete this phase before invoking F12 public Admin routes or UI work.

- [X] T004 [P] Implement JPA entities, enums and Spring Data repositories for `PlanPolicyVersion`, `EntitlementAllowanceCycle`, revised `UserEntitlement`/`AiUsageEvent`, `AiOperationalMeasurement`, `AiMonitoringRule`, `AiMonitoringAlert` and `AiAdminAuditEvent` in `backend/src/main/java/net/pchinese/{entitlement,aiops}/persistence/` and `backend/src/main/java/net/pchinese/{entitlement,aiops}/domain/`.
- [ ] T005 Add PostgreSQL integration coverage for the migration constraints, one current Free policy/cycle invariant, immutable revisions, measurement deduplication and append-only audit behavior in `backend/src/test/java/net/pchinese/{entitlement,aiops}/F12SchemaAndInvariantIT.java`.
- [X] T006 Implement F03 locked policy selection and allowance-cycle rollover, revise runtime entitlement-summary projection, and preserve reserve/reuse/succeed/refund exact-once behavior in `backend/src/main/java/net/pchinese/entitlement/application/{EntitlementService,AiAllowanceService}.java`, `backend/src/main/java/net/pchinese/profile/api/CurrentUserController.java` and `backend/src/test/java/net/pchinese/entitlement/EntitlementCyclePolicyIT.java`.
- [X] T007 Define the shared private telemetry DTO/schema and Spring-side validator in `ai-service/src/shared/contracts/aiTelemetry.ts`, `backend/src/main/java/net/pchinese/aiops/domain/ProviderTelemetry.java` and `backend/src/test/java/net/pchinese/aiops/ProviderTelemetryContractTest.java`, enforcing absent-versus-zero semantics and no private payload fields.

**Checkpoint**: The schema, F03 cycle boundary and typed telemetry validator are ready; no browser/API exposes an F12 capability yet.

---

## Phase 3: User Story 1 — Manage AI plan policies (Priority: P1) 🎯 MVP

**Goal**: An ADMIN can publish and retire immutable Free/Premium catalogue policy revisions without changing a learner's current entitlement or activating billing.

**Independent Test**: Publish a valid Premium revision, verify immutable history/audit and no enrollment; publish a Free revision, verify a new learner receives it while an existing learner adopts it only at the next locked cycle.

### Tests for User Story 1

- [ ] T008 [P] [US1] Add ADMIN authorization, request validation, stale-version, final-Free-retirement, prospective-cycle and no-manual-entitlement integration tests in `backend/src/test/java/net/pchinese/aiops/AiPlanPolicyControllerIT.java`.
- [ ] T009 [P] [US1] Add policy publication/retirement concurrency and audit-atomicity unit tests in `backend/src/test/java/net/pchinese/aiops/AiPlanPolicyServiceTest.java`.
- [ ] T010 [P] [US1] Add contract-client, form validation, conflict-reload and keyboard-accessible policy panel tests in `frontend/src/api/adminAi.test.js` and `frontend/src/features/admin/ai/PlanPoliciesPanel.test.jsx`.

### Implementation for User Story 1

- [X] T011 [US1] Implement transactional policy revision/publication/retirement commands, Free-policy safety checks and same-transaction safe audit writes in `backend/src/main/java/net/pchinese/aiops/application/AiPlanPolicyService.java` and `backend/src/main/java/net/pchinese/aiops/application/AiAdminAuditService.java`.
- [X] T012 [US1] Implement validated ADMIN-only plan/revision/retirement DTOs, controller and standard error mapping for the plan paths in `backend/src/main/java/net/pchinese/aiops/api/{AiPlanPolicyController,AiPlanPolicyExceptionHandler}.java` and synchronize `specs/F12-ai-plan-administration-monitoring/contracts/f12-openapi.yaml` with the implemented envelope.
- [X] T013 [US1] Implement contract-bound plan list/history/publish/retire methods with safe error mapping in `frontend/src/api/adminAi.js`.
- [X] T014 [US1] Implement `frontend/src/features/admin/ai/PlanPoliciesPanel.jsx` with semantic-token forms, reason input, immutable revision history, loading/empty/error states, focus-managed validation, conflict reload and no learner-level controls.
- [X] T015 [US1] Mount the policy panel in `frontend/src/features/admin/ai/AiAdministrationWorkspace.jsx` and add the ADMIN-only navigation/active-tab guard in `frontend/src/App.jsx` and `frontend/src/components/Sidebar.jsx`.

**Checkpoint**: US1 is independently demonstrable: policy configuration is auditable and prospective, while billing and learner-level mutation remain unavailable.

---

## Phase 4: User Story 2 — Understand aggregate AI use (Priority: P1)

**Goal**: An ADMIN can read a bounded, privacy-safe aggregate report of AI units, provider token/cost coverage and reliability by plan policy, capability and outcome.

**Independent Test**: Seed successful, quota-denied, refunded and retried operations with metering present/absent and multiple currencies; a 50-day report reconciles once per logical request without exposing learner/provider-private data.

### Tests for User Story 2

- [ ] T016 [P] [US2] Add reserve/retry/quota-denial/succeed/refund/late-correction measurement lifecycle tests in `backend/src/test/java/net/pchinese/aiops/AiOperationalMeasurementServiceTest.java` and `backend/src/test/java/net/pchinese/aiops/AiOperationalMeasurementIT.java`.
- [ ] T017 [P] [US2] Add private F08/F11 telemetry schema, malformed/missing telemetry, redaction and safe provider-failure contract tests in `ai-service/src/shared/contracts/aiTelemetry.test.ts`, `backend/src/test/java/net/pchinese/ai/PrivateTelemetryContractTest.java` and `backend/src/test/java/net/pchinese/shadowing/PrivateTelemetryContractTest.java`.
- [ ] T018 [P] [US2] Add ADMIN/non-ADMIN, range/UTC-boundary, report-reconciliation, currency-bucket, partial-data and no-learner-field API tests in `backend/src/test/java/net/pchinese/aiops/AiUsageReportControllerIT.java`.
- [ ] T019 [P] [US2] Add aggregate-report client/UI tests for loading, empty, partial, unavailable-not-zero, date validation and privacy-safe rendering in `frontend/src/api/adminAi.test.js` and `frontend/src/features/admin/ai/AiUsageReportPanel.test.jsx`.

### Implementation for User Story 2

- [X] T020 [US2] Implement idempotent PENDING/final/quota-denied operational-measurement creation, finalization, freshness and four-month retention cleanup in `backend/src/main/java/net/pchinese/aiops/application/{AiOperationalMeasurementService,AiOperationalMeasurementRetentionJob}.java` and `backend/src/main/java/net/pchinese/aiops/persistence/AiOperationalMeasurementRepository.java`.
- [X] T021 [P] [US2] Extend the F11 private response flow in `ai-service/src/features/ai-buddy/aiBuddy.service.ts`, `backend/src/main/java/net/pchinese/ai/integration/AiServiceClient.java` and `backend/src/main/java/net/pchinese/ai/application/AiBuddyService.java` to validate optional telemetry and settle its original F12 measurement exactly once.
- [X] T022 [P] [US2] Extend the F08 private response flow in `ai-service/src/features/shadowing-feedback/shadowingFeedback.service.ts`, `backend/src/main/java/net/pchinese/shadowing/integration/ShadowingAiServiceClient.java` and `backend/src/main/java/net/pchinese/shadowing/application/ShadowingAssessmentService.java` to validate optional telemetry and settle its original F12 measurement exactly once.
- [X] T023 [US2] Implement JPA aggregate projections with UTC range/50-day validation, cost-by-currency grouping, metric coverage and data-freshness projection in `backend/src/main/java/net/pchinese/aiops/application/AiUsageReportService.java` and `backend/src/main/java/net/pchinese/aiops/persistence/AiOperationalMeasurementRepository.java`.
- [X] T024 [US2] Implement the ADMIN-only report DTO/controller and standard partial-data/validation mapping in `backend/src/main/java/net/pchinese/aiops/api/{AiUsageReportController,AiUsageReportExceptionHandler}.java` and synchronize `specs/F12-ai-plan-administration-monitoring/contracts/f12-openapi.yaml` plus `API.md`.
- [X] T025 [US2] Extend the report method/error mapping in `frontend/src/api/adminAi.js` and implement `frontend/src/features/admin/ai/AiUsageReportPanel.jsx` with UTC filter inputs, plan/capability filters, cost-currency buckets, metric coverage and accessible loading/empty/partial/error states.
- [X] T026 [US2] Add `AiUsageReportPanel` to `frontend/src/features/admin/ai/AiAdministrationWorkspace.jsx` without adding event-level navigation, learner filters or provider diagnostic output.

**Checkpoint**: US2 is independently demonstrable with deterministic telemetry: every report is aggregate-only, bounded, reconciled and explicit about unavailable/partial metering.

---

## Phase 5: User Story 3 — Monitor AI health and anomalies (Priority: P2)

**Goal**: An ADMIN can configure aggregate one-hour/24-hour thresholds and acknowledge in-dashboard alerts without suppressing ongoing measurement or sending external notifications.

**Independent Test**: Configure a rule, seed a final measurement that breaches it, run the evaluator and verify one privacy-safe alert within five minutes, an auditable acknowledgement and eventual resolution.

### Tests for User Story 3

- [ ] T027 [P] [US3] Add rule validation, stale-version, evaluator idempotency, threshold/window, acknowledgement and resolution unit/integration tests in `backend/src/test/java/net/pchinese/aiops/{AiMonitoringRuleServiceTest,AiMonitoringEvaluatorIT}.java`.
- [ ] T028 [P] [US3] Add ADMIN-only monitoring-rule/alert/audit API, pagination, no-external-delivery and no-private-field tests in `backend/src/test/java/net/pchinese/aiops/AiMonitoringControllerIT.java`.
- [ ] T029 [P] [US3] Add monitoring form, alert state, acknowledgement, conflict-reload, keyboard and recoverable-error tests in `frontend/src/api/adminAi.test.js` and `frontend/src/features/admin/ai/MonitoringPanel.test.jsx`.

### Implementation for User Story 3

- [X] T030 [US3] Implement transactional monitoring-rule CRUD/update version checks, alert acknowledgement and same-transaction safe audit writes in `backend/src/main/java/net/pchinese/aiops/application/{AiMonitoringRuleService,AiMonitoringAlertService}.java` and `backend/src/main/java/net/pchinese/aiops/persistence/{AiMonitoringRuleRepository,AiMonitoringAlertRepository}.java`.
- [X] T031 [US3] Implement the Spring-managed five-minute evaluator for final F12 measurements, one-hour/24-hour JPA aggregate windows and idempotent open/update/resolve transitions in `backend/src/main/java/net/pchinese/aiops/application/AiMonitoringEvaluator.java` and `backend/src/main/java/net/pchinese/aiops/config/AiMonitoringScheduleConfiguration.java`.
- [X] T032 [US3] Implement validated ADMIN-only monitoring-rule, alert, acknowledgement and safe audit-history DTOs/controllers in `backend/src/main/java/net/pchinese/aiops/api/{AiMonitoringRuleController,AiMonitoringAlertController,AiAdminAuditController,AiMonitoringExceptionHandler}.java` and synchronize `specs/F12-ai-plan-administration-monitoring/contracts/f12-openapi.yaml` plus `API.md`.
- [X] T033 [US3] Extend monitoring-rule/alert/audit client operations in `frontend/src/api/adminAi.js` and implement `frontend/src/features/admin/ai/MonitoringPanel.jsx` with semantic status states, accessible acknowledgement, pagination, retry and no external-notification UI.
- [X] T034 [US3] Add `MonitoringPanel` to `frontend/src/features/admin/ai/AiAdministrationWorkspace.jsx`, including only Policies/Reports/Monitoring sections and no learner/event-level route.

**Checkpoint**: All user stories are functional: Admin manages future policy, understands safe aggregate use and receives only in-dashboard operational alerts.

---

## Phase 6: Polish and Cross-Cutting Validation

**Purpose**: Validate full-stack contract coherence, privacy, migration safety and the documented end-to-end journey.

- [X] T035 [P] Verify all F12 and revised F03 public/private contracts remain envelope/schema-compatible in `specs/F12-ai-plan-administration-monitoring/contracts/`, `specs/F03-plans-entitlement-ai-allowance/contracts/f03-openapi.yaml`, `specs/F08-shadowing-practice/contracts/`, `specs/F11-ai-learning-buddy/contracts/` and `API.md`.
- [ ] T036 [P] Add an ADMIN E2E journey covering policy publication, aggregate report, partial-metering state, rule breach and acknowledgement in `frontend/e2e/f12-ai-administration.spec.js` using deterministic backend/provider fixtures only.
- [ ] T037 Run clean-migration, backend unit/integration, private-contract, frontend Jest/lint/build and every scenario in `specs/F12-ai-plan-administration-monitoring/quickstart.md`; record the results and any approved follow-ups in `specs/F12-ai-plan-administration-monitoring/quickstart.md`.

---

## Dependencies and Execution Order

### Phase dependencies

- **Phase 1** authorizes F12 and creates the canonical schema/migration boundary.
- **Phase 2** depends on Phase 1 and blocks all public F12 stories.
- **US1 and US2** can start after Phase 2. They share F03 policy/cycle primitives but have separate public APIs and UI panels; coordinate changes to `frontend/src/api/adminAi.js`.
- **US3** depends on final measurements from US2, because monitoring evaluates their final aggregate outcomes.
- **Phase 6** depends on the desired completed stories.

### User story dependency graph

```text
Setup/Governance → Foundation ──┬──> US1: policy administration (MVP)
                               └──> US2: aggregate reporting ──> US3: monitoring alerts
US1 + US2 + US3 ──> Polish/quickstart
```

### Parallel opportunities

- T004 repository/entity work and T005 schema-invariant integration coverage can proceed in parallel after T002's approved migration contract is stable.
- Within US1, T008–T010 are parallel test-first tasks; T013 frontend client can begin after the OpenAPI contract is stable while T011 implements backend commands.
- Within US2, T016–T019 are parallel test-first tasks. T021 and T022 are parallel F11/F08 integrations after T020 and T007.
- Within US3, T027–T029 are parallel test-first tasks once US2's measurement projection is available.
- T035 and T036 can run in parallel after all implementation tasks; T037 is the final gate.

## Parallel Examples

### User Story 1

```text
Task: "T008 Add backend policy API integration coverage in backend/src/test/java/net/pchinese/aiops/AiPlanPolicyControllerIT.java"
Task: "T009 Add policy service concurrency coverage in backend/src/test/java/net/pchinese/aiops/AiPlanPolicyServiceTest.java"
Task: "T010 Add policy client/panel tests in frontend/src/api/adminAi.test.js and frontend/src/features/admin/ai/PlanPoliciesPanel.test.jsx"
```

### User Story 2

```text
Task: "T021 Integrate F11 private telemetry in ai-service/src/features/ai-buddy/aiBuddy.service.ts and backend/src/main/java/net/pchinese/ai/"
Task: "T022 Integrate F08 private telemetry in ai-service/src/features/shadowing-feedback/shadowingFeedback.service.ts and backend/src/main/java/net/pchinese/shadowing/"
```

## Implementation Strategy

### MVP first

1. Complete T001–T007, including governance approval and the F03/F12 schema/cycle boundary.
2. Complete US1 (T008–T015) and run its independent test. This delivers source-code-free,
   prospective plan policy administration without exposing learner data or activating Premium.
3. Stop for review before adding telemetry/reporting if the governance amendment is not approved.

### Incremental delivery

1. US1 adds safe policy administration.
2. US2 adds aggregate evidence for capacity/cost/reliability decisions.
3. US3 adds persisted in-dashboard anomaly monitoring; it deliberately does not add external alert delivery.
4. Phase 6 validates the complete F12 acceptance journey and existing F03/F08/F11 boundaries.

## Notes

- Every task uses the required checkbox, sequential ID and exact path format.
- `[P]` tasks touch distinct files after their stated dependencies; do not concurrently edit `frontend/src/api/adminAi.js` or the consolidated migration.
- F12 never authorizes per-learner entitlement/quota mutation, billing, checkout, upgrade UI, direct provider access or raw AI-content exposure.
