# Implementation Plan: Course Catalog and Access

**Branch**: F05-course-catalog-access | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

## Summary

Expose a public catalog of publishable Free topic/lesson summaries and enforce server-side access
at every content boundary. Visitors can browse summaries; only signed-in learners enter Player,
playback or practice. No Premium upgrade, media URL, transcript or learner-private data is exposed.

## Technical Context

**Stack**: Java 21/Spring Boot/JPA/PostgreSQL; React 18/JavaScript/JSX/Vite; Flyway, Jest/JUnit
**Storage**: topics, lessons, segments, media_assets and existing lesson_progresses  
**Dependencies**: F03 Free entitlement, F04 publication/media lifecycle, F01 identity  
**Scope**: catalog query/detail/projections and unavailable handling; F06 owns playback/progress.

## Constitution Check

| Gate | Result | Response |
| --- | --- | --- |
| Server access authority | PASS | Backend evaluates publication/Free/access for every read. |
| Privacy | PASS | DTOs exclude transcript, playlist, provider URL, practice and private learner data. |
| Scope | PASS | No Premium journey, Player implementation, progress creation or content mutation. |
| Quality | PASS | Query/filter, direct-URL and content-withdrawal cases are tested. |

## Project Structure

~~~text
backend/src/main/java/net/pchinese/{learning,media,entitlement}/
frontend/src/{api,features/catalog,pages,routes}/
specs/F05-course-catalog-access/{plan,research,data-model,quickstart}.md
specs/F05-course-catalog-access/contracts/f05-api.md
~~~

## Implementation Approach

1. Centralize a publishable-Free predicate: PUBLISHED topic/lesson, valid segments and APPROVED
   media. Evaluate it again on every detail/resource request.
2. Provide bounded title search, HSK 1–6 filter and default sort order. Items without HSK appear
   only in All.
3. Return minimal summary DTOs for visitor and learner; do not attach owned progress to public
   catalog/detail responses. F06 returns owned progress separately.
4. Require sign-in for playback/Player/practice hand-off. Show a safe sign-in/unavailable state,
   never a Premium upgrade journey.
5. When content/media is withdrawn, block next access, retain existing progress and route learner
   back to catalog with a recoverable state.

## Complexity Tracking

None.
