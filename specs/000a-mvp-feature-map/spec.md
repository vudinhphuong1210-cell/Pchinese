# Feature Specification: MVP Feature Map

**Feature Branch**: `[000a-mvp-feature-map]`

**Created**: 2026-09-04

**Last Updated**: 2026-09-05

**Status**: Draft

**Input**: User description: "Hiện tại anh cần chia dự án thành các feature"

## Clarifications

### Session 2026-09-05

- Q: Khi MVP chưa có thanh toán tự động và Admin không được cấp Premium thủ công, learner sẽ nhận Premium bằng cách nào? → A: MVP chỉ có Free; Premium và nút nâng cấp chưa mở cho learner.
- Q: Trong dashboard Admin, Admin được phép quản lý tài khoản ở mức nào? → A: Role `ADMIN` và khóa/mở khóa tài khoản, không xem dữ liệu học tập.
- Q: Dashboard Admin thống nhất cần được đưa vào MVP cùng F01/F04 hay để sau khi learner-facing MVP hoàn tất? → A: Có trong MVP, gồm các tab quản lý được phép của F01 và F04.
- Q: Dictation và Shadowing sẽ là các bước luyện tập trong Lesson Player hay là hai màn hình độc lập? → A: Dictation trong Player, Shadowing là màn hình riêng.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Approve a coherent MVP scope (Priority: P1)

As the product owner, I want to see every planned MVP capability grouped into clear, bounded
features so that I can approve what will be built now and what is explicitly deferred.

**Why this priority**: A shared, bounded scope prevents work from starting against conflicting
documents or accidentally including Phase 2 work in the MVP.

**Independent Test**: A reviewer can compare the project contracts with the inventory below,
account for each MVP capability exactly once, and identify deferred work without reading code.

**Acceptance Scenarios**:

1. **Given** the current product, API, and canonical MVP data contracts, **When** the owner reviews
   the inventory, **Then** every MVP capability belongs to one named feature with a stated outcome
   and scope boundary.
2. **Given** a capability designated for a later phase, **When** the owner reviews the inventory,
   **Then** it is marked as excluded rather than assigned to an MVP feature.

---

### User Story 2 - Start the next feature safely (Priority: P1)

As a delivery team member, I want each feature to state its dependencies, access rules, and
expected result so that I can choose the next ready feature and create a focused specification for it.

**Why this priority**: The team needs a practical delivery sequence, not only a feature list.

**Independent Test**: Given any item in the inventory, a team member can identify its entry
conditions, affected product areas, and the preceding feature specifications that must be approved.

**Acceptance Scenarios**:

1. **Given** an unstarted feature, **When** a team member selects it, **Then** the map identifies
   all prerequisite features and whether it is ready to specify.
2. **Given** a feature with learner data, access control, Premium access, or AI allowance impact,
   **When** it is selected, **Then** its mandatory policy concerns are visible before planning begins.

---

### User Story 3 - Review scope changes consistently (Priority: P2)

As a reviewer, I want a traceable map from each delivery feature to its source contracts so that
new specifications, plans, and changes remain consistent with project governance.

**Why this priority**: The map is useful only if it supports review and prevents duplicate or
unowned responsibility.

**Independent Test**: A reviewer can choose a feature, locate the listed source contracts, and
confirm that the feature has not taken on another feature's responsibility.

**Acceptance Scenarios**:

1. **Given** a proposed new feature specification, **When** a reviewer compares it with this map,
   **Then** the proposed scope is either mapped to one existing feature or the map is deliberately
   amended before approval.
2. **Given** a change to a canonical contract, **When** the affected feature is reviewed, **Then**
   the map identifies the feature whose specification must be revisited.

---

### Edge Cases

- A requested capability spans multiple features: the owner records the primary feature and the
  explicit dependencies; duplicate responsibility is not introduced.
- A capability does not fit the MVP data contract: it is placed in the explicit Phase 2 exclusion
  list until an approved feature changes the contract.
- A feature is technically dependent on another feature but can be specified earlier: its status is
  marked `Blocked` until the stated prerequisite is approved or delivered.
- An administrator attempts to lock their own account or the final active administrator: the action
  is rejected and leaves every account/session unchanged.
- A locked learner attempts to use an existing session or sign in again: access is denied; their
  learning data remains intact and becomes available again only after an authorized unlock and a
  new sign-in.
- A source document conflicts with the map: the root constitution and the canonical source named
  in that contract take precedence; the map is corrected rather than treated as a new authority.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST maintain one canonical MVP feature inventory in this specification.
- **FR-002**: Every inventory entry MUST state a feature ID, user outcome, primary actor, scope
  boundary, prerequisite feature IDs, affected layer or layers, and readiness state.
- **FR-003**: Each MVP capability described by the project contracts MUST be assigned to exactly
  one primary feature; a dependency reference MUST be used instead of assigning the same
  responsibility to two features.
- **FR-004**: Every feature that changes learner data, access, roles, Premium entitlement, AI
  allowance, recording, or conversation content MUST call out its governing policy concerns before
  it is planned.
- **FR-005**: Each delivery feature MUST receive its own feature specification before implementation;
  this map does not authorize implementation by itself.
- **FR-006**: The delivery order MUST make the supporting foundation, identity, authorization,
  content availability, entitlement, and learner-ownership prerequisites visible.
- **FR-007**: A feature affecting MVP data MUST identify `DATA_short.md` as its authoritative data
  contract. `DATA.md` MAY inform future direction but MUST NOT be used alone to authorize MVP data
  changes.
- **FR-008**: The inventory MUST explicitly exclude later-phase capabilities so they are not
  silently included in MVP delivery.
- **FR-009**: Any change that adds, splits, merges, or materially reorders an MVP feature MUST
  update this map and the affected feature specifications in the same review.
- **FR-010**: A feature is `Ready to specify` only when its stated prerequisite features are
  approved, its actor and entry point are known, and its scope has a testable boundary.
- **FR-011**: The MVP MUST automatically assign every eligible learner the Free plan and MUST NOT
  activate Premium access, show an upgrade journey, or expose a manual Premium/quota control until
  automated billing is approved in a later feature.
- **FR-012**: An authorized administrator MUST be able to lock or unlock another account without
  reading learner-private data. Locking MUST end the target's active access, record an auditable
  reason, and be rejected for the acting administrator or the final active administrator.
- **FR-013**: The MVP MUST provide one Admin dashboard containing only an Account/Roles tab for
  F01 and a Content/Media tab for F04. The dashboard MUST NOT create any authority beyond those
  two feature boundaries.
- **FR-014**: The MVP MUST present Dictation inside the Lesson Player and open Shadowing as a
  separate learner-owned practice screen from that player. Both flows MUST preserve the selected
  lesson and segment context without requiring the learner to reselect them.

### MVP Feature Inventory

| ID | Feature | Primary user outcome | Primary actor | Scope boundary | Depends on | Affected layers | Readiness |
| --- | --- | --- | --- | --- | --- | --- | --- |
| F00 | Delivery foundation | Team can deliver governed product slices consistently. | Delivery team | Shared project setup, contract baseline, quality and release gates; no learner-facing behaviour. | None | Frontend and backend | Specified: `F00-delivery-foundation` |
| F01 | Identity, account, and role administration | A learner can use a secure account and an authorized administrator can safely manage the `ADMIN` role and account access state. | Learner and administrator | Registration, verification, sign-in, credential recovery, active sessions, audited `ADMIN` role grant/revocation, and lock/unlock of another account; includes the Account/Roles tab of the MVP Admin dashboard; excludes learner-private data and Premium/quota changes. | F00 | Frontend and backend | Specified: `F01-identity-account-role-admin` |
| F02 | Profile and session control | A signed-in learner can manage personal preferences and active devices. | Learner | Profile settings and session visibility/revocation; excludes account creation and recovery. | F01 | Frontend and backend | Specified: `F02-profile-session-control` |
| F03 | Plans, entitlement, and AI allowance | A learner can see the Free access state that the system automatically applies to their account. | Learner | Automatically assign and enforce the Free plan for MVP, including its AI allowance; excludes paid Premium activation, upgrade journeys, payment-provider integration, and all Admin grant/revoke/edit operations. | F01 | Frontend and backend | Specified: `F03-plans-entitlement-ai-allowance` |
| F04 | Content administration | An authorized administrator can prepare and control learning content safely. | Administrator | Create, edit, publish, archive, and organize topics, lessons, segments, and media metadata; includes the Content/Media tab of the MVP Admin dashboard. MVP content is published as `FREE`. `PREMIUM` labelling is reserved for the later automated-billing feature. | F01 | Frontend and backend | Specified: `F04-content-administration` |
| F05 | Course catalog and access | A visitor can find available Chinese-learning topics and lessons. | Visitor and learner | Browse and inspect published `FREE` content; excludes Premium lock/upgrade journeys, in-lesson progress, and practice submission. | F03, F04 | Frontend and backend | Specified: `F05-course-catalog-access` |
| F06 | Lesson learning and progress | A learner can complete lesson segments and resume learning from an accurate progress state. | Learner | Lesson playback, segment completion, resume state, progress overview, and the in-player Dictation entry point; excludes assessment scoring. | F01, F05 | Frontend and backend | Specified: `F06-lesson-learning-progress` |
| F07 | Dictation practice | A learner can submit a dictation answer inside the Lesson Player and receive an owned, recorded result. | Learner | In-player Dictation prompts, answer validation, attempt history, and progress contribution; does not create a separate primary screen. | F01, F06 | Frontend and backend | Specified: `F07-dictation-practice` |
| F08 | Shadowing practice | A learner can open a dedicated practice screen, submit an eligible voice recording, and receive controlled speaking feedback. | Learner | Separate Shadowing screen opened with the current lesson/segment context, recording lifecycle, speaking attempt, score, feedback, expiry, and progress contribution. | F01, F03, F06 | Frontend and backend | Specified: `F08-shadowing-practice` |
| F09 | Dictionary and personal vocabulary | A learner can search a shared dictionary and save words to a private vocabulary list. | Visitor and learner | Dictionary discovery and learner-owned saved words; excludes scheduling of reviews. | F01, F04 | Frontend and backend | Specified: `F09-dictionary-personal-vocabulary` |
| F10 | Spaced repetition review | A learner can review due vocabulary and see the next review schedule. | Learner | Due queue, review response, schedule update, and immutable review history. | F01, F09 | Frontend and backend | Specified: `F10-spaced-repetition-review` |
| F11 | AI learning buddy | A learner can hold a protected learning conversation within their available AI allowance. | Learner | Conversation lifecycle, messages, safety feedback, allowance consumption, and deletion; excludes direct provider exposure. | F01, F03 | Frontend and backend | Specified: `F11-ai-learning-buddy` |

### Source and Policy Traceability

| Concern | Authoritative source | Features that MUST apply it |
| --- | --- | --- |
| Project governance, architecture boundaries, security, quality | `CONSTITUTION.md`, `AGENT.md`, `CLAUDE.md` | F00–F11 |
| Frontend scope, accessibility, and client responsibility | `frontend/CONSTITUTION.md`, `frontend/AGENT.md`, `frontend/CLAUDE.md`, `frontend/DESIGN.md` | Every feature affecting frontend |
| Backend ownership, authorization, transactions, and persistence responsibility | `backend/CONSTITUTION.md`, `backend/AGENT.md`, `backend/CLAUDE.md` | Every feature affecting backend |
| MVP data, relationships, migration order, and lifecycle constraints | `DATA_short.md` | F01–F11 where data changes |
| Future data direction only | `DATA.md` | Later-phase proposals only unless an approved mapping changes MVP scope |
| Public product routes, endpoints, envelopes, errors, and access rules | `API.md` | F01–F11 where an exposed contract is added or changed |

### Delivery Boundaries

- F00 is a prerequisite delivery slice; it does not replace the independent product specifications
  required for F01–F11.
- F01 and F03 establish the authorization and Free-plan decisions relied upon by protected learner
  journeys. Paid Premium journeys begin only in the later automated-billing feature.
- The MVP Admin dashboard is a single workspace for the permitted role and account-status
  management flow in F01 and content-management flow in F04. It is not a new authority and MUST
  NOT expose learner-private data or manual entitlement/quota controls.
- F04 provides the controlled content lifecycle required for F05, F06, F07, F08, and F09. Seed
  content used solely to verify a feature does not expand F04's scope.
- F06 owns generic lesson progression. F07 and F08 own their assessment-specific results and may
  contribute approved outcomes to F06 without taking ownership of progress state.
- F07 is an in-player activity within F06. F08 is a distinct Shadowing screen entered from F06;
  it receives the selected lesson and segment context but owns no independent lesson-progress state.
- F09 owns saved vocabulary. F10 owns review scheduling and review history.
- F11 owns AI conversation behavior; F03 owns the shared allowance decision and recorded allowance
  use.

### Explicitly Deferred from MVP

- Paid Premium activation, upgrade journeys, payment-provider checkout, invoicing, refunds, and
  tax handling.
- Full privacy/compliance operations: consent history, data export, deletion requests, legal hold,
  and encryption-key ledger.
- Detailed assessment analytics, handwriting/stroke history, recommendations, weak-area analysis,
  daily statistics, leaderboards, teacher sharing, and separate analytics.
- Rich dictionary editorial normalization beyond the MVP dictionary capability.
- Unapproved architecture additions or direct learner interaction with AI or media providers.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of MVP capabilities in the current product, API, and canonical data contracts
  are assigned to exactly one of F00–F11 or are explicitly listed as deferred.
- **SC-002**: A reviewer can select any inventory feature and identify its primary outcome, actor,
  scope boundary, dependencies, affected layers, and governing sources in under 10 minutes.
- **SC-003**: 100% of feature specifications created after this map name their matching inventory ID
  and do not expand the mapped scope without an approved update to this map.
- **SC-004**: Before implementation begins, every selected feature has a specification with a
  defined actor, access rule, state changes, failure cases, affected public contracts, acceptance
  criteria, tests, and explicit non-goals.
- **SC-005**: In a scope review, two independent reviewers classify every proposed MVP capability
  into the same inventory feature or record a resolved decision before planning proceeds.

## Assumptions

- The current source documents describe the intended MVP; implementation has not yet established a
  conflicting production contract.
- The team will create one focused Spec Kit specification, plan, and task set per inventory feature
  before building that feature.
- F00 may be delivered in smaller technical increments, but each increment remains governed by its
  own approved scope and does not silently add learner-facing functionality.
- `DATA_short.md` remains the canonical MVP data contract until the project formally changes that
  status.
- The MVP automatically assigns the Free plan after the applicable account lifecycle completes;
  no learner receives Premium access before an automated-billing feature is approved.
- An Admin account-management action is limited to another account's `ADMIN` role and lock/unlock
  status; it never permits private-data browsing or mutation and always requires an audit reason.
- The MVP Admin dashboard has exactly the Account/Roles and Content/Media scopes mapped to F01 and
  F04; any additional Admin capability requires a feature-map revision and its own specification.
- Dictation remains an activity in the Lesson Player. Shadowing is the only additional learner
  practice screen opened from the player in MVP.
- Phase 2 items remain out of scope unless the owner approves a new feature map revision.
