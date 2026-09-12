# Data Model — F03 Plans, Entitlement, and AI Allowance

| Entity | Canonical F03 rule |
| --- | --- |
| `subscription_plans` | Stable `FREE` and `PREMIUM` identities. The mutable catalogue/quota definition is the F12 `plan_policy_versions` snapshot. |
| `user_entitlements` | One ACTIVE entitlement per user and its selected policy version; no F12 route creates or edits a learner row. |
| `entitlement_allowance_cycles` | Current/closed policy snapshot, limit, period boundary and used-unit counter for one entitlement. |
| `ai_usage_events` | Owner-bound internal ledger event, allowance cycle/policy snapshot, client request id, fingerprint, capability, status and refund/success correlation. |

~~~text
eligible account -> ACTIVE Free entitlement + current published Free policy snapshot
eligible AI request -> RESERVED -> SUCCEEDED
post-reservation invalid/timeout/provider/safety failure -> FAILED_REFUNDED (exactly once)
same request ID + same fingerprint -> existing event/result
same request ID + different fingerprint -> conflict/no mutation
~~~

Lock/unlock does not alter entitlement cycle or usage. Only the entitlement service locks/rolls the
active row, selects a published Free policy at the cycle boundary and persists allowance events;
client input never carries remaining units, plan state, cycle dates or an authoritative score.
