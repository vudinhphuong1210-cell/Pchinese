package net.pchinese.dictionary.application;

import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionarySearchKeyEntity;
import net.pchinese.dictionary.persistence.DictionarySearchKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DictionarySearchProjectionService {

    private final DictionarySearchKeyRepository searchKeyRepository;
    private final DictionarySearchKeyProjector searchKeyProjector;

    public DictionarySearchProjectionService(
            DictionarySearchKeyRepository searchKeyRepository,
            DictionarySearchKeyProjector searchKeyProjector
    ) {
        this.searchKeyRepository = searchKeyRepository;
        this.searchKeyProjector = searchKeyProjector;
    }

    @Transactional
    public void rebuildKeysForEntry(DictionaryEntryEntity entry) {
        searchKeyRepository.deleteByDictionaryEntryId(entry.getDictionaryEntryId());
        List<DictionarySearchKeyEntity> newKeys = searchKeyProjector.generateSearchKeys(entry);
        searchKeyRepository.saveAll(newKeys);
    }
}
