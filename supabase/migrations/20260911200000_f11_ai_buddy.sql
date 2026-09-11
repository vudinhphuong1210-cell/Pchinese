-- F11 AI Learning Buddy: approved schema extension for encrypted, learner-owned chat.
-- This migration intentionally follows DATA_short.md and the configured Flyway location.

CREATE TABLE user_data_keys (
    user_id uuid NOT NULL,
    wrapped_user_dek bytea NOT NULL,
    kms_key_reference varchar(255) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL,
    destroyed_at timestamptz NULL,
    CONSTRAINT pk_user_data_keys PRIMARY KEY (user_id),
    CONSTRAINT fk_user_data_keys_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_user_data_keys_status CHECK (status IN ('ACTIVE', 'DESTROYED'))
);

ALTER TABLE ai_conversations
    ADD COLUMN wrapped_conversation_dek bytea NULL,
    ADD COLUMN retention_notice_sent_at timestamptz NULL,
    ADD COLUMN conversation_key_destroyed_at timestamptz NULL;

ALTER TABLE ai_conversations
    ADD CONSTRAINT ck_ai_conversations_scenario
        CHECK (scenario_code IN ('DAILY_CONVERSATION', 'VOCABULARY_GRAMMAR', 'ROLE_PLAY')) NOT VALID,
    ADD CONSTRAINT ck_ai_conversations_active_key
        CHECK (status = 'DELETED' OR wrapped_conversation_dek IS NOT NULL) NOT VALID;

ALTER TABLE ai_messages
    ADD COLUMN vietnamese_explanation_ciphertext bytea NULL,
    ADD COLUMN suggestion_ciphertext bytea NULL,
    ADD COLUMN client_request_id uuid NULL,
    ADD COLUMN request_fingerprint char(64) NULL,
    ADD COLUMN failure_code varchar(100) NULL,
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE ai_messages
    ALTER COLUMN updated_at DROP DEFAULT;

CREATE UNIQUE INDEX uq_ai_messages_user_client_request
    ON ai_messages (user_id, client_request_id)
    WHERE client_request_id IS NOT NULL;
CREATE INDEX ix_ai_messages_context
    ON ai_messages (ai_conversation_id, status, sequence_no);
CREATE INDEX ix_ai_conversations_retention
    ON ai_conversations (status, last_message_at);

CREATE TABLE ai_processing_audit_events (
    ai_processing_audit_event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    ai_conversation_id uuid NULL,
    correlation_id uuid NOT NULL,
    provider_code varchar(64) NOT NULL,
    model_code varchar(128) NOT NULL,
    transfer_region varchar(64) NOT NULL,
    event_type varchar(40) NOT NULL,
    outcome varchar(20) NOT NULL,
    safe_reason_code varchar(100) NULL,
    provider_request_reference_hash char(64) NULL,
    occurred_at timestamptz NOT NULL,
    CONSTRAINT pk_ai_processing_audit_events PRIMARY KEY (ai_processing_audit_event_id),
    CONSTRAINT fk_ai_processing_audit_events_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ai_processing_audit_events_conversation FOREIGN KEY (ai_conversation_id) REFERENCES ai_conversations (ai_conversation_id),
    CONSTRAINT ck_ai_processing_audit_event_type CHECK (event_type IN ('REQUEST_DISPATCHED', 'RESPONSE_RECEIVED', 'REQUEST_REJECTED', 'DELETION_ATTESTED')),
    CONSTRAINT ck_ai_processing_audit_outcome CHECK (outcome IN ('SUCCESS', 'REJECTED', 'FAILED'))
);

CREATE INDEX ix_ai_processing_audit_events_correlation
    ON ai_processing_audit_events (correlation_id);
CREATE INDEX ix_ai_processing_audit_events_conversation_occurred
    ON ai_processing_audit_events (ai_conversation_id, occurred_at DESC);
