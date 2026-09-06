# Phase 0 Research — F06

## Decision: one progress aggregate owns ordered transitions

**Decision**: lesson_progresses is unique by user/lesson and only the Progress application service
changes completed count, current segment or lesson status.

**Rationale**: F06 requires exactly-once sequential completion across devices.

**Alternatives considered**: F07/F08 mutating progress directly or client-computed unlock were
rejected for race/authority failures.

## Decision: Player end is automatic and server-validated

**Decision**: Playback events require a short-lived signed capability and watermark validation.
Manual complete controls and unbounded forward seek are absent.

**Rationale**: Browser position report alone cannot prove progression; F06 forbids skipping.

**Alternatives considered**: Trusting positionMs or marking complete on Player open was rejected.

## Decision: first player entry is not learning progress

**Decision**: First permitted entry creates NOT_STARTED with first current segment; IN_PROGRESS begins
only after accepted positive playback progress.

**Rationale**: F06 says catalog/summary never creates state and completion requires actual playback.

**Alternatives considered**: Creating progress from catalog or immediately marking in-progress was
rejected.
