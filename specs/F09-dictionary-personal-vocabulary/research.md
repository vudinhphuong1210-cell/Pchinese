# Research: F09 Dictionary and Personal Vocabulary

## Decisions

| Topic | Decision | Rationale |
| --- | --- | --- |
| Public-search source | Read the F04-owned `dictionary_entries` projection only when its current publication state is `PUBLISHED`; independently test related media availability. | Shared editorial state remains authoritative and a stale search projection cannot disclose withdrawn content. |
| Search lookup | Materialize `dictionary_search_keys` from normalized Hanzi/pinyin substrings and Vietnamese keywords. Use an exact indexed key lookup and server-owned ranking. | Supports the required 1-code-point and any-position matching without accepting arbitrary client sort or wildcard expressions. |
| Search ranking | For Hanzi and pinyin, rank whole-field before prefix before other substring; use a stable entry-ID tie-breaker. Pinyin rank precedes a same-query Vietnamese-keyword match. | Gives predictable relevant results and makes pagination deterministic. |
| Query handling | Trim, require 1–120 Unicode code points before normalization, then classify Chinese-script versus Latin/Vietnamese input. Use page 0 and size 20 by default; size is 1–50. | Matches the specified validation and blocks accidental browse-all requests. |
| Saved-word identity | Keep one stable `saved_words` record per learner and dictionary entry, enforced by a database uniqueness constraint. | Supports idempotent saves, restoration and exactly one F10 schedule. |
| Vocabulary recency | `saved_at` is set on first save and each restoration; note edits and duplicate active saves leave it unchanged. Order ties by saved-word UUID. | Implements the agreed Free replacement and Premium-expiry ordering reproducibly. |
| Free capacity | Serialize each learner's vocabulary mutation through a locked user row. At capacity, remove the oldest active record and activate the requested record in the same transaction. | Prevents concurrent requests from exceeding 20 or evicting two words. |
| Premium expiry | F03's internal lifecycle changes the effective entitlement to Free, then invokes the F09 capacity command before commit. A bounded reconciler uses that same command for inactive learners. | A plan change has a deterministic, server-owned effect without creating an upgrade or payment UI. |
| F10 boundary | F09 calls internal F10 commands to ensure, suspend or restore a schedule. It never reads/writes F10 tables directly. | Preserves F10 ownership of due state, interval and immutable review history. |
| Note concurrency | Return JPA `version`; require `expectedVersion` with every note edit and return `409 STATE_CONFLICT` when stale. | Prevents a later device from silently overwriting a current note. |
| Note protection | Add a vocabulary-specific cipher facade over the existing per-user envelope-key facility; encrypt the plaintext before persistence and never log note plaintext/ciphertext. | Meets privacy requirements without putting conversation-specific code in the vocabulary module. |
| Performance verification | A separately invoked Playwright performance project runs 100 browser contexts against 100,000 published entries after 2-minute warm-up, then measures 10 minutes. | The acceptance measure includes visible rendering, network transit and correct empty states, not only database timing. |

## Search-index alternatives

### Rejected: only `LIKE` or only a trigram index

The required input can be one or two Unicode code points. PostgreSQL documents that trigram indexes support `LIKE`/`ILIKE` substring searches, but a pattern with no extractable trigrams can degrade into a full-index scan. A trigram-only design therefore leaves the most common short Hanzi and pinyin input outside the performance design. [PostgreSQL `pg_trgm` documentation](https://www.postgresql.org/docs/18/pgtrgm.html)

### Chosen: exact generated search keys

The dictionary projector emits one exact lookup key for every contiguous normalized Hanzi/pinyin query candidate, consolidating duplicate candidates for one entry to its strongest rank. Vietnamese senses contribute normalized keyword keys rather than arbitrary interior substrings. The `dictionary_search_keys` index starts with query kind and normalized key, so query values are bound parameters and exact index lookups select candidates before joining a published entry. Reprojection occurs whenever F04 changes a dictionary entry's searchable content or publication state.

This consumes more storage than a single text index, so the migration must be benchmarked with the 100,000-entry fixture and the production-like database configuration. It is intentional: predictable short-query latency has priority over minimizing this derived projection.

## Transaction and lock design

Spring Data JPA supports declarative lock metadata on repository query methods, so the feature uses repository-level pessimistic write locks for the user/entitlement serialization boundary and optimistic `@Version` checking for a single saved-word note update. [Spring Data JPA locking reference](https://docs.spring.io/spring-data/jpa/reference/3.4/jpa/locking.html)

Every F09 capacity-sensitive operation observes this order:

1. Lock the learner's `users` row.
2. Lock and reconcile the learner's effective entitlement.
3. Lock affected saved words in UUID order.
4. Invoke F10 commands, which lock affected schedules in UUID order.

The Free save/restore, automatic removal and F10 suspension/restoration commit as one transaction. The uniqueness constraint is a final guard against duplicate `(user_id, dictionary_entry_id)` rows. If the target entry is unpublished, missing or unavailable for saving, the request fails before any capacity removal. A failed request leaves the active vocabulary unchanged.

Premium expiry follows the same ordering in F03: lock user, lock Premium entitlement, mark it expired, establish/reuse current Free entitlement, invoke F09 capacity enforcement, then commit. The lifecycle command is idempotent; retries after the transition find no active Premium entitlement and do not remove more words.

## Retention and observability

Removed words remain recoverable product data: the record, encrypted note and F10 history are retained while the learner account remains within the applicable retention period. Account deletion and the 12-month inactivity baseline are handled by the existing learner-data lifecycle, which must delete/crypto-erase the related private data according to the root privacy matrix. F09 introduces no new retention exception.

Operational logs and metrics contain correlation IDs, opaque entity IDs, query type/length buckets, page size, timing and outcome only. They do not contain raw queries, Vietnamese meanings, personal notes, ciphertext, schedule history or entitlement references.

## Rejected alternatives

- Evicting the oldest word in React: client state can be stale and cannot serialize concurrent saves or entitlement expiry.
- Counting `DELETED` words toward Free capacity: conflicts with the agreed 20 active-word capacity and makes restoration impossible without needless eviction.
- Resetting `saved_at` after a note edit: would silently change the agreed eviction order.
- Returning the latest note in a `409` response: exposes private data more broadly than necessary; the owner can reload it through the normal owner route.
- Deleting a saved-word record or F10 schedule on capacity removal: destroys a note and learning history required for restoration.
- Calling an F10 HTTP endpoint from F09: creates an unnecessary public network boundary inside the modular monolith and weakens transaction control.
- Adding payment, upgrade or manual Premium operations: F03 explicitly excludes them from the MVP scope.
