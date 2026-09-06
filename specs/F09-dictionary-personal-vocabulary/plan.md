# Implementation Plan: F09 Dictionary and Personal Vocabulary

**Branch**: F09-dictionary-personal-vocabulary | **Date**: 2026-09-06 | **Spec**: spec.md
**Input**: Feature specification from D:\schinese\specs\F09-dictionary-personal-vocabulary\spec.md

## Summary

Deliver a published dictionary search and entry-detail experience plus a private personal vocabulary list. Spring Boot is the sole authority for publication visibility, ownership and saved-word state. Adding, deleting and restoring a word integrates atomically with F10 spaced repetition; F09 has no AI or allowance dependency.

## Technical Context

| Area | Decision |
| --- | --- |
| Public API | Spring Boot routes under /api/v1 with the standard envelope and typed React clients |
| Dictionary visibility | Search and detail expose PUBLISHED entries and only published/available media assets |
| Search | Simplified and Traditional Chinese, tone/case/space-insensitive pinyin, and Vietnamese keyword search |
| Personal data | Exactly one saved-word relation per user and dictionary entry; note is encrypted plain text and bounded to 500 characters |
| SRS integration | First save creates or reuses an F10 schedule; deletion suspends it; restoring reactivates it without losing schedule/history |
| Security | Owner-only saved word routes; an unpublished source remains unavailable even when its historical saved-word relation exists |
| AI | No browser-to-AI, ai-service, Mastra, usage reservation, or F03 allowance is involved |

## Constitution Check

| Principle | Status | Evidence |
| --- | --- | --- |
| Backend authority | Pass | Spring Boot enforces publication, user ownership and F10 lifecycle transitions. |
| Privacy | Pass | Personal note is encrypted and never visible to other users. |
| Content protection | Pass | Withdrawn entries and unavailable assets are omitted from public projections. |
| Predictable client/server contract | Pass | Server owns normalization, pagination and SRS state; client does not calculate review schedules. |

## Project Structure

### Documentation

    specs/F09-dictionary-personal-vocabulary/
    ├── plan.md
    ├── research.md
    ├── data-model.md
    ├── quickstart.md
    └── contracts/
        └── f09-api.md

### Source Code

    backend/
    ├── src/main/java/.../dictionary/
    ├── src/main/java/.../vocabulary/
    └── src/main/resources/db/migration/
    frontend/
    └── src/features/dictionary/

## Implementation Phases

1. Add normalized dictionary-search fields and encrypted saved-word note persistence.
2. Implement published-only dictionary search/detail and asset projection.
3. Implement idempotent save, update-note/status, delete and restore endpoints.
4. Invoke F10 scheduling commands transactionally from personal vocabulary lifecycle changes.
5. Build React search, entry detail, saved-word and due-review entry points.
6. Test search variants, ownership, content withdrawal, SRS preservation, pagination and validation.

## Complexity Tracking

No constitution exceptions are required.

