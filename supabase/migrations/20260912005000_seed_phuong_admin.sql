-- Development administrator requested for local Supabase-backed verification.
-- The email is encrypted with the configured development AES key; the lookup
-- value follows SensitiveValueService.hashEmail("phuong@gmail.com").
DO $$
DECLARE
    v_user_id uuid;
BEGIN
    INSERT INTO public.users (
        user_id,
        email_ciphertext,
        email_lookup_hash,
        password_hash,
        status,
        email_verified_at,
        last_activity_at,
        created_at,
        updated_at
    ) VALUES (
        gen_random_uuid(),
        '\\xa02fc15e246fd415ba599408b5163cf80922d023e248e16a1aa4d0dfd7a12cd40626600140d6329f7e909484',
        '3c7b48c059fe0373785ec7bc420cce180031d2bb41bb62244da7ddc39e7c4d9d',
        '$2a$12$gEchzh4N.4EBS4RsNHTnQeVjZAd8KbMRS08z2Ul3HcOYgdKwXtQ6m',
        'ACTIVE', now(), now(), now(), now()
    )
    ON CONFLICT (email_lookup_hash) DO UPDATE SET
        email_ciphertext = EXCLUDED.email_ciphertext,
        password_hash = EXCLUDED.password_hash,
        status = 'ACTIVE',
        email_verified_at = now(),
        last_activity_at = now(),
        updated_at = now(),
        authz_version = public.users.authz_version + 1
    RETURNING user_id INTO v_user_id;

    IF NOT EXISTS (
        SELECT 1 FROM public.user_roles
        WHERE user_id = v_user_id AND role_code = 'ADMIN' AND revoked_at IS NULL
    ) THEN
        INSERT INTO public.user_roles (
            user_role_grant_id, user_id, role_code, granted_by_user_id, granted_at, correlation_id
        ) VALUES (
            gen_random_uuid(), v_user_id, 'ADMIN', NULL, now(), gen_random_uuid()
        );
    END IF;
END $$;
