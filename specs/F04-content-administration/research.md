# Phase 0 Research — F04 Content Administration

## Decision: ADMIN and public projections are separate

**Decision**: ADMIN reads return typed, safe, bounded management projections. Public catalog
projections remain a later-feature consumer of only the valid published-Free query specification.
Management projections include only the current content fields required to administer the item,
current state/version, pagination data, and request correlation; they never return persistence
entities, learner data, actor IDs, storage locators, raw provider URLs, or scanner diagnostics.

**Rationale**: Draft content and safe learner discovery have different authorization and disclosure
requirements. Separate projections support conflict reload without turning internal persistence into
the API contract.

**Alternatives considered**: Reusing public projections for drafts, returning entities, or filtering
drafts in the frontend were rejected because they weaken server authority and safe disclosure.

## Decision: learner-visible content has a closed lifecycle guard

**Decision**: A lesson publishes only when its parent topic is published, it has one or more
segments, every associated segment is published in its required order, every associated media asset
is approved, and access is Free. A segment/order/media change is rejected while the owning lesson is
published. Archive is terminal; invalid commands do not change live content.

**Rationale**: Checking only a non-empty subset of published segments can leave draft or retired
segments inside a learner-visible lesson. Rejecting live structural changes keeps the required
unpublish-edit-republish workflow explicit.

**Alternatives considered**: Checking only published segments, silently withdrawing on every change,
direct live editing, and archive restore were rejected.

## Decision: YouTube-only video-reference intake and terminal safety states

**Decision**: MVP media intake accepts exactly one YouTube video reference: a canonical 11-character
video ID or an HTTPS YouTube watch, share, or embed URL. The backend parses the reference without
following redirects, stores provider `YOUTUBE` and the canonical video ID only, and ignores incidental
playlist/radio query parameters when a supported watch URL has exactly one valid `v` video ID. It rejects
local files, downloads, HTTP, untrusted hosts, playlist/channel/short URLs without a supported video ID,
malformed links, and arbitrary iframe markup. Valid intake starts pending review, and approval requires a clean trusted-source
verification result. The transition matrix is PENDING_SCAN to APPROVED, REJECTED, or QUARANTINED;
APPROVED to REJECTED or QUARANTINED; rejected and quarantined are terminal. An
approval-to-rejected/quarantined transition atomically withdraws every dependent published lesson.

**Rationale**: A browser-supplied URL is not a safe playback source. Canonicalization blocks URL
confusion and avoids storing or rendering arbitrary links while allowing Admins to paste the normal
YouTube link they already have. There are no video bytes to upload, download, checksum, or malware
scan; the retained review state represents trusted-source verification or human review.

**Alternatives considered**: Local file upload, multiple provider allowlists, immediate approval,
arbitrary URLs/iframes, direct provider/browser uploads, and re-approving rejected or quarantined
assets were rejected.

## Decision: fixed provider embedding belongs to the learner player

**Decision**: F04 stores only the validated canonical YouTube video ID. The later learner-player
feature derives an official fixed embed URL from that ID, rather than accepting or replaying the
submitted URL. It uses a permitted YouTube embed with an accessible iframe, no autoplay, and a
provider-restricted frame policy.

**Rationale**: F04 is the ADMIN content workflow, not a learner-player feature. Separating source
validation from playback avoids unsafe raw URL rendering and keeps the provider's embed requirements
at the point where playback is introduced.

**Alternatives considered**: Embedding a browser-supplied URL, proxying video bytes through the
application, or adding learner playback to the Admin feature were rejected.

## Decision: canonical, append-only content audit

**Decision**: Define content_audit_events in DATA_short.md before adding one forward migration to the
canonical supabase/migrations flow. Each event stores a safe actor, action, target, outcome, reason,
expected/observed version, correlation, and explicit before/after state. The store has no mutation
interface, is insert-only for the application role, and rejects database update/delete attempts.
Accepted mutations, cascades, and their audit rows commit together. Authenticated rejected commands
retain one independent safe rejected audit event after the command itself rolls back.

**Rationale**: The runtime and clean tests provision only the canonical migration source. Application
convention alone cannot protect audit immutability, and auth/role audit cannot describe F04
content-lifecycle outcomes.

**Alternatives considered**: A second backend-only migration tree, a generic JSON-only audit,
application-convention-only immutability, and reusing auth audit were rejected.

## Decision: session-safe and accessible administration client

**Decision**: The Content/Media client uses the shared memory-session HTTP mechanism and never reads,
stores, or constructs access credentials. The UI uses semantic status tokens and real accessible tabs
and dialogs with keyboard navigation, focus management, labelled controls, touch targets, and
non-colour-only feedback.

**Rationale**: A feature-specific local-storage token fallback bypasses the shared session/refresh
contract. Status safety and destructive operations require accessibility beyond visual colour.

**Alternatives considered**: Browser token storage, feature-local refresh logic, colour-only state,
and unstructured overlays were rejected.
