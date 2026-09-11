# Quickstart: F11 AI Learning Buddy

## Prerequisites

- Start backend, frontend and exactly one private ai-service container with strict TypeScript, Mastra,
  `@ai-sdk/deepseek`, and no public ingress. Set the real `DEEPSEEK_API_KEY` only in the deployment
  secret store and set `DEEPSEEK_MODEL` to `deepseek-v4-flash` or `deepseek-v4-pro`; startup refuses
  any other model or a missing privacy-policy value.
- Configure private-TLS HMAC-SHA-256 signing with five-minute timestamp/nonce replay protection and
  verify both services against `contracts/f11-hmac-test-vector.json`.
- Configure the deployment KMS envelope client and an active KMS key reference; no KMS or provider
  credential may be committed.
- Seed an authenticated learner with a usable F03 AI allowance.

## Happy Path

1. Create a DAILY_CONVERSATION conversation.
2. Send a safe plain-text message with a generated clientRequestId.
3. Submit the request and, within 30 seconds, observe one COMPLETE assistant response with Chinese reply, concise Vietnamese explanation and at most one suggestion; the UI does not poll or expose PENDING.
4. Verify the next private request includes no more than ten prior COMPLETE messages from this same conversation.
   It also contains only correlation/request IDs, fixed `AI_BUDDY` capability, scenario and bounded context.
5. Retry the identical send request while the original is in flight and verify it joins the original terminal result by the original 30-second deadline; verify no second assistant reply or allowance usage is created. A changed body with the same clientRequestId returns a conflict.
6. Make five distinct send attempts within one minute, then verify a sixth returns `429` with `Retry-After` and creates no provider call, allowance usage or message.
7. Mock an unsafe private request or provider result and verify ai-service returns a typed internal error
   with its correlation ID before provider dispatch or public persistence.
8. Mock a provider timeout and verify the provider is cancelled by 22 seconds, Spring Boot times out
   the private call by 25 seconds, and the learner receives a safe terminal response by 30 seconds.
9. Mock malformed or output-policy-v1-rejected provider output and verify the public response is `502 PROVIDER_ERROR` with one refund; mock a private-service or provider timeout and verify `503 SERVICE_UNAVAILABLE`, with no provider detail.
10. Delete one of two conversations owned by the learner and verify only its wrapped conversation
    DEK is destroyed, a content-free deletion attestation is written, and the other conversation
    remains readable.

## Required Checks

- A learner cannot list, read, rename, delete or send to another learner's conversation.
- The scenario cannot be changed after creation.
- Unsafe/out-of-scenario input produces no persisted message and no F03 reservation.
- Deleted, pending, failed and cross-conversation messages never enter model context.
- Invalid or output-policy-v1-rejected provider output is rejected and not persisted; allowance is refunded once.
- The HMAC test vector accepts byte-for-byte only; changed method, path, timestamp, nonce, hash,
  signature encoding or request bytes are rejected before dispatch.
- ai-service has no public ingress or route, database row, Mastra memory, raw-message log, cache or queue.
- ai-service accepts only the fixed `AI_BUDDY` capability and echoes the correlation ID in success and
  typed internal-error responses.
- F03 ledger has one successful AI_BUDDY usage only for each completed eligible learner request.
- A daily retention sweep creates one idempotency-keyed notice record 30 days before the `last_message_at`/`created_at` expiry, safely retries delivery, then destroys the affected wrapped conversation DEK, writes deletion attestation, and processes database-backed hard-delete work. An active legal hold blocks only that work and grants no chat access.
- Provider dispatch is refused without the approved provider policy; the processing audit captures
  only correlation, provider/model/region, safe outcome and hashed provider reference — never chat
  text, prompts, tokens, credentials, signatures or provider bodies.
