# Feature Specification: Dictionary and Personal Vocabulary

**Feature Branch**: `[F09-dictionary-personal-vocabulary]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F09 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Visitor có thể tìm từ điển bằng những dạng truy vấn nào? → A: Hanzi giản thể/phồn thể, pinyin không phân biệt dấu thanh và từ khóa nghĩa tiếng Việt.
- Q: Khi learner đã remove một saved word rồi save lại cùng dictionary entry, hệ thống xử lý thế nào? → A: Khôi phục saved word cũ về ACTIVE, giữ personal note; không tạo record mới.
- Q: Personal note của một saved word trong MVP có giới hạn và định dạng nào? → A: Optional plain-text, tối đa 500 ký tự; không hỗ trợ format hoặc file đính kèm.
- Q: Khi dictionary entry đã được learner lưu bị F04 gỡ publish, learner nên thấy gì? → A: Giữ saved word và personal note, nhưng hiển thị trạng thái “không còn khả dụng”; ẩn dictionary detail.
- Q: Khi visitor mở một dictionary entry trong MVP, họ được xem những nội dung nào? → A: Hanzi giản thể/phồn thể nếu có, pinyin, nghĩa tiếng Việt, ví dụ; audio/image chỉ hiện khi đã publish và khả dụng.

### Session 2026-09-11

- Q: Khi người học sửa cùng một ghi chú trên hai thiết bị, hệ thống nên xử lý thế nào nếu thiết bị thứ hai lưu dựa trên bản cũ? → A: Báo xung đột; giữ bản đã lưu và yêu cầu thiết bị thứ hai tải lại trước khi sửa tiếp.
- Q: Khi tìm bằng chữ Hán hoặc pinyin, hệ thống nên khớp truy vấn với phần nào của từ? → A: Bất kỳ vị trí; ưu tiên khớp toàn bộ từ, sau đó khớp đầu từ, rồi các vị trí còn lại.
- Q: Số từ được lưu trong kho cá nhân có bị giới hạn theo gói Free/Premium không? → A: Free lưu tối đa 20 từ; khi đủ 20 từ, tự xóa từ cũ nhất rồi thêm từ mới để giữ 20 từ. Premium lưu không giới hạn.
- Q: Khi Premium hết hạn và kho đang có hơn 20 từ, hệ thống nên xử lý số từ vượt giới hạn thế nào? → A: Giữ 20 từ mới nhất; chuyển các từ cũ hơn sang trạng thái đã xóa, giữ ghi chú và lịch sử để có thể khôi phục.
- Q: Khi xác định từ cũ nhất để tự xóa, hệ thống nên tính từ lần lưu đầu tiên hay lần lưu hoặc khôi phục gần nhất? → A: Lần lưu/khôi phục gần nhất; từ vừa khôi phục được xem là mới. Sửa ghi chú hoặc lưu lại từ đang có không đổi thứ tự.

### Session 2026-09-11 (follow-up)

- Q: Bạn muốn dùng mức tải nào để nghiệm thu yêu cầu tìm kiếm dưới 2 giây? → A: 100 người dùng đồng thời, 100.000 mục từ đã xuất bản; mỗi người tìm một lần mỗi 5 giây trong 10 phút sau 2 phút làm nóng. Ít nhất 95% lượt tìm hiển thị kết quả hoặc trạng thái rỗng trong dưới 2 giây từ lúc gửi tìm kiếm; lượt lỗi hoặc hết thời gian tính là không đạt.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover dictionary entries (Priority: P1)

As a visitor, I want to search a shared Chinese dictionary and open an entry so that I can
understand Hanzi, pinyin, and Vietnamese meaning.

**Why this priority**: Dictionary discovery creates value without requiring an account.

**Independent Test**: A visitor searches valid text, receives matching public summaries, and opens
only published permitted entry detail.

**Acceptance Scenarios**:

1. **Given** a valid query, **When** a visitor searches the dictionary, **Then** matching entry
   summaries are returned in a bounded result set.
2. **Given** no match, **When** the visitor searches, **Then** they receive a clear empty state.
3. **Given** a simplified-Hanzi, traditional-Hanzi, tone-insensitive pinyin, or Vietnamese meaning
   keyword query, **When** a matching published entry exists, **Then** its public summary is
   eligible to appear in the bounded result set.
4. **Given** a published dictionary entry, **When** a visitor opens it, **Then** they see its
   simplified Hanzi, available traditional Hanzi, pinyin, Vietnamese meanings, and examples; audio
   or image appears only when that asset is published and available.
5. **Given** published entries containing a Hanzi or normalized-pinyin query at different
   positions, **When** a visitor searches, **Then** entries matching the complete query anywhere
   in the field are eligible, ordered by whole-field matches, then prefix matches, then other
   substring matches. For example, `学` matches both `学生` and `大学`, with `学生` ranked first;
   normalized `xue` matches both `xuesheng` and `daxue` in the same order.
6. **Given** trimmed search input, **When** its length is 1 or 120 Unicode code points,
   **Then** it passes length validation; missing input, whitespace-only input and 121 code points
   are rejected without dictionary results.
7. **Given** a valid query with enough matches, **When** pagination is omitted, **Then** the
   first page contains 20 entries. Page sizes 1 and 50 are accepted; 0, 51, non-integer sizes
   and negative or non-integer page numbers are rejected. A valid page beyond the available
   matches returns an empty state.
8. **Given** the workload defined in SC-001, **When** dictionary searches are measured during
   its 10-minute measurement window, **Then** at least 95% display matching results or a correct
   empty state in under 2 seconds from search submission; errors and timeouts count as failures.

---

### User Story 2 - Build a private vocabulary list (Priority: P1)

As a learner, I want to save, annotate, update, or remove dictionary words in my own vocabulary so
that I can return to words that matter to me.

**Why this priority**: Saved vocabulary is the prerequisite for later spaced-repetition review.

**Independent Test**: A learner saves the same dictionary entry twice without duplicates and sees
only their own note and saved-word state.

**Acceptance Scenarios**:

1. **Given** a dictionary entry, **When** the learner saves it, **Then** it appears once in their
   private vocabulary.
2. **Given** an owned saved word, **When** the learner edits a permitted note or removes it,
   **Then** only their own saved-word state changes.
3. **Given** the learner previously removed a saved word, **When** they save the same dictionary
   entry again, **Then** the original saved word is restored with its personal note and no new
   vocabulary record is created.
4. **Given** an owned saved word, **When** the learner updates its optional plain-text note within
   500 characters, **Then** the note is saved privately without formatting or attachments.
5. **Given** an owned saved word whose dictionary entry is no longer published, **When** the
   learner views private vocabulary, **Then** the saved word and personal note remain but show an
   unavailable state without the withdrawn dictionary detail.
6. **Given** two devices have loaded the same personal note and the first device saves a change,
   **When** the second device submits an edit based on the older version, **Then** the system
   rejects it with a conflict, preserves the first device's saved note, and asks the learner to
   reload the latest version before editing again.
7. **Given** a Free learner has 20 active saved words, **When** they successfully save a new
   dictionary entry or restore a removed word, **Then** the oldest active saved word is
   automatically removed and the requested word becomes active, leaving exactly 20 active words.
8. **Given** a Free learner has 20 active saved words, **When** they save an already active entry,
   **Then** the operation is idempotent and no word is removed.
9. **Given** a learner has an active Premium entitlement and 20 or more active saved words,
   **When** they save another permitted entry, **Then** it is added without a vocabulary-count
   limit or automatic removal of another word.
10. **Given** a Premium learner has more than 20 active saved words, **When** their Premium
    entitlement expires and they return to Free, **Then** only the 20 newest saved words remain
    active; older words become removed, retaining their personal notes and review history, with
    their F10 schedules suspended.
11. **Given** an old removed word is restored, **When** the system determines the oldest word
    for capacity removal or the 20 newest words to retain after Premium expiry, **Then** that
    word is treated as newly saved at its restoration time. Editing a note or saving an already
    active word does not change its position in this order.

---

### Edge Cases

- Search input is trimmed before validation and must contain 1–120 Unicode code points before
  search normalization. Missing, whitespace-only or longer input is rejected with a validation
  message and no results; it is never treated as a request to browse all entries.
- Dictionary results default to 20 entries per page, with a permitted page size of 1–50 and a
  non-negative integer page number starting at 0. Invalid pagination is rejected; a valid page
  beyond the available matches returns an empty result set.
- Tone marks, case, and spacing do not prevent a pinyin query from matching its normalized
  dictionary entry; only published entries may appear for any query type.
- An unavailable or unpublished audio or image asset is omitted from public dictionary detail
  without preventing the text entry from opening.
- Saving an existing active word is idempotent rather than duplicating it.
- Saving a previously removed word restores its original saved-word record and personal note rather
  than creating a duplicate; F09 does not create or reset its F10 review schedule, which F10
  resumes.
- A note longer than 500 characters, or one containing formatting or an attachment, is rejected
  without changing the learner's existing saved-word state.
- A saved word remains learner-owned when its dictionary entry is no longer published, but must not
  reveal the withdrawn dictionary detail.
- Deleting a saved word never deletes the shared dictionary entry or another learner's copy.
- A personal-note update based on an outdated version is rejected with a conflict; the latest
  saved note remains unchanged, and the learner must reload it before editing again.
- Automatic removal to make room for a Free learner uses the existing saved-word removal
  lifecycle: preserve the record and personal note for restoration and let F10 suspend its
  schedule. It never deletes shared dictionary content or another learner's saved word.
- Removing an old word and activating its replacement succeed or fail together. Failed saves
  remove no word, and concurrent saves must not bypass the Free vocabulary limit.
- Premium expiry with 20 or fewer active saved words removes no words. Expiry with more than
  20 applies the same removal lifecycle as automatic capacity removal, preserving notes and
  review history for later restoration.
- Restoring a removed word refreshes its vocabulary recency without changing its original
  record identity, personal note or preserved F10 review history. Note edits and idempotent
  saves of active words do not refresh vocabulary recency.

## Requirements *(mandatory)*

- **FR-001**: Visitors MUST be able to search and view permitted shared dictionary entries. Search
  MUST support simplified Hanzi, traditional Hanzi, pinyin without tone distinctions, and Vietnamese
  meaning keywords, and MUST return only matching published entry summaries in a bounded result set.
  Hanzi and normalized-pinyin matching MUST accept the complete query as a contiguous substring
  anywhere in the corresponding field, ranking whole-field matches before prefix matches and
  prefix matches before other substring matches.
  Queries MUST contain 1–120 Unicode code points after trimming and before search normalization.
  Search MUST paginate results with 20 entries per page by default, a requested page size of
  1–50, and a non-negative integer page number starting at 0. Missing or invalid queries and
  invalid pagination MUST produce a validation message without returning dictionary results.
- **FR-002**: Learners MUST be able to save one private vocabulary record per active dictionary
  entry and update permitted notes or status, subject to the Free/Premium vocabulary rules.
- **FR-003**: Learners MUST be able to list and delete only their own saved words.
- **FR-004**: The system MUST prevent duplicate active saved words for the same learner and entry.
- **FR-005**: Shared dictionary content MUST remain separate from learner-private notes and status.
- **FR-006**: Review scheduling, due-state calculation, and review history are out of scope for this
  feature and belong to F10.
- **FR-007**: Saving a dictionary entry with an existing learner-owned removed saved word MUST
  restore that same record to `ACTIVE`, preserve its personal note, and create no new saved-word
  record. F09 MUST NOT create or reset an F10 review schedule during this restoration; F10 resumes
  the existing schedule.
- **FR-008**: A saved-word personal note MUST be optional plain text of at most 500 characters.
  The system MUST reject formatting, attachments, or a longer note without changing the existing
  saved-word state.
- **FR-009**: When a learner-owned saved word references a dictionary entry that is no longer
  published, the system MUST retain the saved word and personal note, show it as unavailable, and
  withhold the withdrawn dictionary detail.
- **FR-010**: A published dictionary entry detail MUST expose simplified Hanzi, available
  traditional Hanzi, pinyin, Vietnamese meanings, and examples. It MAY expose audio or image only
  when the related F04 asset is published and available; unavailable assets MUST be omitted.
- **FR-011**: The system MUST detect and reject personal-note updates based on an outdated
  version, including concurrent edits from different devices. It MUST preserve the latest saved
  note, report a conflict, and require the learner to reload the latest version before editing
  again.
- **FR-012**: Free learners MUST have a vocabulary capacity of 20 active saved words. When a
  successful save or restoration would add a word to a full Free vocabulary, the system MUST
  automatically remove the oldest active saved word and activate the requested word together,
  leaving 20 active words. Saving an already active entry MUST NOT remove any word. Removed
  records MUST NOT count toward capacity; active unavailable words still count.
- **FR-013**: Learners with an active Premium entitlement MUST be able to save words without a
  vocabulary-count limit or automatic removal for capacity. The backend MUST determine the
  applicable plan using F03 entitlement state and enforce vocabulary capacity during concurrent
  saves. A failed save MUST leave the existing vocabulary unchanged.
- **FR-014**: When Premium expires and the learner returns to Free with more than 20 active
  saved words, the system MUST retain only the 20 newest active words and transition all older
  active words to the removed state. It MUST preserve the removed records, personal notes and
  F10 review history, and have F10 suspend their schedules. If at most 20 words are active, no
  word MUST be removed due to expiry.
- **FR-015**: The system MUST determine oldest/newest saved words from their most recent
  successful first save or restoration to `ACTIVE`, using `saved_word_id ASC` as a tie-breaker
  when timestamps match. A restored word MUST be treated as newly saved for both Free capacity
  removal and Premium-expiry retention. Note edits and saving an already active entry MUST NOT
  change this order. This ordering MUST NOT reset F10 review history or scheduling.

### Key Entities

- **Dictionary entry**: shared Chinese word information available to permitted visitors.
- **Saved word**: a learner-owned link to one dictionary entry with private note and status.
- **Vocabulary recency**: the time of a word's most recent successful first save or restoration
  to `ACTIVE`, used for oldest/newest ordering independently of note changes and review history.
- **Vocabulary capacity**: the plan-dependent number of active saved words: 20 for Free and
  unlimited for active Premium. Automatic capacity removal preserves the existing record and
  note through the same lifecycle as learner-initiated removal.
- **Normalized dictionary query**: a visitor search input evaluated as simplified/traditional Hanzi,
  tone-insensitive pinyin, or Vietnamese meaning keywords against published dictionary content.
  Hanzi and normalized-pinyin queries match contiguous substrings at any position.
- **Restored saved word**: the original learner-owned saved-word record returned to `ACTIVE` when its
  owner saves the same dictionary entry again; its personal note is retained.
- **Personal note**: optional private plain text of at most 500 characters on one saved word; it
  does not support formatting or attachments.
- **Unavailable saved word**: an owned saved word retained after its shared dictionary entry is no
  longer published; it shows no withdrawn dictionary detail but retains the learner's private note.
- **Public dictionary detail**: the permitted published entry fields—Hanzi, pinyin, Vietnamese
  meanings, examples, and only available approved audio or image assets.

## Success Criteria *(mandatory)*

- **SC-001**: With 100,000 published dictionary entries and 100 concurrent users, each submitting
  one valid search every 5 seconds, at least 95% of searches MUST display matching results or a
  correct empty state in under 2 seconds from search submission. Measure for 10 minutes after a
  2-minute warm-up under the same load. The denominator includes all searches submitted during
  the measurement window; errors, timeouts and searches that do not display a correct outcome
  within 2 seconds count as failures. Timing includes the request, network transit and result
  rendering, not just server processing. The test report MUST record the environment, browser,
  network conditions, dataset and query/page-size mix so the measurement can be repeated.
- **SC-002**: 100% of saved-word reads and changes are restricted to the owning learner.
- **SC-003**: Saving the same active dictionary entry twice produces one vocabulary record.
- **SC-004**: Deleting a saved word never changes the shared dictionary entry.
- **SC-005**: 100% of supported Hanzi, normalized-pinyin, and Vietnamese-meaning searches return
  only matching published dictionary summaries or a clear empty state.
- **SC-006**: 100% of saves of a previously removed owned entry restore the original saved-word
  record with its personal note and create no duplicate.
- **SC-007**: 100% of note submissions exceeding 500 characters or containing formatting or an
  attachment are rejected without changing the saved word.
- **SC-008**: 100% of saved words referencing an unpublished dictionary entry retain only their
  owner-visible saved-word state and personal note, without exposing withdrawn dictionary detail.
- **SC-009**: 100% of public dictionary details include only the permitted text fields and assets
  that are published and available; unavailable assets are omitted.
- **SC-010**: 100% of personal-note updates based on an outdated version are rejected with a
  conflict without overwriting the latest saved note, and the learner is prompted to reload
  before editing again.
- **SC-011**: 100% of Hanzi and normalized-pinyin search acceptance cases include matching
  published entries at any substring position as eligible results and rank whole-field matches
  before prefix matches, followed by other substring matches, within the bounded result set.
- **SC-012**: 100% of successful new saves or restorations into a Free vocabulary of 20 active
  words replace its oldest active word and leave exactly 20 active words; duplicate active saves
  and failed saves remove no words, including under concurrent requests.
- **SC-013**: Premium learners can save beyond 20 active words without a vocabulary-count
  rejection or automatic capacity removal.
- **SC-014**: 100% of Premium-to-Free expiry cases with more than 20 active words retain exactly
  the 20 newest active words, preserving notes and review history for removed words and
  suspending their F10 schedules; expiry cases with at most 20 active words remove none.
- **SC-015**: 100% of capacity-removal and Premium-expiry acceptance cases use the most recent
  successful first-save/restoration order; restored words are treated as new, while note edits
  and duplicate active saves leave that order unchanged.
- **SC-016**: 100% of search boundary acceptance cases enforce the 1–120-code-point query
  limit, default page size of 20, allowed page sizes of 1–50 and non-negative integer page
  numbers; invalid input returns no dictionary results and valid pages never exceed their size.

## Assumptions

- F04 provides approved shared dictionary content for MVP.
- F01 provides learner ownership; no Admin access to private saved words exists.
- F03 provides authoritative Free/Premium entitlement state for vocabulary capacity.
- F10 later creates and owns the review schedule for saved words.
