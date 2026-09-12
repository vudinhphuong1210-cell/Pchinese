-- Seed a small published Chinese-Vietnamese dictionary so F09/F10 are usable
-- in the local shared environment. The content is intentionally limited to
-- common HSK 1 words and has no media dependencies.

INSERT INTO dictionary_entries (
    dictionary_entry_id,
    simplified_hanzi,
    traditional_hanzi,
    normalized_hanzi,
    primary_pinyin,
    normalized_pinyin,
    hsk_level,
    word_type,
    senses,
    publication_state,
    created_at,
    updated_at,
    version
) VALUES
    ('00000000-0000-4000-9000-000000000001', '你好', '你好', '你好', 'nǐ hǎo', 'nihao', 1, 'interjection', '[{"meaning_vi":"xin chào","examples":[{"zh":"你好！","vi":"Xin chào!"}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000002', '谢谢', '謝謝', '谢谢', 'xiè xie', 'xiexie', 1, 'verb', '[{"meaning_vi":"cảm ơn","examples":[{"zh":"谢谢你。","vi":"Cảm ơn bạn."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000003', '再见', '再見', '再见', 'zài jiàn', 'zaijian', 1, 'interjection', '[{"meaning_vi":"tạm biệt","examples":[{"zh":"明天再见。","vi":"Ngày mai gặp lại."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000004', '学生', '學生', '学生', 'xué shēng', 'xuesheng', 1, 'noun', '[{"meaning_vi":"học sinh, sinh viên","examples":[{"zh":"我是学生。","vi":"Tôi là học sinh."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000005', '老师', '老師', '老师', 'lǎo shī', 'laoshi', 1, 'noun', '[{"meaning_vi":"giáo viên","examples":[{"zh":"她是老师。","vi":"Cô ấy là giáo viên."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000006', '学校', '學校', '学校', 'xué xiào', 'xuexiao', 1, 'noun', '[{"meaning_vi":"trường học","examples":[{"zh":"学校很大。","vi":"Trường học rất lớn."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000007', '朋友', '朋友', '朋友', 'péng you', 'pengyou', 1, 'noun', '[{"meaning_vi":"bạn bè","examples":[{"zh":"他是我的朋友。","vi":"Anh ấy là bạn của tôi."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000008', '家', '家', '家', 'jiā', 'jia', 1, 'noun', '[{"meaning_vi":"nhà, gia đình","examples":[{"zh":"我回家。","vi":"Tôi về nhà."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000009', '吃', '吃', '吃', 'chī', 'chi', 1, 'verb', '[{"meaning_vi":"ăn","examples":[{"zh":"我吃米饭。","vi":"Tôi ăn cơm."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000010', '喝', '喝', '喝', 'hē', 'he', 1, 'verb', '[{"meaning_vi":"uống","examples":[{"zh":"我喝水。","vi":"Tôi uống nước."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000011', '学习', '學習', '学习', 'xué xí', 'xuexi', 1, 'verb', '[{"meaning_vi":"học tập","examples":[{"zh":"我学习中文。","vi":"Tôi học tiếng Trung."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0),
    ('00000000-0000-4000-9000-000000000012', '中文', '中文', '中文', 'zhōng wén', 'zhongwen', 1, 'noun', '[{"meaning_vi":"tiếng Trung","examples":[{"zh":"我学中文。","vi":"Tôi học tiếng Trung."}]}]'::jsonb, 'PUBLISHED', now(), now(), 0)
ON CONFLICT (normalized_hanzi, normalized_pinyin) DO NOTHING;

-- Generate the same complete/prefix/interior keys used by dictionary search.
INSERT INTO dictionary_search_keys (dictionary_entry_id, query_kind, normalized_key, match_rank, created_at)
SELECT
    entry.dictionary_entry_id,
    'SIMPLIFIED_HANZI',
    substr(entry.simplified_hanzi, start_position, end_position - start_position + 1),
    CASE
        WHEN start_position = 1 AND end_position = char_length(entry.simplified_hanzi) THEN 1
        WHEN start_position = 1 THEN 2
        ELSE 3
    END,
    now()
FROM dictionary_entries entry
CROSS JOIN LATERAL generate_series(1, char_length(entry.simplified_hanzi)) AS start_position
CROSS JOIN LATERAL generate_series(start_position, char_length(entry.simplified_hanzi)) AS end_position
WHERE entry.dictionary_entry_id BETWEEN '00000000-0000-4000-9000-000000000001' AND '00000000-0000-4000-9000-000000000012'
ON CONFLICT (dictionary_entry_id, query_kind, normalized_key) DO NOTHING;

INSERT INTO dictionary_search_keys (dictionary_entry_id, query_kind, normalized_key, match_rank, created_at)
SELECT
    entry.dictionary_entry_id,
    'TRADITIONAL_HANZI',
    substr(entry.traditional_hanzi, start_position, end_position - start_position + 1),
    CASE
        WHEN start_position = 1 AND end_position = char_length(entry.traditional_hanzi) THEN 1
        WHEN start_position = 1 THEN 2
        ELSE 3
    END,
    now()
FROM dictionary_entries entry
CROSS JOIN LATERAL generate_series(1, char_length(entry.traditional_hanzi)) AS start_position
CROSS JOIN LATERAL generate_series(start_position, char_length(entry.traditional_hanzi)) AS end_position
WHERE entry.dictionary_entry_id BETWEEN '00000000-0000-4000-9000-000000000001' AND '00000000-0000-4000-9000-000000000012'
ON CONFLICT (dictionary_entry_id, query_kind, normalized_key) DO NOTHING;

INSERT INTO dictionary_search_keys (dictionary_entry_id, query_kind, normalized_key, match_rank, created_at)
SELECT
    entry.dictionary_entry_id,
    'PINYIN',
    substr(entry.normalized_pinyin, start_position, end_position - start_position + 1),
    CASE
        WHEN start_position = 1 AND end_position = char_length(entry.normalized_pinyin) THEN 1
        WHEN start_position = 1 THEN 2
        ELSE 3
    END,
    now()
FROM dictionary_entries entry
CROSS JOIN LATERAL generate_series(1, char_length(entry.normalized_pinyin)) AS start_position
CROSS JOIN LATERAL generate_series(start_position, char_length(entry.normalized_pinyin)) AS end_position
WHERE entry.dictionary_entry_id BETWEEN '00000000-0000-4000-9000-000000000001' AND '00000000-0000-4000-9000-000000000012'
ON CONFLICT (dictionary_entry_id, query_kind, normalized_key) DO NOTHING;

INSERT INTO dictionary_search_keys (dictionary_entry_id, query_kind, normalized_key, match_rank, created_at)
VALUES
    ('00000000-0000-4000-9000-000000000001', 'VIETNAMESE_KEYWORD', 'xin', 1, now()),
    ('00000000-0000-4000-9000-000000000001', 'VIETNAMESE_KEYWORD', 'chao', 1, now()),
    ('00000000-0000-4000-9000-000000000002', 'VIETNAMESE_KEYWORD', 'cam', 1, now()),
    ('00000000-0000-4000-9000-000000000002', 'VIETNAMESE_KEYWORD', 'on', 1, now()),
    ('00000000-0000-4000-9000-000000000003', 'VIETNAMESE_KEYWORD', 'tam', 1, now()),
    ('00000000-0000-4000-9000-000000000003', 'VIETNAMESE_KEYWORD', 'biet', 1, now()),
    ('00000000-0000-4000-9000-000000000004', 'VIETNAMESE_KEYWORD', 'hoc', 1, now()),
    ('00000000-0000-4000-9000-000000000004', 'VIETNAMESE_KEYWORD', 'sinh', 1, now()),
    ('00000000-0000-4000-9000-000000000004', 'VIETNAMESE_KEYWORD', 'vien', 1, now()),
    ('00000000-0000-4000-9000-000000000005', 'VIETNAMESE_KEYWORD', 'giao', 1, now()),
    ('00000000-0000-4000-9000-000000000005', 'VIETNAMESE_KEYWORD', 'vien', 1, now()),
    ('00000000-0000-4000-9000-000000000006', 'VIETNAMESE_KEYWORD', 'truong', 1, now()),
    ('00000000-0000-4000-9000-000000000006', 'VIETNAMESE_KEYWORD', 'hoc', 1, now()),
    ('00000000-0000-4000-9000-000000000007', 'VIETNAMESE_KEYWORD', 'ban', 1, now()),
    ('00000000-0000-4000-9000-000000000007', 'VIETNAMESE_KEYWORD', 'be', 1, now()),
    ('00000000-0000-4000-9000-000000000008', 'VIETNAMESE_KEYWORD', 'nha', 1, now()),
    ('00000000-0000-4000-9000-000000000008', 'VIETNAMESE_KEYWORD', 'gia', 1, now()),
    ('00000000-0000-4000-9000-000000000008', 'VIETNAMESE_KEYWORD', 'dinh', 1, now()),
    ('00000000-0000-4000-9000-000000000009', 'VIETNAMESE_KEYWORD', 'an', 1, now()),
    ('00000000-0000-4000-9000-000000000010', 'VIETNAMESE_KEYWORD', 'uong', 1, now()),
    ('00000000-0000-4000-9000-000000000011', 'VIETNAMESE_KEYWORD', 'hoc', 1, now()),
    ('00000000-0000-4000-9000-000000000011', 'VIETNAMESE_KEYWORD', 'tap', 1, now()),
    ('00000000-0000-4000-9000-000000000012', 'VIETNAMESE_KEYWORD', 'tieng', 1, now()),
    ('00000000-0000-4000-9000-000000000012', 'VIETNAMESE_KEYWORD', 'trung', 1, now())
ON CONFLICT (dictionary_entry_id, query_kind, normalized_key) DO NOTHING;
