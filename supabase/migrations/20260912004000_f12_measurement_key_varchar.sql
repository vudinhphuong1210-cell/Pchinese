-- Hibernate maps the operational measurement HMAC key as varchar(64).
-- Keep the applied F12 base migration immutable and correct the live schema here.
ALTER TABLE public.ai_operational_measurements
    ALTER COLUMN measurement_key TYPE character varying(64);
