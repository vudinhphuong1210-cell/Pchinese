-- =============================================================================
-- Migration: 20260911210000_f09_dictionary_vocabulary.sql
-- Description: F09 Dictionary search keys projection, saved-word constraints/indexes, and user_entitlements active constraint.
-- =============================================================================

CREATE TABLE IF NOT EXISTS dictionary_search_keys (
    dictionary_search_key_id    uuid            NOT NULL DEFAULT gen_random_uuid(),
    dictionary_entry_id         uuid            NOT NULL,
    query_kind                  varchar(30)     NOT NULL,
    normalized_key              varchar(200)    NOT NULL,
    match_rank                  smallint        NOT NULL,
    created_at                  timestamptz     NOT NULL DEFAULT now(),
    CONSTRAINT pk_dictionary_search_keys PRIMARY KEY (dictionary_search_key_id),
    CONSTRAINT fk_dictionary_search_keys_entry FOREIGN KEY (dictionary_entry_id) REFERENCES dictionary_entries (dictionary_entry_id) ON DELETE CASCADE,
    CONSTRAINT uq_dictionary_search_keys_entry_kind_key UNIQUE (dictionary_entry_id, query_kind, normalized_key),
    CONSTRAINT ck_dictionary_search_keys_kind CHECK (query_kind IN ('SIMPLIFIED_HANZI', 'TRADITIONAL_HANZI', 'PINYIN', 'VIETNAMESE_KEYWORD')),
    CONSTRAINT ck_dictionary_search_keys_rank CHECK (match_rank BETWEEN 1 AND 3)
);

CREATE INDEX IF NOT EXISTS ix_dictionary_search_keys_lookup
    ON dictionary_search_keys (query_kind, normalized_key, match_rank, dictionary_entry_id);

-- Enforce strict unique constraint per (user_id, dictionary_entry_id) in saved_words
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_saved_words_user_entry'
    ) THEN
        ALTER TABLE saved_words ADD CONSTRAINT uq_saved_words_user_entry UNIQUE (user_id, dictionary_entry_id);
    END IF;
END $$;

-- Capacity and recency lookup index for Free limit and Premium expiry
CREATE INDEX IF NOT EXISTS ix_saved_words_capacity
    ON saved_words (user_id, status, saved_at, saved_word_id);

-- Active user entitlement constraint for Free/Premium resolution
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_entitlements_active_user
    ON user_entitlements (user_id)
    WHERE status = 'ACTIVE';
