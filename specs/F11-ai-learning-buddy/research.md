# Research: F11 AI Learning Buddy

## Decisions

| Topic | Decision | Rationale |
| --- | --- | --- |
| Scenario | Conversation creation requires exactly one immutable scenario: DAILY_CONVERSATION, VOCABULARY_GRAMMAR or ROLE_PLAY. | Makes teaching intent explicit and prevents a generic unrestricted chatbot. |
| Content | Learner input is plain text of at most 1000 characters. | Bounds privacy, prompt surface and API complexity. |
| Pre-AI safety | Reject unsafe, out-of-scenario or malformed input before message persistence, allowance reservation or provider call. | Ineligible requests must cost nothing and leave no conversation data. |
| Context | At most 10 most recent COMPLETE messages from the same active conversation. | Prevents cross-conversation leakage and controls request size/cost. |
| Handoff | Spring Boot sends vetted minimal context to private ai-service. | ai-service does not need user identity token, entitlement, database rows, or full history. |
| Output | Required Chinese response plus concise Vietnamese explanation and no more than one suggestion. | Keeps feedback learning-oriented and renderable under a strict schema. |
| Failure | Pending assistant reply becomes FAILED; allowance reservation is refunded exactly once. | Gives a recoverable user experience and correct F03 accounting. |

## Conversation and Message Rules

Conversations are ACTIVE or DELETED. Delete immediately hides the conversation from standard list and read endpoints. Messages are PENDING, COMPLETE, FAILED or DELETED and have a server-assigned strictly ordered sequence in one conversation.

Only COMPLETE messages from the same conversation can enter the model context. PENDING, FAILED and DELETED messages are excluded. A user may rename a conversation title but cannot change its scenario.

## Private ai-service Boundary

Spring Boot calls POST /internal/v1/ai-buddy/respond with correlation ID, signed replay-protected metadata, scenario, current safe learner content, and at most ten completed contextual messages. ai-service uses Mastra to orchestrate the provider and validates that its output meets the fixed schema.

ai-service must not offer public routes, Mastra Studio exposure, durable memory, a database, or logs/traces containing raw learner messages. It returns no quota decision and never calls Spring Boot persistence.

## Idempotency and Allowance

The public send request requires clientRequestId. The identical request returns its original pending/completed/failed projection. A changed request under the same ID returns conflict. Spring Boot reserves F03 allowance with feature AI_BUDDY, calls ai-service outside a long database transaction, validates output, writes the assistant message and settles success; terminal failure refunds the same reservation once.

## Rejected Alternatives

- Browser calls ai-service or model provider: bypasses authentication, safety and F03.
- Mastra memory retains conversation history: duplicates protected data outside the system of record.
- Sending all conversation history: increases cost and risk and violates the bounded-context decision.
- Persisting invalid/unsafe input for analysis: violates the reject-before-persist policy.

