# Data Model — AI Plan Administration and Monitoring

## Canonical-schema amendment

F12 requires an approved update to `DATA_short.md` before a Flyway migration. F03 is specified but
not yet migrated in `backend/`; consolidate the approved F03/F12 structural changes into one
unmerged migration rather than creating migration churn. All IDs are UUIDs and all times are UTC
timestamps.

## Relationship map

```text
subscription_plans (FREE | PREMIUM)
  └──< plan_policy_versions
         ├──< user_entitlements.current_policy_version
         ├──< entitlement_allowance_cycles
         │     └──< ai_usage_events
         │            └── ai_operational_measurements (nullable source reference)
         └──< ai_operational_measurements (policy snapshot)

ai_monitoring_rules ──< ai_monitoring_alerts
ai_admin_audit_events ──> policy version | monitoring rule | alert
```

## Entities and rules

### `subscription_plans` — stable plan identity

Retain the existing two immutable plan codes, `FREE` and `PREMIUM`. A plan code is never created,
renamed or deleted by F12. Its mutable catalogue/quota information moves to a policy version.
Premium remains catalogue-only and has no entitlement-assignment path in F12.

### `plan_policy_versions` — immutable plan-policy snapshot

| Field group | Rules |
| --- | --- |
| Identity | UUID; required stable plan FK; monotonically increasing revision number unique within that plan. |
| Catalogue | Display name, bounded description, bounded benefit list, availability state and optional display label are a complete snapshot. |
| Price | Non-negative recurring decimal amount; ISO 4217 currency; interval `MONTH` or `YEAR`; no payment credential, tax, invoice or billing external reference. |
| Allowance | Non-negative quota units and valid allowance period; only the Free version selected by the automated entitlement lifecycle is assignable. |
| Lifecycle | `PUBLISHED`, `SUPERSEDED` or `RETIRED`. At most one current published revision per plan; retirement removes only future selection and cannot remove the final published Free policy. |
| Evidence | Required ADMIN actor, bounded reason, creation/publish time and optimistic version. Historical rows are never overwritten. |

Publishing is an atomic revision operation: it makes a complete new snapshot current and marks the
prior current revision superseded. A rejected/stale request creates no version or audit event.

### `user_entitlements` and `entitlement_allowance_cycles` — prospective quota selection

`user_entitlements` continues to identify one learner's current access state. Add its current
policy-version relationship. Move authoritative current-cycle allowance values into
`entitlement_allowance_cycles` so every ledger event is bound to an immutable policy/quota snapshot.

| Entity | Required rules |
| --- | --- |
| `user_entitlements` | One active entitlement per learner; references the policy version selected when it became eligible. F12 does not expose or mutate a row by learner. |
| `entitlement_allowance_cycles` | Entitlement FK; policy-version FK; cycle start/end; allowance-limit snapshot; used-unit counter; current/closed status; optimistic version. One current cycle per active entitlement. |
| `ai_usage_events` | Retain F03 idempotency fingerprint/status; add required allowance-cycle and policy-version FKs. One logical retry points to one event/cycle and settles success/refund exactly once. |

At activation or rollover, the entitlement service locks the entitlement, closes the old cycle if
needed, resolves the current published Free policy at commit time and creates the next cycle. This
is the only route that applies a revised policy to an existing learner. The current learner cycle
and its events are never repriced, reset or reassigned.

### `ai_operational_measurements` — pseudonymized final AI facts

One row represents one logical eligible AI operation for F12 aggregation. It is not a learner-facing
record and no F12 API exposes rows individually.

| Field group | Rules |
| --- | --- |
| Identity/deduplication | UUID and a unique server-derived HMAC deduplication key. The key is derived from the logical request but contains no exposed learner/client identifier. |
| References | Nullable F03 usage-event FK for a reserved request; nullable only for quota denial. Policy-version and capability snapshots are required. The measurement has no ADMIN-visible learner identifier. |
| Outcome | `PENDING`, `SUCCEEDED`, `QUOTA_DENIED`, `FAILED_REFUNDED` or the separately approved consumed-failure state. Only final rows contribute to reports/alerts. |
| Allowance facts | Reserved, used and refunded unit counts. A quota denial has zeros and no allowance-event reference. |
| Provider metering | Nullable input/output/total tokens, nullable cost and required cost currency when cost is present, provider-duration milliseconds and explicit availability/coverage indicators. `NULL` means unavailable; numeric zero means measured zero. |
| Timing/freshness | Occurred, finalized and measured-through times plus end-to-end Spring duration. A delayed/unfinalized source produces an explicit partial-data signal. |
| Retention | Purge/pseudonymize event-level operational measurements after four months under the retention job; no raw AI content, provider body, credential, JWT, correlation ID or user ID is stored in the F12 projection. |

Reserve/reuse creates or reuses the measurement in the same short transaction as F03's ledger
decision. Success/refund finalizes that same row atomically with F03 settlement. An idempotent
quota-denial retry reuses its one measurement but does not create an F03 usage event. A late
correction changes/reconciles the original measurement once; it never creates a second counted row.

### `ai_monitoring_rules` — optimistic ADMIN configuration

| Field group | Rules |
| --- | --- |
| Identity/scope | UUID; optional plan-policy and capability scope; no learner scope. |
| Condition | Metric type `TOKEN_VOLUME`, `ESTIMATED_COST`, `REQUEST_VOLUME`, `QUOTA_DENIAL_RATE`, `FAILURE_RATE` or `RESPONSE_TIME`; comparison and validated non-negative threshold. |
| Window | Exactly one rolling evaluation window: one hour or 24 hours. |
| Lifecycle | Enabled/disabled; optimistic version; actor/time for creation and last update. Disabling preserves history. |

### `ai_monitoring_alerts` — current breach lifecycle

| Field group | Rules |
| --- | --- |
| Identity | UUID and rule FK. At most one open/acknowledged alert for the same rule/scope/window breach. |
| State | `OPEN`, `ACKNOWLEDGED` or `RESOLVED`; first-seen, last-evaluated, latest measured value and data-coverage/partial state. |
| Acknowledgement | Optional current ADMIN actor, time and bounded operational note; acknowledgement never disables the rule or stops evaluation. |
| Integrity | Scheduled evaluation uses a unique breach key/locking so repeated runs update rather than duplicate an alert. A resolved alert remains immutable history. |

### `ai_admin_audit_events` — append-only privileged evidence

Store policy revision/publication/retirement, monitoring-rule changes and alert acknowledgements.
Each event has actor, action, target type/id, correlation, timestamp, success outcome, safe
before/after configuration summary, bounded reason/note and no learner/provider private material.
It is written in the same transaction as an accepted configuration/acknowledgement command and
retained under the canonical privileged-audit policy.

## Query constraints and indexes

- Policy versions: unique `(subscription_plan_id, revision_number)` and a guarded single-current
  published revision per plan.
- Allowance cycles: unique current cycle per entitlement; indexes by entitlement/status and cycle
  boundary; usage events index their cycle/policy/finalization state.
- Measurements: unique deduplication key; indexes on finalization/occurrence time, policy version,
  capability and final outcome to support the bounded report/alert aggregates.
- Monitoring rules/alerts: index enabled rules by window/scope and active alerts by
  rule/state/last-evaluated. Audit events index target/time for safe policy-history reads.

All report queries use JPA projections and approved filter/sort allowlists. They group records by
policy, capability, final outcome and cost currency; they do not query or return user/account data.

## State transitions

```text
policy: PUBLISHED --new revision--> SUPERSEDED
policy: PUBLISHED --retire if valid--> RETIRED

allowance cycle: CURRENT --server rollover under entitlement lock--> CLOSED
                 CLOSED  --new selected policy snapshot--> next CURRENT

measurement: PENDING --> SUCCEEDED
                     --> FAILED_REFUNDED
                     --> QUOTA_DENIED

alert: OPEN --> ACKNOWLEDGED --> RESOLVED
       OPEN ------------------> RESOLVED
       ACKNOWLEDGED --continuing breach--> ACKNOWLEDGED
```

No F12 state transition grants/revokes an individual entitlement, moves a learner into Premium,
charges a payment method, resets an active cycle or exposes a learner-private record.
