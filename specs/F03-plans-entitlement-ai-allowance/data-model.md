# Data Model — F03 Plans, Entitlement, and AI Allowance

| Entity | Canonical F03 rule |
| --- | --- |
| `subscription_plans` | Active Free seed has `ai_quota_units=30`, `ai_quota_period=MONTH`, and configured device policy; Premium is reserved only. |
| `user_entitlements` | One ACTIVE entitlement per user; `ai_quota_period_started_at`, `ai_used_units`, cycle/status/version and user composite ownership constraints. |
| `ai_usage_events` | Owner, entitlement, client_request_id, fingerprint hash, `AI_BUDDY` or `SHADOWING_ASSESSMENT`, status and refund/success correlation. |

~~~text
eligible account -> ACTIVE Free entitlement
eligible AI request -> RESERVED -> SUCCEEDED
post-reservation invalid/timeout/provider/safety failure -> FAILED_REFUNDED (exactly once)
same request ID + same fingerprint -> existing event/result
same request ID + different fingerprint -> conflict/no mutation
~~~

Lock/unlock does not alter entitlement cycle or usage. Only the entitlement service locks/rolls the
active row and persists allowance events; client input never carries remaining units, plan state,
cycle dates or an authoritative score.
