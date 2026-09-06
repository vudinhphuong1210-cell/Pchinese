# Quickstart: F11 AI Learning Buddy

## Prerequisites

- Start backend, frontend and the private ai-service with Mastra/provider configuration.
- Configure service-to-service mTLS or HMAC replay protection.
- Seed an authenticated learner with a usable F03 AI allowance.

## Happy Path

1. Create a DAILY_CONVERSATION conversation.
2. Send a safe plain-text message with a generated clientRequestId.
3. Observe a pending state, then one COMPLETE assistant response with Chinese reply, concise Vietnamese explanation and at most one suggestion.
4. Verify the next private request includes no more than ten prior COMPLETE messages from this same conversation.
5. Retry the identical send request and verify no second assistant reply or allowance usage is created.

## Required Checks

- A learner cannot list, read, rename, delete or send to another learner's conversation.
- The scenario cannot be changed after creation.
- Unsafe/out-of-scenario input produces no persisted message and no F03 reservation.
- Deleted, pending, failed and cross-conversation messages never enter model context.
- Invalid provider output is rejected and not persisted; allowance is refunded once.
- ai-service has no public route, database row, Mastra memory or raw-message log.
- F03 ledger has one successful AI_BUDDY usage only for each completed eligible learner request.

