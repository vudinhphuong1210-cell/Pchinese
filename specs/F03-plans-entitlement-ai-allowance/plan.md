# Implementation Plan: Plans, Entitlement, and AI Allowance

**Branch**: `F03-plans-entitlement-ai-allowance` | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

## Summary

Implement the server-authoritative MVP Free entitlement: every eligible learner has exactly one
current Free plan with 30 AI units per rolling 30-day cycle. Spring Boot supplies a safe summary and
atomically reserves/reuses/refunds allowance for the future F08/F11 calls; no billing, Premium
activation, upgrade journey or Admin plan/quota control is introduced.

## Technical Context

**Language/Version**: Java 21/Spring Boot 3.4.5; React 18/TypeScript strict  
**Dependencies**: Spring Data JPA, Flyway, Jakarta Validation; typed API client/Jest  
**Storage**: `subscription_plans`, `user_entitlements`, `ai_usage_events`, F01 users in `DATA_short.md`  
**Testing**: JUnit/Mockito, PostgreSQL-compatible integration, Jest summary/error mapping  
**Scope**: Free assignment/summary, atomic allowance ledger and private application interface for F08/F11.

## Constitution Check

| Gate | Result | Design response |
| --- | --- | --- |
| Backend authority | PASS | Only Spring Boot creates/changes entitlement and usage events. |
| Scope boundary | PASS | No payment provider, Premium activation, upgrade UI or manual Admin mutation. |
| Integrity/concurrency | PASS | Entitlement row is locked for cycle rollover/reservation and idempotency is fingerprint-bound. |
| Privacy/tests | PASS | Summary has no raw ledger; quota/refund/idempotency paths are tested. |

Post-design result: PASS.

## Project Structure

~~~text
backend/src/main/java/net/pchinese/
└── entitlement/{api,application,domain,persistence}/
frontend/src/api/me.ts
frontend/src/features/account/
specs/F03-plans-entitlement-ai-allowance/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/f03-api.md
~~~

## Implementation Approach

1. Seed one active Free subscription-plan policy: 30 units, 30-day rolling cycle, no learner-visible
   Premium activation. On eligible account activation, create/reuse one active entitlement.
2. Contribute a safe entitlement fragment to F02's canonical GET `/me` projection: plan code,
   allowance limit/used/remaining and cycle start/end only. Do not expose usage events or mutation
   endpoints.
3. Centralize `reserveOrReuse`, `succeed` and `refundOnce` in an entitlement application
   service. It locks the active entitlement, rolls the cycle server-side, checks quota and binds a
   `clientRequestId` to a server-computed feature/owned-operation fingerprint.
4. Allow only F08 actual assessment and F11 eligible reply requests to consume one unit. Same
   fingerprint reuses result; ID reuse for a different activity/fingerprint returns safe
   `409 IDEMPOTENCY_CONFLICT`; post-reservation failure maps once to `FAILED_REFUNDED`.
5. Keep account lock/unlock neutral: it blocks access through F01 but never resets/revokes/recreates
   entitlement or allowance state.

## Complexity Tracking

None.
