# Phase 0 Research — AI Plan Administration and Monitoring

## Decision: Stable plans use immutable policy versions and cycle snapshots

**Decision**: Keep `FREE` and `PREMIUM` as the two stable plan identities. Each accepted Admin
change creates a complete immutable policy version. A newly eligible learner selects the current
published Free version immediately; a learner already in a cycle keeps its cycle's policy and quota
snapshot until locked rollover creates the next cycle.

**Rationale**: `subscription_plans.plan_code` is unique and F03's entitlement currently has only a
mutable current-cycle counter. A policy-version plus cycle snapshot provides a provable historical
source for a report, a late refund and a prospective allowance change without resetting an active
learner.

**Alternatives considered**: Updating the plan row in place loses historical policy/cost context.
Applying a revision immediately to every entitlement violates F12's clarified cycle rule.
Creating a manual Admin entitlement command violates F03 ownership and privacy boundaries.

## Decision: Retain a separate pseudonymized operational measurement

**Decision**: Record one F12 operational measurement per logical eligible AI request, separately
from the F03 allowance ledger. It references the allowance event when one exists, and uses an
internal HMAC-derived deduplication key when quota is denied before F03 creates an event.

**Rationale**: F03's `ai_usage_events` cannot represent a quota denial and deliberately lacks
provider metering/latency fields. A separate measurement permits aggregate usage, cost and health
reporting while the F03 ledger remains authoritative for quota. Reused requests update/reuse the
same measurement; only final outcomes contribute to aggregates.

**Alternatives considered**: Counting HTTP requests double-counts retries. Extending the learner
ledger with raw provider data mixes different retention/privacy purposes. Sending event-level data
to an external analytics product expands scope and violates the no-new-service constraint.

## Decision: Metering is optional, typed and returned only to Spring Boot

**Decision**: Extend the existing private F08/F11 `ai-service` response with nullable,
schema-validated input/output/total token counts, estimated cost/currency, provider duration and a
safe failure class. Spring Boot measures its own end-to-end latency and decides final F03/F12 state.

**Rationale**: Providers may not report every metric. An absent field must remain unavailable,
rather than being recorded as zero. The AI service stays stateless and sends no learner content,
JWT, credential or authoritative business decision to F12.

**Alternatives considered**: Browser/provider metering exposes credentials and bypasses Spring
Boot. Parsing provider logs or response bodies risks secret/private-content retention. Estimating
unreported values produces misleading financial reporting.

## Decision: Aggregate reads use bounded UTC intervals and currency buckets

**Decision**: Report requests use a UTC `[fromInclusive, toExclusive)` interval no longer than 50
days, drawn from the last four months. Cost is aggregated only within a reported currency; the API
does not convert currencies or invent a cross-currency total.

**Rationale**: UTC makes boundaries deterministic across operator browsers and scheduled alert
evaluation. A bounded indexed range supports the five-second goal. Currency conversion requires an
exchange-rate source and accounting policy not requested by F12.

**Alternatives considered**: Browser-local dates produce inconsistent report/alert boundaries.
An unlimited range risks unbounded aggregation. Implicit currency conversion adds a financial
authority outside the feature.

## Decision: Alerts are in-dashboard records evaluated by Spring scheduling

**Decision**: A Spring-managed evaluator runs at least once per five minutes, queries final
measurements via JPA aggregate projections, and idempotently opens/updates/resolves alert records
for enabled one-hour or 24-hour rules. Acknowledgement records an actor/time/note but does not halt
evaluation or suppress a continuing breach.

**Rationale**: This meets the visibility outcome without adding a queue, scheduler service, email,
chat or pager integration. Evaluation can be retried safely from persisted rules and measurements.

**Alternatives considered**: Evaluating only when an Admin opens the dashboard misses the
five-minute target. Evaluating inside a provider/allowance transaction makes an external call path
longer and couples alert failure to learning. External notifications are explicitly out of scope.

## Decision: Use dedicated AI-admin audit records and conflict-safe commands

**Decision**: Store F12 policy/revision, retirement, monitoring-rule and acknowledgement evidence
in an append-only `ai_admin_audit_events` store. Policy/rule/alert mutable records use an expected
version and return `409 STATE_CONFLICT` without overwrite when stale.

**Rationale**: The existing auth audit taxonomy is deliberately restricted to auth/account events;
F04 uses a domain audit store for the same reason. Dedicated records keep approved policy values,
reason and operational note auditable without leaking learner/provider data.

**Alternatives considered**: Reusing unrestricted JSON in auth audit weakens its allowlist.
Silently accepting a stale Admin form loses a newer policy/rule. Recording raw provider errors or
learner references creates a privacy breach.

## Decision: Reports are aggregate-only ADMIN projections

**Decision**: Expose only period/plan-policy/capability/outcome aggregate buckets, coverage and
freshness through `/api/v1/admin/ai/*`. There is no endpoint to enumerate operational measurements,
usage events, learner accounts, prompts, replies, recordings or provider diagnostics.

**Rationale**: F12 provides operational control without changing the established rule that ADMIN
does not gain learner-private data access.

**Alternatives considered**: A per-user quota/usage view or CSV export would defeat the stated
privacy boundary. Returning an empty metric as zero hides unavailable provider metering and risks
bad operations decisions.
