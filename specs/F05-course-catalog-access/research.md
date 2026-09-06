# Phase 0 Research — F05

## Decision: catalog is public but only for publishable Free content

**Decision**: Topic/lesson list and detail use one backend publication/access predicate and return
only PUBLISHED Free projections.

**Rationale**: F05 permits visitor discovery but excludes unpublished, Premium and provider content.

**Alternatives considered**: React route-guard filtering or returning full content then hiding fields
are rejected because direct API callers could bypass them.

## Decision: no private learner state in catalog

**Decision**: Catalog/detail DTOs have title, summary, HSK, duration and published counts only;
F06 provides owned progress after Player entry.

**Rationale**: F05 forbids progress/attempt/chat disclosure and catalog reads must not create progress.

**Alternatives considered**: Embedding progress conditionally in public detail was rejected for
contract ambiguity and privacy.

## Decision: withdrawal preserves history but blocks new resource use

**Decision**: An unavailable lesson/segment/media is denied at next access with recoverable catalog
state; existing progress stays intact.

**Rationale**: Content availability changes must not erase learner data.

**Alternatives considered**: Cached playback continuation or deletion of progress are rejected.
