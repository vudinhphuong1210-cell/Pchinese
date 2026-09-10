# Implementation Plan: AI Plan Administration and Monitoring

**Branch**: `F12-ai-plan-administration-monitoring` | **Date**: 2026-09-09 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/F12-ai-plan-administration-monitoring/spec.md`

## Summary

Add one ADMIN-only AI operations workspace that manages immutable, versioned Free and Premium
catalogue policies; reports privacy-safe aggregate AI use; and exposes in-dashboard monitoring
alerts. Spring Boot remains the only authority for policy selection, entitlement-cycle rollover,
telemetry finalization, reporting, alerts and audit. The browser receives aggregate projections
only; it never sees an AI usage event, learner identity, provider payload or secret.

F12 extends the planned F03 model rather than permitting individual entitlement mutation. A policy
revision applies immediately to newly eligible learners; a learner with a current entitlement keeps
its existing quota snapshot until the next server-owned allowance-cycle boundary. F08/F11 private
AI responses contribute optional, minimized provider metering to Spring Boot, which writes a
separate operational measurement for reporting and monitoring.

## Technical Context

| Area | Decision |
| --- | --- |
| Language/version | Java 21 with Spring Boot 3.4.5; React 18 with JavaScript/JSX and Vite; private Node.js/TypeScript `ai-service` only where F08/F11 already authorize it. |
| Primary dependencies | Spring Data JPA, Flyway, Jakarta Validation, Spring scheduling, existing JWT/ADMIN policy, contract-bound frontend HTTP client, Jest, JUnit 5/Mockito/Testcontainers. No new service, queue, cache, analytics SaaS or browser-to-provider integration. |
| Storage | PostgreSQL. Amend the approved F03 schema contract with immutable plan-policy versions, entitlement allowance cycles, privacy-safe operational measurements, monitoring rules/alerts and a dedicated AI-admin audit store. |
| Testing | Unit tests for policy/cycle/measurement/alert state decisions; PostgreSQL-backed integration tests for authorization, constraints, concurrency, report reconciliation and migrations; contract tests for public and private telemetry schemas; Jest client/component tests; an ADMIN E2E journey with deterministic measurements. |
| Target platform | Authenticated web SPA and Spring Boot REST API under `/api/v1`; F08/F11 private telemetry remains server-to-server only. |
| Project type | Full-stack modular-monolith web application with a controlled private AI supporting service. |
| Performance goals | At least 95% of aggregate report reads for a selected period of at most 50 days return a result or explicit partial-data state within five seconds; a verified threshold breach becomes visible within five minutes of its measurement being available. |
| Constraints | ADMIN is enforced by the server; no learner drill-down, raw content, provider body, secret, external alert delivery, billing, checkout, upgrade flow or manual learner entitlement/quota command. Missing metering is `unavailable`, never zero. |
| Scale/scope | Retain pseudonymized operational measurements for four months; reports aggregate up to 50 days per request; monitoring rules evaluate a rolling one-hour or 24-hour window; only `FREE` and `PREMIUM` plan identities exist. |

## Constitution Check

| Gate | Result | Design response |
| --- | --- | --- |
| Server authority and module boundaries | PASS | Spring Boot owns ADMIN checks, policy publication, cycle selection, telemetry settlement, reports, alerts, audit and all persistence. React is presentation only; `ai-service` only returns typed minimized telemetry. |
| AI boundary and privacy | PASS | F08/F11 keep the existing private HMAC/mTLS boundary. No browser calls, provider credentials, raw prompt/reply/audio/transcript, user identifier or provider body enters F12 projections. |
| Entitlement integrity | PASS | Immutable policy/cycle snapshots and short locked rollover/reservation transactions preserve F03 idempotency and exact-once refund; provider work remains outside the transaction. |
| ADMIN least privilege | PASS | All `/admin/ai` commands and reads check the current server-managed role. Reports and alerts are aggregate-only and cannot filter or drill down by learner. |
| Contract/migration safety | PASS, conditional on governance prerequisite | Before implementation, amend `000a-mvp-feature-map`, F03 scope/contract and `DATA_short.md` in the same approved change. Use one consolidated, clean-tested Flyway migration; do not modify deployed migrations. |
| Quality and accessibility | PASS | Contract-bound clients provide loading, empty, partial-data, conflict-reload and recoverable-error states with semantic tokens and keyboard-accessible forms/acknowledgements. |

**Post-design result**: PASS for the proposed architecture. Implementation remains gated on the
feature-map and F03/DATA_short amendments identified above; those amendments authorize the new
ADMIN scope without weakening the prohibition on individual learner access.

## Project Structure

### Documentation (this feature)

```text
specs/F12-ai-plan-administration-monitoring/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── f12-openapi.yaml
    └── f12-private-telemetry.md
```

### Source Code (repository root)

```text
backend/src/main/java/net/pchinese/
├── entitlement/{api,application,domain,persistence}/   # F03 policy/cycle and summary evolution
├── aiops/{api,application,domain,persistence}/          # F12 ADMIN policy/report/rule/alert/audit module
├── ai/{application,integration}/                         # F11 telemetry-finalization integration
└── shadowing/{application,integration}/                  # F08 telemetry-finalization integration
backend/src/main/resources/db/migration/                  # one approved consolidated F12 migration
backend/src/test/java/net/pchinese/{entitlement,aiops,ai,shadowing}/

ai-service/src/
└── shared/contracts/                                    # versioned optional telemetry response extension

frontend/src/
├── api/adminAi.js
├── features/admin/ai/
│   ├── AiAdministrationWorkspace.jsx
│   ├── PlanPoliciesPanel.jsx
│   ├── AiUsageReportPanel.jsx
│   └── MonitoringPanel.jsx
├── components/Sidebar.jsx
├── App.jsx
└── features/admin/ai/*.test.jsx

API.md
DATA_short.md
specs/000a-mvp-feature-map/spec.md
specs/F03-plans-entitlement-ai-allowance/{spec.md,data-model.md,contracts/f03-openapi.yaml}
specs/F08-shadowing-practice/contracts/
specs/F11-ai-learning-buddy/contracts/
```

**Structure Decision**: Extend F03's `entitlement` module only for policy-version selection and
allowance-cycle snapshots. Keep F12 ADMIN commands, aggregate reporting, monitoring and their
audit in a focused `aiops` module. F08/F11 and `ai-service` change only at their typed telemetry
boundaries; neither gains authority over F03/F12 state. The frontend uses one ADMIN workspace with
Policies, Reports and Monitoring sections, rather than adding unrelated application-level stores.

## Implementation Approach

1. **Approve the governing contracts first.** Add F12 to the feature map, revise F03's Free-only
   constants and Admin exclusion to describe F12's prospective global policy management, and amend
   `DATA_short.md` before any migration. Keep Premium catalogue-only: no billing or learner
   enrollment route is introduced.
2. **Version policies and quota cycles.** Preserve the two stable plan identities. Create an
   immutable full policy revision for every accepted Admin change, using expected-version conflict
   protection and an append-only audit event. At F03 activation/rollover, under the existing
   entitlement lock, select the current published Free policy and create the next cycle snapshot.
   Existing cycles and their usage events never change retrospectively.
3. **Capture final operational measurements.** Add a separate, pseudonymized measurement to the
   F03 ledger path for each logical eligible AI request, including an idempotent quota-denial
   measurement with no allowance event. Spring Boot creates/reuses it during reserve, finalizes it
   with success/refund exactly once, and records its data-freshness state. Provider work remains
   outside database transactions.
4. **Extend only the private telemetry contract.** F08/F11 `ai-service` responses may return
   optional input/output/total tokens, cost and currency, provider duration and a safe failure
   class. Spring Boot measures end-to-end duration, validates the typed data, converts missing
   provider values to unavailable, and never persistently stores a provider body or secret.
5. **Provide protected aggregate APIs.** Add contract-first `/api/v1/admin/ai/*` projections for
   plans/revisions, reports, monitoring rules, alerts/acknowledgements and safe audit history.
   Report aggregation is restricted to the last four months and 50 days per selection, groups cost
   by currency rather than converting it, and returns freshness/coverage so partial data cannot be
   mistaken for zero.
6. **Evaluate alerts inside the monolith.** A Spring-managed evaluator runs often enough to meet
   the five-minute visibility criterion, evaluates enabled rules over their selected one-hour or
   24-hour rolling window, and idempotently opens, updates, acknowledges or resolves in-dashboard
   alerts. It sends no email, chat or pager notification.
7. **Build and verify the ADMIN workspace.** Reuse the existing ADMIN-only sidebar/app guard for
   usability, but handle server `401`, `403`, `409`, validation and partial-data responses. Add
   accessible forms, conflict reload, filters, clear metric-unavailable states and audit-safe
   acknowledgement feedback. Verify migrations, API envelopes, race cases, privacy redaction,
   cross-currency cost buckets and UI states against deterministic data.

## Complexity Tracking

No Constitution exception is required. The plan uses the existing modular monolith, PostgreSQL,
JPA, private `ai-service` boundary and frontend conventions.
