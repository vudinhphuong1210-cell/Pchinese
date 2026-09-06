# Research: F09 Dictionary and Personal Vocabulary

## Decisions

| Topic | Decision | Rationale |
| --- | --- | --- |
| Search input | Trim and bound query; normalize Chinese variants and pinyin tone marks, case and whitespace before indexed lookup. | Learners commonly type the same word in different forms. |
| Search scope | Search only PUBLISHED entries; optional HSK filter and bounded pagination. | Draft/withdrawn teaching content must not leak. |
| Entry detail | Return Simplified, Traditional when available, pinyin, Vietnamese senses/examples and only available published assets. | The client receives a complete safe learning projection. |
| Save uniqueness | One logical saved word per user plus dictionary entry. | Avoids duplicate vocabulary and duplicated SRS schedules. |
| Delete/re-save | Delete marks the relation inactive and suspends its schedule; re-save restores the same relation, note and prior SRS state. | Preserves learning history rather than silently resetting it. |
| Source withdrawal | Saved history may remain, but the withdrawn entry's content and media are not returned. | Reconciles personal audit with publication control. |

## Search Implementation

Maintain normalized columns or database-supported full-text/trigram indexes for Simplified, Traditional, normalized pinyin and normalized Vietnamese senses. Rank exact script/pinyin matches ahead of broad Vietnamese keyword matches. The API must not accept raw user-provided sort expressions.

## F10 Boundary

F09 calls an internal F10 service command, not its HTTP endpoint:

1. First active save ensures one LEARNING schedule due immediately.
2. Delete suspends the schedule and excludes it from the F10 due queue.
3. Restore reactivates the existing schedule. It returns to LEARNING if never reviewed, otherwise REVIEW; overdue content is due immediately.

The save/delete/restore transition and schedule mutation share a Spring Boot transaction.

## Rejected Alternatives

- Resetting the schedule on re-save: destroys real learner history.
- Returning source content after editorial withdrawal: violates F04 publication control.
- Storing rich notes or attachments: expands privacy and moderation scope without a stated need.
- Building this feature through ai-service: it has no AI decision to make.

