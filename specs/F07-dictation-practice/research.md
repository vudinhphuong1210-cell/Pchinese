# Phase 0 Research — F07

## Decision: deterministic Spring Boot evaluator, no AI

**Decision**: Dictation evaluates in the backend from the server-held expected answer and never calls
ai-service or allowance ledger.

**Rationale**: F07 is deterministic practice; AI belongs only to approved F08/F11 routes.

**Alternatives considered**: LLM feedback or browser scoring were rejected for authority, cost and
privacy reasons.

## Decision: final exact normalized answer replaces edit-distance penalty

**Decision**: Normalize Unicode/space/punctuation, then score exact Simplified Hanzi final answer as
100 or retry-needed 0. A learner can retake after correction; no character-level penalty is retained.

**Rationale**: The product direction explicitly removes edit distance so correcting a known mistake is
treated as progress.

**Alternatives considered**: Character edit distance was rejected by product direction; exposing
per-character hints/full answer was rejected by F07 feedback boundary.

## Decision: one active attempt and idempotent submit

**Decision**: Reuse one IN_PROGRESS attempt per learner/segment. After EVALUATED, a new retake
creates history; same clientSubmissionId returns original result.

**Rationale**: Avoids abandoned duplicate attempts and concurrent double evaluation.

**Alternatives considered**: Unlimited active attempts and frontend-only duplicate suppression were
rejected.
