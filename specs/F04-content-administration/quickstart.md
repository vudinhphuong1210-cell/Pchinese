# Quickstart Validation — F04

1. As non-ADMIN, attempt every write/draft-read route: expect safe 403.
2. Create topic, approved media, ordered segments and lesson. Publish in valid parent-child order and
   verify public catalog shows only the valid Free lesson.
3. Attempt publish with unpublished parent, zero/unpublished segment, pending/rejected media or
   Premium value: expect validation/state failure and no learner visibility.
4. Attempt stale expectedVersion and edit a published lesson/segment/media without unpublish: expect
   409/rejection and no live mutation. Unpublish, edit, validate and republish successfully.
5. Reject/quarantine media used by multiple published lessons: verify all dependent lessons become
   unavailable while learner progress is retained.
6. Archive each content type and verify no restore/republish path; inspect safe append-only content
   audit events.

Run clean Flyway integration tests, lifecycle/concurrency unit tests and frontend keyboard/focus,
conflict-reload and destructive-command tests.
