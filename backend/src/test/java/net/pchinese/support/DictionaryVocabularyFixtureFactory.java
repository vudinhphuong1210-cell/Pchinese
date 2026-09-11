package net.pchinese.support;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DictionaryVocabularyFixtureFactory {

    public static final UUID PUBLISHED_ENTRY_ID = UUID.fromString("00000000-0000-4000-a000-000000000001");
    public static final UUID WITHDRAWN_ENTRY_ID = UUID.fromString("00000000-0000-4000-a000-000000000002");
    public static final UUID UNAVAILABLE_MEDIA_ENTRY_ID = UUID.fromString("00000000-0000-4000-a000-000000000003");

    public static final UUID FREE_USER_ID = UUID.fromString("00000000-0000-4000-b000-000000000001");
    public static final UUID PREMIUM_USER_ID = UUID.fromString("00000000-0000-4000-b000-000000000002");

    public static final UUID SAVED_WORD_ID_1 = UUID.fromString("00000000-0000-4000-c000-000000000001");
    public static final UUID SRS_SCHEDULE_ID_1 = UUID.fromString("00000000-0000-4000-d000-000000000001");

    public record FixtureDictionaryEntry(
            UUID dictionaryEntryId,
            String simplifiedHanzi,
            String traditionalHanzi,
            String normalizedHanzi,
            String primaryPinyin,
            String normalizedPinyin,
            Integer hskLevel,
            String wordType,
            String sensesJson,
            UUID audioMediaAssetId,
            UUID imageMediaAssetId,
            String publicationState,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {}

    public record FixtureSavedWord(
            UUID savedWordId,
            UUID userId,
            UUID dictionaryEntryId,
            byte[] personalNoteCiphertext,
            String status,
            Instant savedAt,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {}

    public static FixtureDictionaryEntry createPublishedEntry() {
        return new FixtureDictionaryEntry(
                PUBLISHED_ENTRY_ID,
                "学生",
                "學生",
                "学生",
                "xué shēng",
                "xuesheng",
                1,
                "noun",
                "[{\"meaning_vi\": \"học sinh, sinh viên\", \"examples\": [{\"zh\": \"我是学生。\", \"vi\": \"Tôi là học sinh.\"}]}]",
                null,
                null,
                "PUBLISHED",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                0L
        );
    }

    public static FixtureDictionaryEntry createWithdrawnEntry() {
        return new FixtureDictionaryEntry(
                WITHDRAWN_ENTRY_ID,
                "旧词",
                "舊詞",
                "旧词",
                "jiù cí",
                "jiuci",
                2,
                "noun",
                "[{\"meaning_vi\": \"từ cũ\"}]",
                null,
                null,
                "UNPUBLISHED",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-05T00:00:00Z"),
                1L
        );
    }

    public static FixtureDictionaryEntry createUnavailableMediaEntry(UUID unapprovedAudioAssetId) {
        return new FixtureDictionaryEntry(
                UNAVAILABLE_MEDIA_ENTRY_ID,
                "大学",
                "大學",
                "大学",
                "dà xué",
                "daxue",
                1,
                "noun",
                "[{\"meaning_vi\": \"đại học\"}]",
                unapprovedAudioAssetId,
                null,
                "PUBLISHED",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                0L
        );
    }

    public static FixtureSavedWord createActiveSavedWord(UUID userId, UUID dictionaryEntryId, Instant savedAt) {
        return new FixtureSavedWord(
                UUID.randomUUID(),
                userId,
                dictionaryEntryId,
                null,
                "ACTIVE",
                savedAt,
                null,
                savedAt,
                savedAt,
                0L
        );
    }

    public static FixtureSavedWord createDeletedSavedWord(UUID userId, UUID dictionaryEntryId, Instant savedAt, Instant deletedAt) {
        return new FixtureSavedWord(
                UUID.randomUUID(),
                userId,
                dictionaryEntryId,
                null,
                "DELETED",
                savedAt,
                deletedAt,
                savedAt,
                deletedAt,
                0L
        );
    }
}
