package net.pchinese.dictionary;

import net.pchinese.content.domain.PublicationState;
import net.pchinese.dictionary.application.DictionarySearchProjectionService;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionaryEntryRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DictionaryPerformanceFixtureSeeder {

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final DictionarySearchProjectionService projectionService;

    public DictionaryPerformanceFixtureSeeder(
            DictionaryEntryRepository dictionaryEntryRepository,
            DictionarySearchProjectionService projectionService
    ) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.projectionService = projectionService;
    }

    public void seedPerformanceDataset(int count) {
        Instant now = Instant.now();
        for (int i = 1; i <= count; i++) {
            DictionaryEntryEntity entry = new DictionaryEntryEntity();
            entry.setDictionaryEntryId(UUID.randomUUID());
            entry.setSimplifiedHanzi("词" + i);
            entry.setPrimaryPinyin("ci " + i);
            entry.setNormalizedHanzi("ci" + i);
            entry.setNormalizedPinyin("ci" + i);
            entry.setHskLevel((short) ((i % 6) + 1));
            entry.setSenses("[{\"meaning_vi\": \"từ mẫu " + i + "\"}]");
            entry.setPublicationState(PublicationState.PUBLISHED);
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            dictionaryEntryRepository.save(entry);
            projectionService.rebuildKeysForEntry(entry);
        }
    }
}
