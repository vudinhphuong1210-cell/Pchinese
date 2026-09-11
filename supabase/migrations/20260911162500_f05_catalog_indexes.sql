-- Published FREE topics by sort_order
CREATE INDEX IF NOT EXISTS ix_topics_pub_sort
    ON topics (publication_state, sort_order, title) WHERE publication_state = 'PUBLISHED';

-- Published FREE lessons filtered by topic
CREATE INDEX IF NOT EXISTS ix_lessons_topic_pub_free
    ON lessons (topic_id, publication_state, access_level)
    WHERE publication_state = 'PUBLISHED' AND access_level = 'FREE';

-- Published FREE lessons with HSK filter support
CREATE INDEX IF NOT EXISTS ix_lessons_pub_free_hsk_sort
    ON lessons (publication_state, access_level, hsk_level, sort_order, title)
    WHERE publication_state = 'PUBLISHED' AND access_level = 'FREE';

-- Published segments count per lesson
CREATE INDEX IF NOT EXISTS ix_segments_lesson_pub
    ON segments (lesson_id, publication_state) WHERE publication_state = 'PUBLISHED';
