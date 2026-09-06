# ai-service Agent Guide

**Project**: Pchinese private AI supporting service  
**Status**: Available only for approved AI feature work

## Read First

Before changing this service, read these documents in order:

1. `../AGENT.md` -- project operating rules and Definition of Done.
2. `../CONSTITUTION.md` -- binding project law.
3. `../CLAUDE.md` -- system architecture, AI-service boundary and product flows.
4. The approved feature specification, especially `../specs/F11-ai-learning-buddy/spec.md` for
   AI Buddy work.
5. This file, then `CLAUDE.md` and `CONSTITUTION.md` in this directory.

The root documents are authoritative. This directory narrows their rules for `ai-service`; it
never relaxes them.

## Service Role and Technology

`ai-service` is the sole approved supporting-service exception to Pchinese's modular monolith.
It is a private Node.js + TypeScript service using Mastra to orchestrate approved LLM and speech
providers. It owns prompts, agents, workflows, model selection, provider adapters and typed safe
output. It is not a public product API, an independent domain service or a product-data store.

- Use Node.js, TypeScript strict mode, npm and Mastra only for approved AI feature work.
- Start with one private endpoint and one approved feature agent. Do not scaffold later feature
  folders or agents before their feature specification is approved.
- Approved private endpoints are `POST /internal/v1/ai-buddy/respond` for F11 and
  `POST /internal/v1/shadowing/assess` for F08. Each is reachable only from Spring Boot on the
  private network and has an independent typed contract.
- Validate environment variables at startup. Real credentials belong only in deployment-managed
  secrets; `.env.example` contains names and safe placeholders only.

## Authority Boundary

| `ai-service` MAY own | Spring Boot exclusively owns |
| --- | --- |
| Prompts, agents, workflows, model selection, safety guards and schema-validated explanations | Authentication and session validation |
| Private provider calls and provider-specific failure normalization | Learner ownership, `ADMIN`, entitlement, AI quota, rate limits and idempotency |
| Typed, minimized response objects for approved internal requests | PostgreSQL persistence, encryption, deletion, scores, SRS scheduling, HSK classification, lesson access and progress |

`ai-service` MUST NOT calculate or persist authoritative Dictation scores, pronunciation scores,
SRS schedules, HSK levels, access decisions, quotas or progress. A dedicated speech engine is
required for real audio-pronunciation assessment; Mastra may orchestrate it and explain the
result, but does not assess audio pronunciation itself.

## Private-Access and Data Rules

- Accept only private service-to-service requests authenticated by HMAC or mTLS, with replay
  protection. Never accept a browser JWT.
- Trust Spring Boot only after authenticating the internal request. Accept the minimum validated
  context required by its typed contract; never query Pchinese product tables or databases.
- For F08, receive only the recording bytes Spring Boot streams after ownership, status and malware
  validation. Never receive or use object-storage credentials, an object key or a signed URL.
- Do not expose Mastra default routes, agent/workflow routes, Studio, provider endpoints or any
  public listener to the Internet.
- MVP operation is stateless for learner data. Do not enable Mastra Memory or persist learner
  conversations, prompts, attempts, recordings, speech transcripts or product data in this service.
- Never commit, return, log or place in traces: provider credentials, HMAC material, browser JWTs,
  raw learner-private content, recordings or unredacted prompts. Evaluation fixtures use only
  approved non-private synthetic data.
- Validate external and internal input, constrain provider output with explicit schemas, and
  return controlled errors without provider internals or stack traces.

## Required Engineering Practice

- Keep request and response contracts typed and versioned with Spring Boot; schema-validate every
  request before dispatch and every agent result before it is returned.
- Preserve correlation IDs and safe operational telemetry, with redaction before logging or
  tracing.
- Treat provider failure, timeout, malformed output and safety rejection as expected outcomes.
  Return a safe internal error that lets Spring Boot map the public response.
- Keep provider adapters isolated from feature agents and keep safety guards independent of the
  provider implementation.
- Do not add data persistence, a public API, a second service, a queue, cache or new provider
  capability without an approved feature specification and any required Constitution amendment.

## Definition of Done

- [ ] The approved feature specification and this service's responsibility boundary are met.
- [ ] HMAC/mTLS authentication, replay protection, request validation and private-network
  exposure are verified.
- [ ] The Spring Boot <-> `ai-service` contract is typed, schema-validated and covered by
  contract tests.
- [ ] Agent input and output safety controls, provider timeout/failure handling, and redacted
  logging are tested.
- [ ] Quota reservation and idempotency remain Spring Boot responsibilities and are represented
  correctly in the internal contract.
- [ ] Unit, integration and non-private evaluation tests pass; linting, formatting and TypeScript
  compilation have no errors.
- [ ] No secret, raw learner-private data, debug code or unapproved persistent state exists.

## Git and Review

Use the project branch and commit conventions from `../CONSTITUTION.md`. Reviewers MUST confirm
that a change neither grants this service product authority nor weakens its private data boundary.
