# Implementation Plan: Dictation Practice

**Branch**: F07-dictation-practice | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

## Summary

Implement Dictation inside the Lesson Player for current-unlocked or completed segments. Spring Boot
validates context, evaluates normalized Simplified Hanzi deterministically, retains owned attempts and
updates best Dictation metric only. It does not call ai-service, consume AI allowance or change
progress unlock/completion.

## Technical Context

**Stack**: Java 21/Spring Boot/JPA/PostgreSQL; React 18/JavaScript/JSX
**Storage**: dictation_attempts, segments answer key, lesson_progresses best-score field  
**Dependencies**: F01 identity, F04 content, F06 practiceability/progress service  
**Testing**: deterministic evaluator unit tests, transaction/idempotency integration, Player Jest/E2E  
**Scope**: in-player attempt lifecycle/history; excludes AI, transcript reveal and segment completion.

## Constitution Check

| Gate | Result | Response |
| --- | --- | --- |
| Server score/ownership authority | PASS | Backend loads expected answer/context and calculates result. |
| AI boundary | PASS | No ai-service call, provider dependency or allowance event. |
| Privacy | PASS | Answer/feedback encrypted and owner-only; expected answer never returned automatically. |
| Integrity | PASS | Attempt state/idempotency and best-score update are transactional. |

## Project Structure

~~~text
backend/src/main/java/net/pchinese/dictation/{api,application,domain,persistence}/
frontend/src/features/{player,dictation}/
frontend/src/api/dictation.js
specs/F07-dictation-practice/{plan,research,data-model,quickstart}.md
specs/F07-dictation-practice/contracts/f07-api.md
~~~

## Implementation Approach

1. Start/reuse only one IN_PROGRESS attempt for an owned, published Dictation/BOTH segment that is
   current unlocked or already completed; reject future locked segment before creating data.
2. Normalize Unicode, whitespace and punctuation and compare the final Simplified Hanzi answer in
   Spring Boot. Use exact final-answer success (100) or retry-needed result (0), rather than
   character edit-distance penalties; retakes let learners correct and improve.
3. Submit with clientSubmissionId. Same logical submit returns existing evaluation; a new retake is
   allowed only after evaluated attempt and creates a new history record.
4. Return only overall score, accuracy and general guidance. Never return per-character diagnostics
   or full expected answer automatically.
5. Update dictation best score only when higher via the Progress application service; do not change
   current segment, completion count or lesson status. Update F07 spec, DATA_short.md and API.md
   together to replace the superseded edit-distance text.

## Complexity Tracking

None.
