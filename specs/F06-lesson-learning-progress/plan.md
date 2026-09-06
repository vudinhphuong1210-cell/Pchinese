# Implementation Plan: Lesson Learning and Progress

**Branch**: F06-lesson-learning-progress | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

## Summary

Implement one authoritative progress aggregate per learner/lesson. A permitted Player lazily creates
progress; only validated automatic end-of-segment playback transitions advance ordered content.
Dictation and Shadowing may update practice metrics but never unlock/complete a segment.

## Technical Context

**Stack**: Java 21/Spring Boot/JPA/PostgreSQL; React 18/TypeScript/Vite  
**Storage**: lesson_progresses, lessons/segments/media; no new practice result table  
**Dependencies**: F01 identity, F03 access, F04 availability, F05 catalog  
**Testing**: JUnit/Mockito, integration/concurrency, Jest Player/E2E  
**Scope**: Player hand-off, watermark, automatic ordered completion and owned progress.

## Constitution Check

| Gate | Result | Response |
| --- | --- | --- |
| Aggregate authority | PASS | One backend progress service is sole transition authority. |
| Access/privacy | PASS | Player rechecks learner access/publication; only owned state is returned. |
| Integrity | PASS | Unique aggregate plus lock/version protects lazy creation and concurrent completion. |
| Quality | PASS | Watermark/seek/concurrent/final-segment cases are mandatory tests. |

## Project Structure

~~~text
backend/src/main/java/net/pchinese/progress/{api,application,domain,persistence}/
frontend/src/{api,features/progress,features/player,pages}/
specs/F06-lesson-learning-progress/{plan,research,data-model,quickstart}.md
specs/F06-lesson-learning-progress/contracts/f06-api.md
~~~

## Implementation Approach

1. GET playback rechecks access/publication, creates/reuses exactly one progress row only at Player
   entry and returns permitted current/completed segments plus server watermark.
2. Issue a short-lived playback capability bound to learner, lesson, current segment, content
   version and issue time. Require it for playback events; do not trust positionMs alone.
3. Progress event service accepts PROGRESS/ENDED only for current unlocked segment, advances bounded
   watermark and accepts ENDED only after validated playback reaches segment end.
4. In one transaction/version transition, increment contiguous completion once, unlock next sequence,
   reset current watermark or mark final lesson COMPLETED at 100 percent. Late concurrent event returns
   latest state.
5. Expose Dictation entry/context and Shadowing hand-off but give F07/F08 only a progress application
   method to record practice metrics, never completion fields.
6. Build Player loading/empty/unavailable/recoverable states; client cannot calculate unlock,
   watermark or completion.

## Complexity Tracking

The signed short-lived playback capability is necessary to make server-validated watermark meaningful;
it adds no new service or persistent event store.
