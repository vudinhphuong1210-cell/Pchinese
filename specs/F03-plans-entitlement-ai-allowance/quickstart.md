# Quickstart Validation — F03

1. Activate a new eligible learner and verify exactly one active Free entitlement is created/reused.
   F02's canonical GET `/api/v1/me` projection includes only the safe 30-unit/cycle entitlement
   fragment alongside the caller's profile.
2. Concurrently reserve two F08/F11-style requests at the remaining boundary. Verify at most the
   allowed count succeeds and exhausted requests return `429 AI_QUOTA_EXCEEDED`.
3. Retry the same request ID/fingerprint and verify one event/charge/result; retry with a different
   fingerprint and verify `409 IDEMPOTENCY_CONFLICT`, no private call and no quota mutation.
4. Force timeout, unavailable service, malformed result and safety failure after reserve. Verify
   exactly one refund, no valid feature outcome and safe public error.
5. Lock then unlock the learner via F01. Verify entitlement/cycle/used count is unchanged while
   account access remains blocked until a fresh sign-in.

Run backend unit/integration tests and frontend summary/error mapping tests against a clean migration
state; ensure no Admin endpoint/UI can change plan or quota.
