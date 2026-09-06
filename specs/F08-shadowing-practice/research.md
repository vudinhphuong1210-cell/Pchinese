# Research: F08 Shadowing Practice

## Decisions

| Topic | Decision | Rationale |
| --- | --- | --- |
| Eligibility | Only a currently unlocked or completed lesson segment can be recorded or assessed. | Prevents F08 from bypassing F05/F06 access gates. |
| Recording upload | Browser uploads multipart audio to Spring Boot. Spring validates ownership, segment context, MIME, size and scan state. | The browser never receives object-store authority. |
| Assessment handoff | Spring Boot streams only validated audio and minimal context to the private ai-service assessment endpoint. | Keeps authentication, storage and quota authority in Spring Boot. |
| AI orchestration | ai-service uses a dedicated speech provider; Mastra only orchestrates the call and validates the contract. | Mastra is not a source of truth and must not retain learning data. |
| Result | Persist numeric dimensions and encrypted concise feedback; do not retain a transcript. | Gives useful coaching while minimizing sensitive learner speech data. |
| Quota transaction | Reserve allowance in a short transaction, call AI outside it, then settle success or refund failure exactly once. | Avoids long database locks and double charges. |
| Retention | Successful assessment recordings expire after 30 days; failed/quarantined recordings after 24 hours. | Matches the project privacy policy while preserving short-term review value. |

## Lifecycle Rules

Recording status is UPLOADING, SCANNING, AVAILABLE, QUARANTINED, or DELETED. Only AVAILABLE recordings may start an attempt.

Attempt status is PROCESSING, COMPLETED, FAILED, or EXPIRED. A recording may be associated with only one assessment attempt. Repeated clientRequestId with the same request fingerprint returns the original attempt; a different fingerprint returns conflict.

## Private Contract

Spring Boot invokes POST /internal/v1/shadowing/assess with a correlation ID, replay-protected signature, segment reference text, allowed language metadata, and validated audio stream. The request never includes a browser JWT, object key, signed URL, entitlement record, or raw database model.

The response is a bounded structured assessment: overall score, pronunciation/rhythm/fluency dimensions, concise learner feedback, and provider request ID. Invalid, oversized, unsafe, or timed-out results are rejected before persistence.

## Rejected Alternatives

- Browser directly invokes a speech provider or ai-service: leaks authority and bypasses F03.
- Persisting a transcript for every recording: conflicts with privacy-by-default and is not necessary for this feature.
- Holding a database transaction through the provider call: creates contention and failure risk.
- Allowing a recording from any lesson segment: bypasses course entitlement.

