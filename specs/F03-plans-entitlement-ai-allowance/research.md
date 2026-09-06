# Phase 0 Research — F03 Plans, Entitlement, and AI Allowance

## Decision: automatic Free entitlement only

**Decision**: Eligible accounts receive/reuse one ACTIVE Free entitlement with 30 units for each
rolling 30-day cycle. Premium records may remain reserved schema but are never activated or shown.

**Rationale**: F03 and the MVP map explicitly exclude billing, upgrades and manual entitlement work.

**Alternatives considered**: Admin grant/revoke, client-maintained quota and payment-provider
integration are rejected as scope/authority violations.

## Decision: one locked ledger service owns allowance

**Decision**: Reserve/reuse usage in a short transaction on the active entitlement before F08/F11
private calls, then mark success or refund exactly once on invalid/timeout/unavailable outcome.

**Rationale**: Concurrent requests cannot exceed quota or double-charge a retry.

**Alternatives considered**: Decrement after provider result and frontend counter enforcement are
rejected because concurrent/external failure states become inconsistent.

## Decision: idempotency is cross-activity safe

**Decision**: A client request ID binds to an immutable server fingerprint containing feature type
and owned operation ID. Same fingerprint reuses the ledger/result; a different fingerprint is a
conflict before provider/allowance mutation.

**Rationale**: A reused client UUID must not accidentally charge a different AI activity.

**Alternatives considered**: Unique ID alone or provider-side idempotency are rejected because
Spring Boot owns the quota decision.
