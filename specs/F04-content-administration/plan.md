# Implementation Plan: Content Administration

**Branch**: F04-content-administration | **Date**: 2026-09-11 | **Spec**: [spec.md](./spec.md)

## Summary

Deliver the ADMIN-only Content/Media experience for safe topic, lesson, segment, and media
administration. The backend remains authoritative for authorization, lifecycle, content safety,
auditing, and safe projections. MVP video intake accepts a YouTube link or video ID only, stores
its validated canonical identity, and never uploads or downloads video bytes. This revision adds a
closed media state machine, immutable audit outcomes, a canonical migration path, and accessible
memory-session UX.

## Technical Context

**Language/Version**: Java 21/Spring Boot 3.4.5; React 18/JavaScript/JSX
**Dependencies**: Spring Data JPA, Flyway, Jakarta Validation, controlled YouTube-reference adapter,
contract-bound API client, JUnit/Mockito, Jest and Playwright
**Storage**: Canonical \`DATA_short.md\` schema and \`supabase/migrations\`; topics, lessons, segments,
media assets, and immutable \`content_audit_events\`
**Testing**: JUnit/Mockito, PostgreSQL/Testcontainers clean-and-upgrade migration integration,
controller integration, Jest and Admin-content E2E
**Scope**: Content/Media admin commands and safe management projections. Excludes learner data,
plan/quota changes, Premium activation, AI, browser-direct provider calls, arbitrary media URLs,
local video upload/download, and catalog/player delivery owned by later features.

## Constitution Check

| Gate | Result | Design response |
| --- | --- | --- |
| ADMIN/server authority | PASS | Every content/media read and command requires current server-managed ADMIN authorization; UI guards only improve UX. |
| Lifecycle/data integrity | PASS | Versioned commands use one transaction; lesson publication verifies every associated segment and media; live structural/media changes are rejected until unpublish. |
| Privacy/security | PASS | Media enters only through backend validation of a supported YouTube URL/ID; it stores only the canonical video ID. Safe projections omit learner data, raw supplied URLs, scanner diagnostics, and credentials. Session credentials remain memory/cookie based. |
| Migration safety | PASS | The audit schema is first approved in \`DATA_short.md\`, then delivered once through canonical \`supabase/migrations\`, with clean and upgrade verification. |
| Quality/accessibility | PASS | Contract, unit/controller/migration/E2E coverage covers success and failure paths; admin controls use semantic tokens, keyboard/dialog behavior, and recoverable states. |

Post-design result: PASS. No unresolved clarification remains.

## Project Structure

~~~text
backend/src/main/java/net/pchinese/
├── content/{api,application,domain,persistence}/
├── media/{api,application,domain,persistence,integration}/
└── common/{api,error}/
frontend/src/
├── api/
├── features/admin/content/
└── routes/
supabase/migrations/
specs/F04-content-administration/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/f04-openapi.yaml
~~~

## Implementation Approach

1. Build distinct ADMIN management projections and validated request DTOs; never expose entities or
   public catalog projections for draft administration. List operations are bounded, include
   pagination and request correlation, and all operations use the standard response envelope.
2. Require server-managed ADMIN authorization and current expected version for every existing-row
   command. Detect stale writes atomically and return a safe recoverable conflict without state
   change. The frontend uses the shared in-memory authenticated client, never direct fetch or
   browser-persisted credentials.
3. Publish a Free lesson only when its parent topic is published, it owns at least one segment,
   every associated segment is published in the required order, and every associated media asset is
   approved. A safe query specification is the sole future source for public catalog projections.
4. Treat learner-visible changes to lesson fields, segment creation/update/order/unpublish/archive,
   and referenced-media metadata as invalid while the owning lesson is published. Archive remains
   terminal; create a replacement rather than restoring a retired item. Reject invalid commands
   without state change rather than silently modifying live content.
5. Accept one backend-validated YouTube video reference per media asset. Permit a bare canonical
   video ID and the documented watch, share, or embed URL forms; normalize all valid input to the
   canonical ID, ignore incidental playlist/radio query parameters attached to one valid watch-video
   ID, and reject arbitrary URLs, standalone playlist/channel links, redirects, local upload, and
   download requests. A valid reference starts pending safety review; approval is allowed only after
   the required review. Never expose the supplied URL, a storage key, or scanner details.
6. Enforce the closed media state machine: \`PENDING_SCAN\` may become approved, rejected, or
   quarantined; approved may become rejected or quarantined; rejected and quarantined are terminal.
   A safety invalidation transaction withdraws every dependent published lesson before another
   learner read can be served, recording its cascades with the same request correlation.
7. Add \`content_audit_events\` to the canonical data definition and one forward canonical migration.
   Store safe explicit before/after state, expected/observed version, actor, target, outcome,
   reason, and correlation. The audit store has no mutation API, uses insert-only application
   access, and rejects database update/delete attempts. Successful action, required cascades, and
   audit rows commit together; authenticated rejected commands retain one safe rejected audit event
   independently of the rolled-back command transaction.
8. Make Content/Media forms, tabs, lifecycle controls, conflict reload and destructive confirmation
   accessible: semantic theme tokens, keyboard navigation, labelled controls, focus management,
   real modal dialog behavior, responsive touch targets, and non-colour-only feedback.

## Validation Strategy

- Run clean and representative-upgrade PostgreSQL migration tests from the same canonical migration
  source; assert audit table, indexes, append-only protection, and JPA validation.
- Test authorization, safe DTO/envelope behavior, pagination, expected-version conflicts, accepted
  YouTube URL/ID normalization, rejected malformed/unsupported references, all content/media
  transitions, live-change rejections, full publication prerequisites, and media invalidation
  cascades.
- Assert audit contents and correlation for accepted, rejected, stale, and cascading commands;
  assert audit write failure rolls back accepted content changes.
- Run frontend unit and E2E tests for memory-session client use, accessible tabs/dialogs, semantic
  status feedback, destructive confirmation, media transition visibility, and conflict recovery.

## Complexity Tracking

The feature adds one approved audit schema extension and a controlled YouTube-reference adapter
because the existing auth audit store cannot represent F04 lifecycle outcomes, and direct or
arbitrary media input would violate server authority and safety requirements. The change stays
within the modular monolith and uses the existing canonical schema and session boundaries.
