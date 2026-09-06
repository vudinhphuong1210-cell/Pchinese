# Data Model — F04 Content Administration

| Entity | F04 invariants |
| --- | --- |
| topics | versioned, ADMIN author fields, lifecycle; PUBLISHED topic alone is not catalog-visible without valid lesson. |
| lessons | parent topic, FREE access only, publication/version; publish requires valid parent/segments/media. |
| segments | lesson FK, unique (lesson_id, sequence_no), approved media reference, transcript/timing held server-side. |
| media_assets | scan/approval state PENDING_SCAN, APPROVED, REJECTED, QUARANTINED; no arbitrary public URL. |
| content_audit_events | approved new append-only audit extension, safe actor/action/target/version/outcome/correlation fields. |

~~~text
DRAFT -> PUBLISHED -> UNPUBLISHED -> PUBLISHED
DRAFT|PUBLISHED|UNPUBLISHED -> ARCHIVED (terminal)
PENDING_SCAN -> APPROVED | REJECTED | QUARANTINED
APPROVED -> REJECTED|QUARANTINED => dependent PUBLISHED lessons -> UNPUBLISHED
~~~

All existing-row mutations require expectedVersion. Content actions never mutate entitlement, quota,
attempt, recording, chat or progress state.
