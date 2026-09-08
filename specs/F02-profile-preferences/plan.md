# Implementation Plan: Profile Preferences

**Branch**: `F02-profile-preferences` | **Date**: 2026-09-08 | **Spec**: [spec.md](./spec.md)

## Summary

Implement learner-owned profile preferences and an ADMIN-only system activity history on top of F01 identity.
Spring Boot owns profile versioning, role authorization, audit minimization and the controlled
audit-event taxonomy; React provides contract-bound forms and an accessible ADMIN audit view.

## Technical Context

**Language/Version**: Java 21/Spring Boot 3.4.5; React 18/JavaScript/JSX/Vite
**Dependencies**: Spring Security, Jakarta Validation, Spring Data JPA, Flyway; contract-bound frontend API client, Jest
**Storage**: Existing `users` profile row and append-only `auth_audit_events` table in `DATA_short.md`
**Testing**: JUnit/Mockito, Spring integration, Jest and E2E
**Scope**: `/me` profile projection/update, ADMIN-only `/admin/audit-events` system activity history,
and a structured audit-recording policy for F02 event writers; no learner activity history,
entitlement changes, account deletion or provider integration.

## Constitution Check

| Gate | Result | Design response |
| --- | --- | --- |
| Server authority/ownership | PASS | Backend loads the authenticated owner and profile version for every operation. |
| Data integrity | PASS | Profile uses optimistic versioning. |
| Privacy/security | PASS | Profile data is private to its owner. |
| Activity-log privacy | PASS | Only ADMIN can list the safe system audit projection; each item has only type, time and permitted account-name labels, never profile values or audit metadata. |
| Structured audit records | PASS | Event type maps to an approved category; bounded outcome/reason/changed-field codes are the only values admitted to audit details. Session and correlation context stay private. |
| Accessibility/quality | PASS | Contract-bound client, conflict/reload UI and tests are required. |

Post-design result: PASS. The plan adds no schema migration, token storage, learner-data Admin
access or F03 entitlement behavior.

## Project Structure

~~~text
backend/src/main/java/net/pchinese/
├── auth/application/{AuthAuditService,AuditEventTaxonomy}.java
├── users/{api,application}/
└── profile/{api,application,domain,persistence}/
frontend/src/
├── api/{profile,adminAuditEvents}.js
├── features/{profile,admin}/
├── components/Sidebar.jsx
└── pages/settings/
specs/F02-profile-preferences/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/f02-openapi.yaml
~~~

## Implementation Approach

1. Make F02 the canonical `GET /me` and `PATCH /me` owner: return the profile version and require
   `expectedProfileVersion` on PATCH. Map stale writes to `409 STATE_CONFLICT`; the client reloads
   the current projection with GET before another update. F03 later contributes its entitlement
   fragment to this established route after its authoritative persistence exists.
2. Validate all profile preference fields in DTO/service, preserve prior values on failure and keep
   profile reads/updates owner-only.
3. Build the Profile Settings page with a contract-bound client, no optimistic conflict overwrite,
   safe 401/409 states and responsive accessible controls.
4. Reuse the canonical append-only `auth_audit_events` store through the auth module's public audit
   service. The audit service centrally maps each approved event type to a category, accepts only
   bounded outcome/reason codes and an allowlisted changed-field-code array, and rejects raw fallback
   messages or unsafe fields. A profile update and its minimized
   `PROFILE_PREFERENCES_UPDATED` event share one transaction. Actor/target, occurrence time, session
   and correlation remain in the existing canonical record; session/correlation are private incident
   trace, not public fields.
5. An ADMIN-only read flow checks the server-derived role in controller and service, returns a
   newest-first page of safe event type/time and permitted account-name labels, and never exposes
   audit details, UUIDs, outcome/reason codes, session/correlation identifiers or profile values.
   Learners have neither endpoint nor navigation. No migration is needed because the canonical table
   already exists.

## Complexity Tracking

None.
