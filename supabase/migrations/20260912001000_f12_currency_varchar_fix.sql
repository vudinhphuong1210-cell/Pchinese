-- Correct fixed-width ISO currency codes to Hibernate's varchar mapping.
-- This follow-up is intentionally separate: the F12 base migration may already
-- have been applied in an environment, so its checksum must remain immutable.
ALTER TABLE public.plan_policy_versions
    ALTER COLUMN currency TYPE character varying(3);

ALTER TABLE public.ai_operational_measurements
    ALTER COLUMN cost_currency TYPE character varying(3);
