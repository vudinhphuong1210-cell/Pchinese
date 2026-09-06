# Quickstart: F08 Shadowing Practice

## Prerequisites

- Start backend, frontend, ai-service and their approved private speech provider configuration.
- Seed an entitled learner, a published course, an unlocked lesson segment and a valid media capability flow.
- Configure private object storage and the malware/scan integration.

## Happy Path

1. Sign in as the learner and open an unlocked lesson's Shadowing screen.
2. Record a short permitted audio clip and upload it.
3. Wait until its server-side status becomes AVAILABLE.
4. Create an assessment attempt with a generated clientRequestId.
5. Observe PROCESSING, then COMPLETED with bounded score feedback.
6. Verify F03 has exactly one successful SHADOWING_ASSESSMENT usage for the attempt.

## Required Checks

- A future locked segment cannot be uploaded, recorded, or assessed.
- A different user receives 404 for recording and attempt resources.
- Quarantined, expired or deleted audio cannot be assessed.
- A retry with identical clientRequestId returns the same attempt without a second charge.
- Provider error ends FAILED and refunds its reservation exactly once.
- Database, API response and logs contain no transcript or object storage credential.
- Cleanup removes expired recording objects and makes their locators inaccessible.

