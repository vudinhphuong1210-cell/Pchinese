-- ==============================================================================
-- SEED TEST ACCOUNTS FOR ACTIVE VERIFIED LEARNERS AND ADMINS
-- Password for all: Password123@
-- ==============================================================================

DO $$
DECLARE
    v_sys_user_id uuid := '00000000-0000-0000-0000-000000000001';
    v_admin_user_id uuid := '00000000-0000-0000-0000-000000000002';
    v_learner_user_id uuid := '00000000-0000-0000-0000-000000000003';
    v_free_plan_id uuid;
BEGIN

    -- Admin user (admin@pchinese.net / Password123@)
    INSERT INTO public.users (
        user_id, email_ciphertext, email_lookup_hash, password_hash, status, 
        email_verified_at, last_activity_at, created_at, updated_at
    ) VALUES (
        v_admin_user_id, 
        '\x3558d6efe3bfafd1db027d2647168c4b563b1f3e0a90ddd800d396aea3cb13a409bf32eca60c67dea7c92a0b61f5', 
        '885509de81e74052182ef9c3a4b56b7c6dcd721abbb6b1e0401ff557c6abaeb4', 
        '$2a$12$YtEXEMz.XHvAaebebWegSe9fuQqPRkeM87eAEv.IBIhoqPRStSr86', 
        'ACTIVE', now(), now(), now(), now()
    ) ON CONFLICT (user_id) DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        status = 'ACTIVE',
        email_verified_at = now();

    -- Learner user (learner@pchinese.net / Password123@)
    INSERT INTO public.users (
        user_id, email_ciphertext, email_lookup_hash, password_hash, status, 
        email_verified_at, last_activity_at, created_at, updated_at
    ) VALUES (
        v_learner_user_id, 
        '\x7aadda0440297059052aec19ab863d85d3b338f77e187d3073ee23787dcf58e46fdabdd069265195f178aace27c1f994', 
        '8fc45194ead4625bfb485047643f2aefee32a8fc538dc64b8389ce600d3143c8', 
        '$2a$12$YtEXEMz.XHvAaebebWegSe9fuQqPRkeM87eAEv.IBIhoqPRStSr86', 
        'ACTIVE', now(), now(), now(), now()
    ) ON CONFLICT (user_id) DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        status = 'ACTIVE',
        email_verified_at = now();

    -- Grant ADMIN role to admin user
    IF NOT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = v_admin_user_id AND role_code = 'ADMIN' AND revoked_at IS NULL) THEN
        INSERT INTO public.user_roles (
            user_role_grant_id, user_id, role_code, granted_by_user_id, granted_at, correlation_id
        ) VALUES (
            gen_random_uuid(), v_admin_user_id, 'ADMIN', v_sys_user_id, now(), gen_random_uuid()
        );
    END IF;

    -- Assign FREE entitlement
    SELECT subscription_plan_id INTO v_free_plan_id FROM public.subscription_plans WHERE plan_code = 'FREE' LIMIT 1;
    IF v_free_plan_id IS NOT NULL THEN
        INSERT INTO public.user_entitlements (
            user_entitlement_id, user_id, subscription_plan_id, status, source_type, starts_at, ai_quota_period_started_at, created_at, updated_at
        ) VALUES 
        (gen_random_uuid(), v_admin_user_id, v_free_plan_id, 'ACTIVE', 'DEFAULT', now(), now(), now(), now()),
        (gen_random_uuid(), v_learner_user_id, v_free_plan_id, 'ACTIVE', 'DEFAULT', now(), now(), now(), now())
        ON CONFLICT (user_id) WHERE status = 'ACTIVE' DO NOTHING;
    END IF;

END $$;
