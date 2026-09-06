# Phase 0 Research — F04 Content Administration

## Decision: public and Admin projections are separate

**Decision**: Public topic/lesson routes return only publishable Free summaries. Admin draft/read
routes are ADMIN-only, paginated and never expose learner work.

**Rationale**: A public catalog cannot safely double as a draft editor.

**Alternatives considered**: Filtering public endpoints by frontend role and returning drafts to
Admin through public DTOs were rejected for disclosure/authority risks.

## Decision: lifecycle is explicit and archive is terminal

**Decision**: Topics/lessons/segments use DRAFT, PUBLISHED, UNPUBLISHED, ARCHIVED. Any learner-live
lesson structural/media change requires unpublish first; ARCHIVED never returns to publishable state.

**Rationale**: F04 protects learners from silent live-content changes and irreversible archival must
remain auditable.

**Alternatives considered**: Direct live editing or archive restore are rejected by F04.

## Decision: media eligibility controls dependent lessons

**Decision**: Publish checks every referenced media asset. APPROVED media becoming REJECTED or
QUARANTINED atomically unpublishes all dependent published lessons.

**Rationale**: Media provider state is not authorization; unsafe/unavailable media cannot remain
learner-playable.

**Alternatives considered**: Hiding only the failed segment or waiting for manual Admin action
leaves broken learner access.

## Decision: add domain-appropriate content audit storage

**Decision**: Add append-only content_audit_events only after DATA_short.md approval, with actor,
target/type/action, correlation, before/after safe state and outcome.

**Rationale**: F04 requires audit; auth_audit_events is restricted to auth/role concerns.

**Alternatives considered**: Reusing auth audit or omitting audit is rejected.
