# F12 Private AI Telemetry Contract

## Boundary

This is an extension to the existing private Spring Boot → `ai-service` F08/F11 response contract.
It is not a browser API and does not create an `ai-service` database, public route, queue or
product authority. Spring Boot continues to authenticate the learner, validate safety/ownership,
reserve/settle F03 allowance, persist results and decide the F12 final operational measurement.

## Optional response addition

After the existing feature-specific output passes the `ai-service` schema, the private response may
include a `telemetry` object:

| Field | Producer and validation rule |
| --- | --- |
| `inputTokens`, `outputTokens`, `totalTokens` | Non-negative whole numbers from provider metering when supplied; absent when unavailable. Spring verifies consistency when more than one value is present. |
| `estimatedCost` and `costCurrency` | Non-negative decimal and uppercase ISO 4217 currency supplied together, or both absent. Spring does not estimate a missing cost. |
| `providerDurationMs` | Non-negative provider elapsed time when supplied; absent when unavailable. |
| `failureClass` | Bounded safe class only: timeout, unavailable, invalid-output or safety-rejected. It contains no provider body, stack trace or credential. |

Spring Boot separately measures its end-to-end duration. It treats any malformed, negative,
inconsistent or unexpected telemetry as unavailable (or as a safe provider-contract failure when the
feature result cannot be trusted); it never forwards telemetry to the learner.

## Lifecycle mapping

```text
F03 reserve/reuse ──same short transaction──> create/reuse PENDING measurement
quota denied      ──same short transaction──> create/reuse final QUOTA_DENIED measurement
validated success ──same settlement tx──────> SUCCEEDED measurement + used allowance
terminal failure  ──same settlement tx──────> FAILED_REFUNDED measurement + exact-once refund
retry             ──same logical key────────> same usage event/measurement, no second count
```

Only final measurements feed reporting and monitoring. A delayed/unfinalized measurement affects
the freshness/partial-data state, not a zero-valued aggregate. Private telemetry has no user ID,
browser JWT, raw prompt/reply/audio/transcript, provider credential, provider request/response body
or durable AI-service storage.
