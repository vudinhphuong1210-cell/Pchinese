-- Email lookup hashes must be stable across deployments so canonical seeded
-- credentials do not depend on a deployment-specific token pepper.
ALTER TABLE public.users
    ALTER COLUMN email_lookup_hash TYPE character varying(64);

UPDATE public.users
SET email_lookup_hash = '8a9b83999bd44e8c628a2185fbc01280ff3bdcc9d4553f314c965c1e2b399113',
    updated_at = now()
WHERE user_id = '00000000-0000-0000-0000-000000000002';

UPDATE public.users
SET email_lookup_hash = '763e72c86cc76610c4fd0cdf962376d60988387c972978193c422e6ffb5fd465',
    updated_at = now()
WHERE user_id = '00000000-0000-0000-0000-000000000003';
