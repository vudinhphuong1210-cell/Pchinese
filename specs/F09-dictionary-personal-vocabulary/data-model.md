# Data Model: F09 Dictionary and Personal Vocabulary

## Dictionary Read Model

Existing F04 dictionary/content tables remain authoritative. Add or maintain the following searchable projections as appropriate:

| Field | Rules |
| --- | --- |
| simplified_normalized | Indexed normalized Simplified Chinese form |
| traditional_normalized | Indexed normalized Traditional Chinese form |
| pinyin_normalized | Indexed form without tone, case or spacing variance |
| vietnamese_search | Indexed normalized senses/keywords |
| publication_status | PUBLISHED is the only public state |

## saved_words

| Column | Rules |
| --- | --- |
| id | UUID primary key, stable across delete and restore |
| user_id | FK users; owner only |
| dictionary_entry_id | FK dictionary entry |
| personal_note_encrypted | Nullable encrypted plain text, maximum 500 characters |
| status | ACTIVE or DELETED |
| created_at, updated_at, deleted_at | Lifecycle timestamps |

Constraints and indexes:

- Unique user_id plus dictionary_entry_id.
- Index user_id plus status plus updated_at for a learner's vocabulary list.
- Personal notes are not added to search indexes or logs.

## F10 Association

An F10 srs_schedules row has one-to-one ownership through saved_words.id. F09 does not duplicate due date, interval or review history. It requests schedule creation, suspension or restoration through the F10 domain service.

