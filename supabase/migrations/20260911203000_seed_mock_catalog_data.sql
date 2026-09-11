-- ==============================================================================
-- MOCK DATA FOR F05 CATALOG FEATURE TESTING
-- ==============================================================================

DO $$
DECLARE
    v_sys_user_id uuid := '00000000-0000-0000-0000-000000000001';
    
    v_topic1_id uuid := '11111111-1111-1111-1111-100000000001';
    v_topic2_id uuid := '11111111-1111-1111-1111-100000000002';
    
    v_lesson1_id uuid := '22222222-2222-2222-2222-200000000001';
    v_lesson2_id uuid := '22222222-2222-2222-2222-200000000002';
    v_lesson3_id uuid := '22222222-2222-2222-2222-200000000003';
    
    v_media1_id uuid := '33333333-3333-3333-3333-300000000001';
BEGIN

    -- 1. Create a dummy system user for created_by / updated_by references
    IF NOT EXISTS (SELECT 1 FROM public.users WHERE user_id = v_sys_user_id) THEN
        INSERT INTO public.users (
            user_id, email_ciphertext, email_lookup_hash, password_hash, status, 
            last_activity_at, created_at, updated_at
        ) VALUES (
            v_sys_user_id, '\x00', 'system@pchinese.net', 'nohash', 'ACTIVE', 
            now(), now(), now()
        );
    END IF;

    -- 2. Insert Topics
    INSERT INTO public.topics (
        topic_id, slug, title, description, hsk_level, sort_order, publication_state, 
        created_by_user_id, updated_by_user_id, published_at, created_at, updated_at
    ) VALUES 
    (
        v_topic1_id, 'giao-tiep-hang-ngay', 'Giao tiếp hàng ngày', 'Các tình huống giao tiếp cơ bản trong cuộc sống', 
        1, 1, 'PUBLISHED', v_sys_user_id, v_sys_user_id, now(), now(), now()
    ),
    (
        v_topic2_id, 'kinh-doanh-cong-so', 'Tiếng Trung Công Sở', 'Từ vựng và mẫu câu dùng trong môi trường văn phòng', 
        3, 2, 'PUBLISHED', v_sys_user_id, v_sys_user_id, now(), now(), now()
    )
    ON CONFLICT (topic_id) DO NOTHING;

    -- 3. Insert Lessons
    INSERT INTO public.lessons (
        lesson_id, topic_id, slug, title, summary, hsk_level, lesson_type, access_level, 
        publication_state, sort_order, estimated_duration_seconds, completion_min_percent,
        created_by_user_id, updated_by_user_id, published_at, created_at, updated_at
    ) VALUES 
    (
        v_lesson1_id, v_topic1_id, 'chao-hoi-co-ban', 'Bài 1: Chào hỏi cơ bản', 'Cách chào hỏi và làm quen khi lần đầu gặp mặt.', 
        1, 'VIDEO', 'FREE', 'PUBLISHED', 1, 300, 80,
        v_sys_user_id, v_sys_user_id, now(), now(), now()
    ),
    (
        v_lesson2_id, v_topic1_id, 'mua-sam-mac-ca', 'Bài 2: Mua sắm & Mặc cả', 'Các mẫu câu đi chợ, hỏi giá và mặc cả.', 
        2, 'VIDEO', 'FREE', 'PUBLISHED', 2, 420, 80,
        v_sys_user_id, v_sys_user_id, now(), now(), now()
    ),
    (
        v_lesson3_id, v_topic2_id, 'phong-van-xin-viec', 'Bài 1: Phỏng vấn xin việc', 'Giới thiệu bản thân và trả lời phỏng vấn bằng tiếng Trung.', 
        3, 'VIDEO', 'FREE', 'PUBLISHED', 1, 600, 80,
        v_sys_user_id, v_sys_user_id, now(), now(), now()
    )
    ON CONFLICT (lesson_id) DO NOTHING;

    -- 4. Insert Dummy Media Asset for Segments
    INSERT INTO public.media_assets (
        media_asset_id, provider_name, provider_asset_identifier, media_kind, 
        approval_status, malware_scan_status, created_by_user_id, created_at, updated_at
    ) VALUES (
        v_media1_id, 'YOUTUBE', 'dQw4w9WgXcQ', 'VIDEO', 
        'APPROVED', 'CLEAN', v_sys_user_id, now(), now()
    )
    ON CONFLICT (media_asset_id) DO NOTHING;

    -- 5. Insert Segments for Lesson 1 (just to have segments count > 0)
    INSERT INTO public.segments (
        segment_id, lesson_id, media_asset_id, sequence_no, segment_type, 
        publication_state, start_milliseconds, end_milliseconds, 
        transcript_hanzi, transcript_pinyin, translation_vi, 
        created_by_user_id, updated_by_user_id, created_at, updated_at
    ) VALUES 
    (
        gen_random_uuid(), v_lesson1_id, v_media1_id, 1, 'BOTH', 
        'PUBLISHED', 0, 5000, 
        '你好！很高兴认识你。', 'Nǐ hǎo! Hěn gāoxìng rènshí nǐ.', 'Xin chào! Rất vui được gặp bạn.', 
        v_sys_user_id, v_sys_user_id, now(), now()
    ),
    (
        gen_random_uuid(), v_lesson1_id, v_media1_id, 2, 'BOTH', 
        'PUBLISHED', 5000, 10000, 
        '我叫林浩。', 'Wǒ jiào Lín Hào.', 'Tôi tên là Lâm Hạo.', 
        v_sys_user_id, v_sys_user_id, now(), now()
    )
    ON CONFLICT DO NOTHING;

END $$;
