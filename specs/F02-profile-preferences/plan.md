# Implementation Plan: Profile Preferences

**Branch**: `F02-profile-preferences` | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

## Summary

Implement learner-owned profile preferences on top of F01 identity. Spring Boot owns profile
versioning; React provides contract-bound forms and accessible validation feedback.

## Technical Context

**Language/Version**: Java 21/Spring Boot 3.4.5; React 18/JavaScript/JSX/Vite
**Dependencies**: Spring Security, Jakarta Validation, Spring Data JPA, Flyway; contract-bound frontend API client, Jest
**Storage**: Existing `users` profile row in `DATA_short.md`
**Testing**: JUnit/Mockito, Spring integration, Jest and E2E
**Scope**: `/me` profile projection and update; no role, entitlement, admin browsing, account
deletion or provider integration.

## Constitution Check

| Gate | Result | Design response |
| --- | --- | --- |
| Server authority/ownership | PASS | Backend loads the authenticated owner and profile version for every operation. |
| Data integrity | PASS | Profile uses optimistic versioning. |
| Privacy/security | PASS | Profile data is private to its owner. |
| Accessibility/quality | PASS | Contract-bound client, conflict/reload UI and tests are required. |

Post-design result: PASS. The plan adds no service, token storage, learner-data Admin access or
F03 entitlement behavior.

## Project Structure

~~~text
backend/src/main/java/net/pchinese/
└── profile/{api,application,domain,persistence}/
frontend/src/
├── api/profile.js
├── features/profile/
└── pages/settings/
specs/F02-profile-preferences/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/f02-openapi.yaml
~~~

## Implementation Approach

1. Make F02 the canonical `GET /me` and `PATCH /me` owner: return the profile version plus the
   safe F03 entitlement/allowance fragment; require `expectedProfileVersion` on PATCH and map
   stale writes to `409 STATE_CONFLICT` with the latest safe current-user projection.
2. Validate all profile preference fields in DTO/service, preserve prior values on failure and keep
   profile reads/updates owner-only.
3. Build the Profile Settings page with a contract-bound client, no optimistic conflict overwrite,
   safe 401/409 states and responsive accessible controls.

## Complexity Tracking

None.
