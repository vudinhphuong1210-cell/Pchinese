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

---

### Edge Cases

- Empty or oversized search input is handled without exposing unrelated entries.
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

## Requirements *(mandatory)*

- **FR-001**: Visitors MUST be able to search and view permitted shared dictionary entries. Search
  MUST support simplified Hanzi, traditional Hanzi, pinyin without tone distinctions, and Vietnamese
  meaning keywords, and MUST return only matching published entry summaries in a bounded result set.
- **FR-002**: Learners MUST be able to save one private vocabulary record per active dictionary
  entry and update permitted notes or status.
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

### Key Entities

- **Dictionary entry**: shared Chinese word information available to permitted visitors.
- **Saved word**: a learner-owned link to one dictionary entry with private note and status.
- **Normalized dictionary query**: a visitor search input evaluated as simplified/traditional Hanzi,
  tone-insensitive pinyin, or Vietnamese meaning keywords against published dictionary content.
- **Restored saved word**: the original learner-owned saved-word record returned to `ACTIVE` when its
  owner saves the same dictionary entry again; its personal note is retained.
- **Personal note**: optional private plain text of at most 500 characters on one saved word; it
  does not support formatting or attachments.
- **Unavailable saved word**: an owned saved word retained after its shared dictionary entry is no
  longer published; it shows no withdrawn dictionary detail but retains the learner's private note.
- **Public dictionary detail**: the permitted published entry fields—Hanzi, pinyin, Vietnamese
  meanings, examples, and only available approved audio or image assets.

## Success Criteria *(mandatory)*

- **SC-001**: A visitor receives search or empty results in under 2 seconds under normal conditions.
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

## Assumptions

- F04 provides approved shared dictionary content for MVP.
- F01 provides learner ownership; no Admin access to private saved words exists.
- F10 later creates and owns the review schedule for saved words.
