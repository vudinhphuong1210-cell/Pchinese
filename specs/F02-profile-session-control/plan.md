# Implementation Plan: Profile and Session Control

**Branch**: `F02-profile-session-control` | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

## Summary

Implement learner-owned profile preferences and safe active-device control on top of F01 identity.
Spring Boot owns profile versioning, session ownership/revocation and the restricted device-limit
recovery flow; React provides typed forms, confirmation UX and in-memory session state only.

## Technical Context

**Language/Version**: Java 21/Spring Boot 3.4.5; React 18/TypeScript strict/Vite  
**Dependencies**: Spring Security, Jakarta Validation, Spring Data JPA, Flyway; typed frontend API client, Jest  
**Storage**: Existing `users` profile row and `auth_sessions`/refresh-token lineage in `DATA_short.md`  
**Testing**: JUnit/Mockito, Spring integration, Jest and E2E  
**Scope**: `/me`, owned session projections/revocation and single-purpose device-limit flow; no role,
entitlement, admin browsing, account deletion or provider integration.

## Constitution Check

| Gate | Result | Design response |
| --- | --- | --- |
| Server authority/ownership | PASS | Backend loads owner/session/profile version for every operation. |
| Data integrity | PASS | Profile uses optimistic version; session mutation is transactional and revocable. |
| Privacy/security | PASS | Device projection is privacy-safe; access/refresh credentials remain protected. |
| Accessibility/quality | PASS | Typed client, conflict/reload UI, confirmations and tests are required. |

Post-design result: PASS. The plan adds no service, token storage, learner-data Admin access or
F03 entitlement behavior.

## Project Structure

~~~text
backend/src/main/java/net/pchinese/
├── users/{api,application,domain,persistence}/
└── auth/{api,application,domain,persistence}/
frontend/src/
├── api/{me,auth}.ts
├── features/{profile,sessions}/
└── pages/settings/
specs/F02-profile-session-control/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/f02-api.md
~~~

## Implementation Approach

1. Make F02 the canonical `GET /me` and `PATCH /me` owner: return the profile version plus the
   safe F03 entitlement/allowance fragment; require `expectedProfileVersion` on PATCH and map
   stale writes to `409 STATE_CONFLICT` with the latest safe current-user projection.
2. Validate all profile preference fields in DTO/service, preserve prior values on failure and keep
   profile reads/updates owner-only.
3. Reuse F01 session storage but expose only the caller’s active device projection. Revoke one/all
   session in transactions; current-session revoke requires explicit confirmation and immediately
   removes access.
4. At device limit, issue no ordinary session. Supply a single-purpose, browser-memory,
   short-lived session-management flow credential; it can list/revoke only that verified account’s
   sessions and is invalidated on completion, close/reload signal or expiry.
5. Build Profile and Session Settings pages with typed clients, no optimistic conflict overwrite,
   safe 401/404/409 states, focus-managed dialogs and responsive accessible controls.

## Complexity Tracking

None.
