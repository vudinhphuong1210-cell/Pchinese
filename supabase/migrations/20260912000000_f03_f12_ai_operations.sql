-- =============================================================================
-- F03/F12: versioned plan policy, allowance-cycle and private AI operations data.
-- This is the one consolidated schema change for the unmerged F03/F12 boundary.
-- =============================================================================

CREATE TABLE plan_policy_versions (
    plan_policy_version_id uuid           NOT NULL DEFAULT gen_random_uuid(),
    subscription_plan_id   uuid           NOT NULL,
    revision_number        integer        NOT NULL,
    status                 varchar(16)    NOT NULL,
    display_name           varchar(120)   NOT NULL,
    description            varchar(2000),
    benefits               jsonb          NOT NULL DEFAULT '[]'::jsonb,
    availability_state     varchar(16)    NOT NULL,
    price_amount           numeric(19, 4) NOT NULL,
    currency               char(3)        NOT NULL,
    price_interval         varchar(16)    NOT NULL,
    display_label          varchar(120),
    allowance_units        integer        NOT NULL,
    allowance_period       varchar(16)    NOT NULL,
    reason                 varchar(500)   NOT NULL,
    created_by_user_id     uuid,
    created_at             timestamptz    NOT NULL DEFAULT now(),
    retired_at             timestamptz,
    version                bigint         NOT NULL DEFAULT 0,
    CONSTRAINT pk_plan_policy_versions PRIMARY KEY (plan_policy_version_id),
    CONSTRAINT fk_plan_policy_versions_plan FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans (subscription_plan_id),
    CONSTRAINT fk_plan_policy_versions_actor FOREIGN KEY (created_by_user_id) REFERENCES users (user_id),
    CONSTRAINT uq_plan_policy_versions_plan_revision UNIQUE (subscription_plan_id, revision_number),
    CONSTRAINT ck_plan_policy_versions_revision CHECK (revision_number >= 1),
    CONSTRAINT ck_plan_policy_versions_status CHECK (status IN ('PUBLISHED', 'SUPERSEDED', 'RETIRED')),
    CONSTRAINT ck_plan_policy_versions_benefits CHECK (jsonb_typeof(benefits) = 'array'),
    CONSTRAINT ck_plan_policy_versions_availability CHECK (availability_state IN ('ACTIVE', 'HIDDEN')),
    CONSTRAINT ck_plan_policy_versions_price CHECK (price_amount >= 0),
    CONSTRAINT ck_plan_policy_versions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_plan_policy_versions_interval CHECK (price_interval IN ('MONTH', 'YEAR')),
    CONSTRAINT ck_plan_policy_versions_allowance CHECK (allowance_units >= 0),
    CONSTRAINT ck_plan_policy_versions_period CHECK (allowance_period IN ('DAY', 'MONTH'))
);

CREATE UNIQUE INDEX uq_plan_policy_versions_current
    ON plan_policy_versions (subscription_plan_id)
    WHERE status = 'PUBLISHED';

-- Existing stable plan rows become the first system-imported policy snapshots. Every command
-- created after this migration supplies an authenticated ADMIN actor and reason.
INSERT INTO plan_policy_versions (
    subscription_plan_id, revision_number, status, display_name, description, benefits,
    availability_state, price_amount, currency, price_interval, display_label,
    allowance_units, allowance_period, reason, created_at
)
SELECT
    subscription_plan_id,
    1,
    'PUBLISHED',
    display_name,
    NULL,
    '[]'::jsonb,
    CASE WHEN plan_code = 'FREE' THEN 'ACTIVE' ELSE 'HIDDEN' END,
    0,
    'USD',
    'MONTH',
    NULL,
    ai_quota_units,
    ai_quota_period,
    'SYSTEM_IMPORT',
    created_at
FROM subscription_plans;

ALTER TABLE user_entitlements
    ADD COLUMN current_policy_version_id uuid;

UPDATE user_entitlements entitlement
SET current_policy_version_id = policy.plan_policy_version_id
FROM plan_policy_versions policy
WHERE policy.subscription_plan_id = entitlement.subscription_plan_id
  AND policy.status = 'PUBLISHED';

ALTER TABLE user_entitlements
    ALTER COLUMN current_policy_version_id SET NOT NULL,
    ADD CONSTRAINT fk_user_entitlements_policy
        FOREIGN KEY (current_policy_version_id) REFERENCES plan_policy_versions (plan_policy_version_id);

CREATE TABLE entitlement_allowance_cycles (
    entitlement_allowance_cycle_id uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_entitlement_id            uuid        NOT NULL,
    plan_policy_version_id         uuid        NOT NULL,
    cycle_started_at               timestamptz NOT NULL,
    cycle_ends_at                  timestamptz NOT NULL,
    allowance_limit                integer     NOT NULL,
    allowance_period               varchar(16) NOT NULL,
    used_units                     integer     NOT NULL DEFAULT 0,
    status                         varchar(16) NOT NULL,
    created_at                     timestamptz NOT NULL DEFAULT now(),
    closed_at                      timestamptz,
    version                        bigint      NOT NULL DEFAULT 0,
    CONSTRAINT pk_entitlement_allowance_cycles PRIMARY KEY (entitlement_allowance_cycle_id),
    CONSTRAINT fk_entitlement_allowance_cycles_entitlement FOREIGN KEY (user_entitlement_id) REFERENCES user_entitlements (user_entitlement_id),
    CONSTRAINT fk_entitlement_allowance_cycles_policy FOREIGN KEY (plan_policy_version_id) REFERENCES plan_policy_versions (plan_policy_version_id),
    CONSTRAINT ck_entitlement_allowance_cycles_order CHECK (cycle_ends_at > cycle_started_at),
    CONSTRAINT ck_entitlement_allowance_cycles_limit CHECK (allowance_limit >= 0),
    CONSTRAINT ck_entitlement_allowance_cycles_used CHECK (used_units >= 0),
    CONSTRAINT ck_entitlement_allowance_cycles_period CHECK (allowance_period IN ('DAY', 'MONTH')),
    CONSTRAINT ck_entitlement_allowance_cycles_status CHECK (status IN ('CURRENT', 'CLOSED'))
);

CREATE UNIQUE INDEX uq_entitlement_allowance_cycles_current
    ON entitlement_allowance_cycles (user_entitlement_id)
    WHERE status = 'CURRENT';
CREATE INDEX ix_entitlement_allowance_cycles_boundary
    ON entitlement_allowance_cycles (status, cycle_ends_at);

INSERT INTO entitlement_allowance_cycles (
    user_entitlement_id, plan_policy_version_id, cycle_started_at, cycle_ends_at,
    allowance_limit, allowance_period, used_units, status, created_at
)
SELECT
    entitlement.user_entitlement_id,
    entitlement.current_policy_version_id,
    entitlement.ai_quota_period_started_at,
    CASE policy.allowance_period
        WHEN 'DAY' THEN entitlement.ai_quota_period_started_at + INTERVAL '1 day'
        ELSE entitlement.ai_quota_period_started_at + INTERVAL '1 month'
    END,
    policy.allowance_units,
    policy.allowance_period,
    entitlement.ai_used_units,
    'CURRENT',
    entitlement.created_at
FROM user_entitlements entitlement
JOIN plan_policy_versions policy ON policy.plan_policy_version_id = entitlement.current_policy_version_id;

ALTER TABLE ai_usage_events
    ADD COLUMN entitlement_allowance_cycle_id uuid,
    ADD COLUMN plan_policy_version_id uuid;

UPDATE ai_usage_events usage_event
SET entitlement_allowance_cycle_id = cycle.entitlement_allowance_cycle_id,
    plan_policy_version_id = cycle.plan_policy_version_id
FROM entitlement_allowance_cycles cycle
WHERE cycle.user_entitlement_id = usage_event.user_entitlement_id
  AND cycle.status = 'CURRENT';

ALTER TABLE ai_usage_events
    ALTER COLUMN entitlement_allowance_cycle_id SET NOT NULL,
    ALTER COLUMN plan_policy_version_id SET NOT NULL,
    ADD CONSTRAINT fk_ai_usage_events_cycle
        FOREIGN KEY (entitlement_allowance_cycle_id) REFERENCES entitlement_allowance_cycles (entitlement_allowance_cycle_id),
    ADD CONSTRAINT fk_ai_usage_events_policy
        FOREIGN KEY (plan_policy_version_id) REFERENCES plan_policy_versions (plan_policy_version_id);

CREATE INDEX ix_ai_usage_events_cycle_policy_status
    ON ai_usage_events (entitlement_allowance_cycle_id, plan_policy_version_id, status, created_at DESC);

CREATE TABLE ai_operational_measurements (
    ai_operational_measurement_id uuid           NOT NULL DEFAULT gen_random_uuid(),
    measurement_key               char(64)       NOT NULL,
    ai_usage_event_id             uuid,
    plan_policy_version_id        uuid           NOT NULL,
    capability                    varchar(32)    NOT NULL,
    outcome                       varchar(32)    NOT NULL,
    reserved_units                integer        NOT NULL DEFAULT 0,
    used_units                    integer        NOT NULL DEFAULT 0,
    refunded_units                integer        NOT NULL DEFAULT 0,
    input_tokens                  bigint,
    output_tokens                 bigint,
    total_tokens                  bigint,
    estimated_cost                numeric(19, 6),
    cost_currency                 char(3),
    provider_duration_ms          bigint,
    end_to_end_duration_ms        bigint,
    failure_class                 varchar(32),
    occurred_at                   timestamptz    NOT NULL,
    finalized_at                  timestamptz,
    CONSTRAINT pk_ai_operational_measurements PRIMARY KEY (ai_operational_measurement_id),
    CONSTRAINT uq_ai_operational_measurements_key UNIQUE (measurement_key),
    CONSTRAINT uq_ai_operational_measurements_usage_event UNIQUE (ai_usage_event_id),
    CONSTRAINT fk_ai_operational_measurements_usage_event FOREIGN KEY (ai_usage_event_id) REFERENCES ai_usage_events (ai_usage_event_id),
    CONSTRAINT fk_ai_operational_measurements_policy FOREIGN KEY (plan_policy_version_id) REFERENCES plan_policy_versions (plan_policy_version_id),
    CONSTRAINT ck_ai_operational_measurements_capability CHECK (capability IN ('AI_BUDDY', 'SHADOWING_ASSESSMENT')),
    CONSTRAINT ck_ai_operational_measurements_outcome CHECK (outcome IN ('PENDING', 'SUCCEEDED', 'QUOTA_DENIED', 'FAILED_REFUNDED', 'FAILED_CONSUMED')),
    CONSTRAINT ck_ai_operational_measurements_units CHECK (reserved_units >= 0 AND used_units >= 0 AND refunded_units >= 0),
    CONSTRAINT ck_ai_operational_measurements_tokens CHECK ((input_tokens IS NULL OR input_tokens >= 0) AND (output_tokens IS NULL OR output_tokens >= 0) AND (total_tokens IS NULL OR total_tokens >= 0)),
    CONSTRAINT ck_ai_operational_measurements_cost CHECK ((estimated_cost IS NULL) = (cost_currency IS NULL) AND (estimated_cost IS NULL OR estimated_cost >= 0) AND (cost_currency IS NULL OR cost_currency ~ '^[A-Z]{3}$')),
    CONSTRAINT ck_ai_operational_measurements_duration CHECK ((provider_duration_ms IS NULL OR provider_duration_ms >= 0) AND (end_to_end_duration_ms IS NULL OR end_to_end_duration_ms >= 0))
);

CREATE INDEX ix_ai_operational_measurements_report
    ON ai_operational_measurements (finalized_at, plan_policy_version_id, capability, outcome);
CREATE INDEX ix_ai_operational_measurements_retention
    ON ai_operational_measurements (occurred_at);

CREATE TABLE ai_monitoring_rules (
    ai_monitoring_rule_id uuid           NOT NULL DEFAULT gen_random_uuid(),
    metric                varchar(32)    NOT NULL,
    threshold             numeric(19, 6) NOT NULL,
    evaluation_window     varchar(16)    NOT NULL,
    subscription_plan_id  uuid,
    plan_policy_version_id uuid,
    capability            varchar(32),
    enabled               boolean        NOT NULL,
    created_by_user_id    uuid           NOT NULL,
    updated_by_user_id    uuid           NOT NULL,
    created_at            timestamptz    NOT NULL DEFAULT now(),
    updated_at            timestamptz    NOT NULL DEFAULT now(),
    version               bigint         NOT NULL DEFAULT 0,
    CONSTRAINT pk_ai_monitoring_rules PRIMARY KEY (ai_monitoring_rule_id),
    CONSTRAINT fk_ai_monitoring_rules_plan FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans (subscription_plan_id),
    CONSTRAINT fk_ai_monitoring_rules_policy FOREIGN KEY (plan_policy_version_id) REFERENCES plan_policy_versions (plan_policy_version_id),
    CONSTRAINT fk_ai_monitoring_rules_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ai_monitoring_rules_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_ai_monitoring_rules_metric CHECK (metric IN ('TOKEN_VOLUME', 'ESTIMATED_COST', 'REQUEST_VOLUME', 'QUOTA_DENIAL_RATE', 'FAILURE_RATE', 'RESPONSE_TIME')),
    CONSTRAINT ck_ai_monitoring_rules_threshold CHECK (threshold >= 0),
    CONSTRAINT ck_ai_monitoring_rules_window CHECK (evaluation_window IN ('ONE_HOUR', 'TWENTY_FOUR_HOURS')),
    CONSTRAINT ck_ai_monitoring_rules_capability CHECK (capability IS NULL OR capability IN ('AI_BUDDY', 'SHADOWING_ASSESSMENT'))
);
CREATE INDEX ix_ai_monitoring_rules_enabled_scope
    ON ai_monitoring_rules (enabled, evaluation_window, subscription_plan_id, plan_policy_version_id, capability);

CREATE TABLE ai_monitoring_alerts (
    ai_monitoring_alert_id  uuid           NOT NULL DEFAULT gen_random_uuid(),
    ai_monitoring_rule_id   uuid           NOT NULL,
    state                   varchar(16)    NOT NULL,
    metric                  varchar(32)    NOT NULL,
    threshold               numeric(19, 6) NOT NULL,
    latest_value            numeric(19, 6) NOT NULL,
    partial_data            boolean        NOT NULL DEFAULT false,
    first_seen_at           timestamptz    NOT NULL,
    last_evaluated_at       timestamptz    NOT NULL,
    acknowledged_by_user_id uuid,
    acknowledged_at         timestamptz,
    acknowledgement_note    varchar(500),
    resolved_at             timestamptz,
    version                 bigint         NOT NULL DEFAULT 0,
    CONSTRAINT pk_ai_monitoring_alerts PRIMARY KEY (ai_monitoring_alert_id),
    CONSTRAINT fk_ai_monitoring_alerts_rule FOREIGN KEY (ai_monitoring_rule_id) REFERENCES ai_monitoring_rules (ai_monitoring_rule_id),
    CONSTRAINT fk_ai_monitoring_alerts_actor FOREIGN KEY (acknowledged_by_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_ai_monitoring_alerts_state CHECK (state IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT ck_ai_monitoring_alerts_values CHECK (threshold >= 0 AND latest_value >= 0)
);
CREATE UNIQUE INDEX uq_ai_monitoring_alerts_active_rule
    ON ai_monitoring_alerts (ai_monitoring_rule_id)
    WHERE state IN ('OPEN', 'ACKNOWLEDGED');
CREATE INDEX ix_ai_monitoring_alerts_rule_state
    ON ai_monitoring_alerts (ai_monitoring_rule_id, state, last_evaluated_at DESC);

CREATE TABLE ai_admin_audit_events (
    ai_admin_audit_event_id uuid           NOT NULL DEFAULT gen_random_uuid(),
    actor_user_id           uuid           NOT NULL,
    event_type              varchar(32)    NOT NULL,
    target_type             varchar(32)    NOT NULL,
    target_id               uuid           NOT NULL,
    correlation_id          uuid,
    outcome                 varchar(16)    NOT NULL,
    reason_or_note          varchar(500),
    safe_before             jsonb,
    safe_after              jsonb,
    occurred_at             timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_admin_audit_events PRIMARY KEY (ai_admin_audit_event_id),
    CONSTRAINT fk_ai_admin_audit_events_actor FOREIGN KEY (actor_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_ai_admin_audit_events_type CHECK (event_type IN ('POLICY_PUBLISHED', 'PLAN_RETIRED', 'MONITORING_RULE_CHANGED', 'ALERT_ACKNOWLEDGED')),
    CONSTRAINT ck_ai_admin_audit_events_target CHECK (target_type IN ('PLAN_POLICY', 'MONITORING_RULE', 'MONITORING_ALERT')),
    CONSTRAINT ck_ai_admin_audit_events_outcome CHECK (outcome = 'SUCCESS'),
    CONSTRAINT ck_ai_admin_audit_events_before CHECK (safe_before IS NULL OR jsonb_typeof(safe_before) = 'object'),
    CONSTRAINT ck_ai_admin_audit_events_after CHECK (safe_after IS NULL OR jsonb_typeof(safe_after) = 'object')
);
CREATE INDEX ix_ai_admin_audit_events_target_occurred
    ON ai_admin_audit_events (target_type, target_id, occurred_at DESC);

CREATE FUNCTION prevent_ai_admin_audit_event_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ai_admin_audit_events are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ai_admin_audit_events_append_only
    BEFORE UPDATE OR DELETE ON ai_admin_audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_ai_admin_audit_event_mutation();
