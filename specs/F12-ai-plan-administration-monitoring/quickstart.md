# Quickstart Validation — F12

## Prerequisites

1. The F12 feature-map amendment, F03 scope/contract revision and `DATA_short.md` schema revision
   are approved together. The clean database contains the consolidated F03/F12 migration and the
   stable `FREE` and `PREMIUM` plan identities.
2. An active, server-authorized ADMIN account and a non-ADMIN learner account exist. F03 allowance
   services and deterministic F08/F11 telemetry fixtures are available; no test uses a live AI
   provider.
3. Use the public contract in [f12-openapi.yaml](./contracts/f12-openapi.yaml), the private
   telemetry contract in [f12-private-telemetry.md](./contracts/f12-private-telemetry.md), and the
   state rules in [data-model.md](./data-model.md).

## Validation scenarios

1. **Admin policy revision**: Read the current Free and Premium policies. Publish a valid Premium
   revision with price, currency, interval, benefit list, availability and allowance data. Verify a
   new immutable revision and safe audit event exist; no billing, learner upgrade or entitlement
   assignment occurs.
2. **Prospective Free quota**: Publish a new Free policy. Verify a newly activated eligible learner
   receives the new policy immediately. Verify a learner already in a current allowance cycle keeps
   its old quota/policy until locked rollover creates the next cycle; no counter reset occurs.
3. **Policy integrity**: Submit a stale expected version, invalid price/quota/period and retirement
   of the final published Free policy. Each attempt must produce the documented validation or `409`
   outcome with no new revision, audit success or learner change.
4. **Aggregate usage and privacy**: Seed successful, quota-denied, refunded and idempotently
   retried F08/F11 operations, including provider metering present, metering absent and two cost
   currencies. Request a UTC report within 50 days. Verify one count per logical operation,
   separate currency totals, unavailable-not-zero metering, partial/freshness status and no
   learner/event/content/provider-private fields.
5. **Range and access enforcement**: Request a range exceeding 50 days and a range older than four
   months; expect a recoverable validation result. As a learner or expired/locked account, call
   every F12 endpoint and verify safe `401`/`403` responses with no data disclosure.
6. **Monitoring lifecycle**: Create one-hour and 24-hour rules for token volume, cost, request
   volume, quota denial, failure rate and response time. Seed an eligible threshold breach and
   verify the evaluator produces an in-dashboard alert within five minutes. Acknowledge it with a
   bounded note; verify the audit record, continuing evaluation and eventual resolution after the
   breach clears. Confirm no email/chat/pager is sent.
7. **Frontend accessibility and recovery**: As an ADMIN, open the single AI Administration
   workspace from the ADMIN navigation. Exercise keyboard form entry, visible focus, validation
   errors, empty state, unavailable-metering/partial-data state, report filters, stale conflict
   reload and alert acknowledgement. Verify a non-ADMIN has no usable navigation or server access.

## Commands

Run the applicable tests and build after implementation:

```powershell
Set-Location backend
.\mvnw.cmd test
.\mvnw.cmd verify

Set-Location ..\frontend
npm test -- --runInBand
npm run lint
npm run build
```

Run migration integration coverage against a clean PostgreSQL-compatible database. Contract tests
must exercise both public envelope/error responses and malformed/missing private telemetry without
calling a real provider.
