# ai-service Architecture and Development Guide

## Purpose

`ai-service` is Pchinese's private Node.js + TypeScript Mastra integration boundary. It transforms
minimized, already-authorized internal requests into schema-validated AI explanations or feedback.
It is not a public API and does not own learner, product or business-rule data.

The root [project architecture](../CLAUDE.md) is canonical. This document records the implementation
rules for this service only.

## Canonical Project Contract

This document implements the root [AI service contract, safety and operations](../CLAUDE.md) for
the private service. Every change MUST preserve these project-level rules:

- Spring Boot authenticates the learner, checks ownership and entitlement, reserves quota, applies
  rate limits and chooses idempotency behaviour before the internal call. This service authenticates
  HMAC/mTLS requests and rejects invalid, expired and replayed traffic.
- Internal contracts are typed and versioned. They carry only a correlation/request ID, approved
  capability and minimized validated context; they never carry browser JWTs, provider credentials,
  mutable product authority or unrestricted learner history/audio.
- Responses contain only schema-validated explanations, feedback, safety status or safe internal
  failures. They never contain authoritative scores, schedules, quota decisions, progress changes,
  provider internals or secrets.
- Input guards run before provider dispatch; output safety/schema guards run before any response.
  Provider timeout, invalid output and safety rejection return safe typed failures for Spring Boot
  to map to the public error contract.
- MVP operation is one private container, one Mastra instance and no local learner-data persistence,
  product database, queue, cache or public ingress.

## Request Flow

```text
React SPA
  -> Spring Boot /api/v1/*
     authenticate, ownership, input safety, rate limit, quota reservation, idempotency
  -> private HMAC/mTLS request
     ai-service: validation, safety guards, Mastra agent/workflow, provider adapter
  -> typed schema-validated response
     Spring Boot: authoritative persistence and public response
```

React never calls this service, Mastra, an LLM provider or a speech provider directly. Spring Boot
never delegates product authority to this service.

## Responsibilities

### ai-service owns

- Prompts, feature agents, workflows and model selection.
- Provider adapters and normalized provider failures.
- Request validation, input and output safety guards, and schema-validated typed results.
- Secret-backed private integration with approved AI and speech providers.

### ai-service does not own

- Browser authentication, JWT/session validation, learner ownership, `ADMIN`, entitlement, AI
  quota, rate limiting or idempotency.
- Database access, product tables, persistent conversations, messages, attempts, recordings or
  progress.
- Dictation scores, pronunciation scores, SRS scheduling, HSK classification, lesson access,
  quota decisions or progress calculations.
- Public routes, public Mastra Studio, default agent/workflow routes, or provider credentials in
  a browser response.

## Internal Surface

The approved internal surface is deliberately limited to:

```text
POST /internal/v1/ai-buddy/respond
POST /internal/v1/shadowing/assess
```

The first route belongs only to F11 AI Buddy. The second belongs only to F08 Shadowing's dedicated
speech-engine assessment; it MUST NOT be multiplexed through the AI Buddy capability. Both routes
are private-network-only and callable exclusively by Spring Boot after a valid HMAC or mTLS
handshake with replay protection. Their requests and responses live in independent typed contracts.
The service validates the complete request schema before agent dispatch and validates the complete
output schema before responding. Spring Boot owns conversion of an internal failure to the public
`{ success, data, error, meta }` envelope.

The request contract MUST be versioned and include a correlation/request ID, approved feature
capability and only the minimized context the agent needs. It MUST NOT carry a browser JWT, provider
credential, unrestricted learner history/audio or a field that grants product authority. The response
MAY contain validated feedback, explanation, safety status or safe failure information only; Spring
Boot rejects invalid results and remains responsible for all persistence and public error mapping.

Do not create an endpoint for future Dictation, Shadowing, vocabulary or enrichment work until the
corresponding feature specification has been approved.

## Directory and File Structure

Build only the paths required by the approved feature. The following is the target structure;
paths marked `MVP` belong to the first AI Buddy slice. Do not create folders for later features
merely to make the tree look complete.

```text
ai-service/
|-- package.json                         # npm scripts and approved dependencies
|-- tsconfig.json                        # TypeScript strict configuration
|-- Dockerfile                           # private service container
|-- .env.example                         # names/placeholders only; never real secrets
|-- AGENT.md
|-- CLAUDE.md
|-- CONSTITUTION.md
|-- src/
|   |-- server.ts                         # MVP: private HTTP server only
|   |-- config/
|   |   |-- env.ts                        # MVP: validate runtime environment
|   |   |-- modelRegistry.ts              # model selection by feature/environment
|   |   `-- observability.ts              # redaction, traces and metrics
|   |-- http/
|   |   |-- routes/
|   |   |   |-- internalAiBuddy.route.ts  # MVP: POST /internal/v1/ai-buddy/respond
|   |   |   |-- internalDictation.route.ts # add only with approved Dictation AI feedback
|   |   |   `-- internalShadowing.route.ts # add only with approved Shadowing AI feedback
|   |   `-- middleware/
|   |       |-- internalAuth.ts           # MVP: HMAC/mTLS and replay protection
|   |       |-- requestValidation.ts      # MVP
|   |       `-- errorHandler.ts           # MVP: safe internal errors only
|   |-- mastra/
|   |   `-- index.ts                      # MVP: register only approved agents/workflows
|   |-- features/
|   |   |-- ai-buddy/
|   |   |   |-- chineseTutor.agent.ts     # MVP
|   |   |   |-- chineseTutor.prompt.ts    # MVP
|   |   |   |-- aiBuddy.schema.ts         # MVP: Zod request/output schemas
|   |   |   |-- aiBuddy.service.ts        # MVP
|   |   |   `-- aiBuddy.eval.ts           # MVP: synthetic, non-private evaluation cases
|   |   |-- dictation-feedback/           # add only with its approved feature
|   |   |   |-- dictationFeedback.workflow.ts
|   |   |   |-- dictationFeedback.prompt.ts
|   |   |   |-- dictationFeedback.schema.ts
|   |   |   `-- dictationFeedback.service.ts
|   |   |-- shadowing-feedback/           # add only with its approved feature
|   |   |   |-- shadowingFeedback.workflow.ts
|   |   |   |-- shadowingFeedback.prompt.ts
|   |   |   |-- shadowingFeedback.schema.ts
|   |   |   `-- shadowingFeedback.service.ts
|   |   |-- vocabulary-coach/             # add only with its approved feature
|   |   |   |-- vocabularyCoach.agent.ts
|   |   |   |-- vocabularyCoach.prompt.ts
|   |   |   `-- vocabularyCoach.schema.ts
|   |   `-- lesson-enrichment/            # add only with its approved feature
|   |       |-- lessonEnrichment.workflow.ts
|   |       |-- lessonEnrichment.prompt.ts
|   |       `-- lessonEnrichment.schema.ts
|   |-- adapters/
|   |   |-- llm/
|   |   |   `-- modelProvider.ts          # MVP: approved LLM provider configuration
|   |   |-- speech/
|   |   |   `-- speechAssessmentClient.ts # add only for pronunciation assessment
|   |   `-- pchinese/
|   |       `-- pchineseInternalClient.ts # allowlisted private backend tools only
|   |-- shared/
|   |   |-- contracts/
|   |   |   |-- internalRequest.ts         # MVP
|   |   |   |-- internalResponse.ts        # MVP
|   |   |   `-- errors.ts                 # MVP
|   |   |-- safety/
|   |   |   |-- inputGuard.ts              # MVP
|   |   |   `-- outputGuard.ts             # MVP
|   |   `-- utils/
|   `-- test/
|       |-- unit/
|       |-- integration/
|       `-- contract/
`-- evals/
    |-- ai-buddy/
    |   |-- hsk-scenarios.json            # synthetic data only
    |   |-- unsafe-input.json
    |   `-- vietnamese-feedback.json
    `-- shadowing-feedback/               # add only with its approved feature
```

Feature agents do not import HTTP authentication or provider-secret configuration directly.
Provider adapters do not make product decisions. HTTP middleware does not contain prompt logic.

## Security and Privacy

- Validate runtime configuration before serving traffic. `LLM_API_KEY`, HMAC material and mTLS
  keys/certificates are deployment secrets, never repository content.
- Authenticate internal callers with HMAC or mTLS, enforce replay protection and reject all
  browser JWTs.
- Never connect to Pchinese product tables or PostgreSQL. The request contains only minimized,
  validated context supplied by Spring Boot.
- For F08, receive recording bytes only through Spring Boot's private HMAC/mTLS stream after its
  ownership, status and malware validation. Do not receive object-storage credentials, an object
  key, signed URL or persistent recording reference.
- Keep the MVP stateless for learner data: Mastra Memory is disabled and no learner conversation,
  prompt, recording, speech transcript or result is persisted locally.
- Redact logs, traces and metrics. Do not log raw prompts, raw agent content, recordings,
  credentials, JWTs, HMAC signatures or provider request/response bodies.
- Do not expose Mastra Studio, default routes or provider diagnostic output outside the private
  service boundary.

## Provider and Output Rules

Use a provider adapter with explicit timeouts and safe failure classification. An agent may generate
an explanation or feedback only in the typed output schema approved for the feature. Reject output
that fails schema or safety validation; never return a best-effort unvalidated response.

For a timeout, unavailable provider, invalid result or safety rejection, return a typed safe internal
failure with the correlation ID and no stack trace, credential, prompt or provider diagnostic. Spring
Boot maps the failure to its standard public error contract, including `PROVIDER_ERROR` or
`SERVICE_UNAVAILABLE` where applicable.

Actual pronunciation scoring requires a dedicated speech provider. If a future approved Shadowing
feature uses one, this service may call it through `adapters/speech/` and return a validated
explanation. Spring Boot remains authoritative for the recorded attempt, score, entitlement, quota
and progress.

## Tests and Evaluations

- Unit-test HMAC/mTLS and replay checks, request parsing, safety guards, agents and provider-error
  normalization.
- Integration-test the private endpoint and provider adapter with a mocked provider.
- Contract-test every Spring Boot <-> `ai-service` request and response schema, including malformed
  input, malformed model output, timeout and safe failure paths.
- Keep evaluation data synthetic, non-private and free of provider secrets. Cover Vietnamese
  learner feedback, HSK scenarios and unsafe-input handling only when the approved feature needs
  them.
- Do not let an E2E test depend on nondeterministic provider output unless it runs in a dedicated
  controlled environment.

## Operational Rules

This service runs as one private container and one Mastra instance for the initial feature. It uses
one configured LLM credential unless a future approved specification changes that design. It must
not introduce a separate data store, queue, cache or public ingress. Spring Boot owns retries,
quota reservation and idempotency decisions; the service returns deterministic typed status data
that makes safe retry decisions possible.

A new route, agent, provider, memory/persistence capability or future feature directory requires an
approved feature specification that states the actor, access rule, minimized data, output schema,
failure handling, retention and tests.
