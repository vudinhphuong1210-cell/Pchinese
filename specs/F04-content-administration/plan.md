# Implementation Plan: Content Administration

**Branch**: F04-content-administration | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

## Summary

Deliver the ADMIN-only Content/Media tab: create and edit ordered topics, lessons, segments and
media; enforce lifecycle, version conflicts and publish prerequisites. Spring Boot owns all
content/media decisions; public catalog reads remain safe published-Free projections.

## Technical Context

**Language/Version**: Java 21/Spring Boot 3.4.5; React 18/JavaScript/JSX
**Dependencies**: Spring Data JPA, Flyway, Jakarta Validation, approved media adapter; contract-bound API client/Jest
**Storage**: topics, lessons, segments, media_assets and approved content-audit extension  
**Testing**: JUnit/Mockito, integration/migration, Jest/E2E  
**Scope**: Content/Media admin commands and projections; excludes learner data, plan/quota, Premium
activation, AI and arbitrary media URLs.

## Constitution Check

| Gate | Result | Design response |
| --- | --- | --- |
| ADMIN/server authority | PASS | Service checks current ADMIN role; frontend guard is UX only. |
| Lifecycle/data integrity | PASS | Versioned entities, transactional publish/quarantine cascade and terminal archive. |
| Privacy/security | PASS | No learner-private data or persistent provider URL; upload/scan/approval through backend. |
| Migration safety | PASS WITH REQUIRED UPDATE | Add approved content audit schema to DATA_short.md and focused Flyway migration before code. |

Post-design result: PASS when the DATA_short.md audit extension is approved in implementation review.

## Project Structure

~~~text
backend/src/main/java/net/pchinese/
├── learning/{api,application,domain,persistence}/
└── media/{api,application,domain,persistence,integration}/
frontend/src/{api,features/admin,pages,routes}/
specs/F04-content-administration/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/f04-api.md
~~~

## Implementation Approach

1. Build ADMIN draft projections separate from public catalog projections; do not use public GET
   routes to edit draft content. Validate exact parent/child references and ordered segment uniqueness.
2. Require expectedVersion for all existing-row edits and lifecycle commands. Stale mutation returns
   409 STATE_CONFLICT without change.
3. Permit learner-visible lesson publish only if parent topic is PUBLISHED, it has at least one
   PUBLISHED segment, every referenced media asset is APPROVED and access is FREE.
4. Require unpublish before altering a published lesson, segment sequence or referenced media.
   ARCHIVED topic/lesson/segment is terminal; create a replacement row rather than restore it.
5. Validate Admin media upload/metadata, scan then command APPROVE/REJECT/QUARANTINE. An APPROVED to
   rejected/quarantined transition transactionally unpublishes dependent published lessons.
6. Append safe content audit events and build accessible admin form, conflict-reload and destructive
   confirmation UX.

## Complexity Tracking

One approved schema extension, content_audit_events, is required because auth audit records cannot
represent F04 mandatory content-operation audit without crossing domains.
