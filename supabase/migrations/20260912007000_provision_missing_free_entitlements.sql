-- The development account seed is applied after the F03 policy migration, so
-- it bypasses normal account activation and does not receive a Free
-- entitlement. Provision that required F03 state for every active user that
-- does not already have an active entitlement.

WITH free_policy AS (
    SELECT
        plan.subscription_plan_id,
        policy.plan_policy_version_id,
        policy.allowance_units,
        policy.allowance_period
    FROM subscription_plans plan
    JOIN plan_policy_versions policy
      ON policy.subscription_plan_id = plan.subscription_plan_id
     AND policy.status = 'PUBLISHED'
    WHERE plan.plan_code = 'FREE'
)
INSERT INTO user_entitlements (
    user_entitlement_id,
    user_id,
    subscription_plan_id,
    current_policy_version_id,
    status,
    source_type,
    starts_at,
    ends_at,
    ai_quota_period_started_at,
    ai_used_units,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    app_user.user_id,
    free_policy.subscription_plan_id,
    free_policy.plan_policy_version_id,
    'ACTIVE',
    'DEFAULT',
    now(),
    NULL,
    now(),
    0,
    now(),
    now(),
    0
FROM users app_user
CROSS JOIN free_policy
WHERE app_user.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM user_entitlements entitlement
      WHERE entitlement.user_id = app_user.user_id
        AND entitlement.status = 'ACTIVE'
  );

INSERT INTO entitlement_allowance_cycles (
    entitlement_allowance_cycle_id,
    user_entitlement_id,
    plan_policy_version_id,
    cycle_started_at,
    cycle_ends_at,
    allowance_limit,
    allowance_period,
    used_units,
    status,
    created_at,
    version
)
SELECT
    gen_random_uuid(),
    entitlement.user_entitlement_id,
    policy.plan_policy_version_id,
    entitlement.ai_quota_period_started_at,
    CASE policy.allowance_period
        WHEN 'DAY' THEN entitlement.ai_quota_period_started_at + INTERVAL '1 day'
        ELSE entitlement.ai_quota_period_started_at + INTERVAL '1 month'
    END,
    policy.allowance_units,
    policy.allowance_period,
    0,
    'CURRENT',
    now(),
    0
FROM user_entitlements entitlement
JOIN plan_policy_versions policy
  ON policy.plan_policy_version_id = entitlement.current_policy_version_id
WHERE entitlement.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM entitlement_allowance_cycles cycle
      WHERE cycle.user_entitlement_id = entitlement.user_entitlement_id
        AND cycle.status = 'CURRENT'
  );
