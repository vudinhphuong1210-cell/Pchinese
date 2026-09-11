package net.pchinese.dictionary;

import net.pchinese.dictionary.application.DictionarySearchKeyProjector;
import net.pchinese.dictionary.domain.QueryKind;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionarySearchKeyEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DictionarySearchKeyProjectorTest {

    private final DictionarySearchKeyProjector projector = new DictionarySearchKeyProjector();

    @Test
    void normalizesPinyinRemovingTonesAndPunctuation() {
        assertEquals("xuesheng", projector.normalizePinyin("xué shēng"));
        assertEquals("daxue", projector.normalizePinyin("dà xué"));
        assertEquals("nuer", projector.normalizePinyin("nǚ'ér"));
    }

    @Test
    void generatesWholeFieldPrefixAndSubstringRanksForHanzi() {
        DictionaryEntryEntity entry = new DictionaryEntryEntity();
        entry.setDictionaryEntryId(UUID.randomUUID());
        entry.setSimplifiedHanzi("学生");
        entry.setPrimaryPinyin("xué shēng");

        List<DictionarySearchKeyEntity> keys = projector.generateSearchKeys(entry);

        assertTrue(keys.stream().anyMatch(k -> k.getQueryKind() == QueryKind.SIMPLIFIED_HANZI && k.getNormalizedKey().equals("学生") && k.getMatchRank() == 1));
        assertTrue(keys.stream().anyMatch(k -> k.getQueryKind() == QueryKind.SIMPLIFIED_HANZI && k.getNormalizedKey().equals("学") && k.getMatchRank() == 2));
        assertTrue(keys.stream().anyMatch(k -> k.getQueryKind() == QueryKind.SIMPLIFIED_HANZI && k.getNormalizedKey().equals("生") && k.getMatchRank() == 3));
    }
}
