# Data Model — F04 Content Administration

## Content entities

| Entity | Required safe fields and invariants |
| --- | --- |
| topics | Identifier, title/slug, ordering, lifecycle, author/update attribution, current version and publish time. A published topic is not learner-visible without a valid published lesson. |
| lessons | Parent topic, Free access only, ordering, lifecycle, current version, and publish time. It may publish only when every associated segment is published and every associated media asset is approved. |
| segments | Parent lesson, unique positive sequence number within that lesson, media reference, learner content, lifecycle, and current version. Creation, change, reordering, unpublish, and archive are rejected while the parent lesson is published. |
| media assets | One `YOUTUBE` `VIDEO` source with an immutable canonical 11-character video ID, safety-review state, approval attribution, safe display metadata, and current version. The submitted URL, local file metadata, storage key, credential, and raw verification result are never retained or exposed. |
| content audit events | Immutable identifier/time, actor, action code, target type/id, success or rejected outcome, safe reason code, expected and observed version, request correlation, and safe before/after state. |

## State transitions

~~~text
Topic / Lesson / Segment
DRAFT -> PUBLISHED -> UNPUBLISHED -> PUBLISHED
DRAFT | PUBLISHED | UNPUBLISHED -> ARCHIVED
ARCHIVED -> no transition

Media
PENDING_SCAN -> APPROVED | REJECTED | QUARANTINED
APPROVED -> REJECTED | QUARANTINED
REJECTED | QUARANTINED -> no transition

APPROVED media -> REJECTED | QUARANTINED
  => every dependent PUBLISHED lesson -> UNPUBLISHED
~~~

## Cross-entity integrity

- All existing-row commands compare the caller's expected version with the current version and retain
  the latest state on conflict.
- A lesson is publishable only with a published parent topic, at least one segment, every segment
  published, Free access, and approved media for every segment.
- A learner-visible segment/order/media change requires its lesson to be unpublished first. The
  media safety cascade is the sole automatic withdrawal path.
- Media intake is one validated YouTube video ID, watch link, share link, or embed link. The backend
  normalizes valid input to the canonical ID and starts it pending trusted-source review; it may be
  approved only after a clean result. Local file upload/download and generic URLs are not an intake
  mode.
- `provider_name` is always `YOUTUBE`, `media_kind` is always `VIDEO`, and the canonical ID is
  unique across media assets. A source cannot be replaced in place; a different video is a new media
  asset and receives its own review and audit history.
- Rejected and quarantined media are terminal. A replacement uses a distinct media asset.
- Content administration never changes learner entitlement, quota, attempt, recording, chat,
  vocabulary, or progress data.

## Audit and retention integrity

- Every accepted content/media action and every authenticated rejected command creates one safe
  content audit event; accepted action, cascade, and audit events commit atomically.
- Rejected-command audit writing is independent of the rolled-back command transaction and records
  no raw request body, transcript, submitted media URL, credential, verification diagnostic, or
  learner data; a source reference may be represented only by its canonical video ID.
- Audit events are insert-only in the application and database layers. Update and delete attempts
  are rejected.
- Canonical schema definition precedes a single forward canonical migration. The audit table indexes
  target/time, actor/time, and correlation for safe investigation.
