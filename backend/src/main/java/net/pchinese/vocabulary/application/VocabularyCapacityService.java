package net.pchinese.vocabulary.application;

import net.pchinese.entitlement.application.EffectiveEntitlementService;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.review.application.SrsScheduleCommands;
import net.pchinese.vocabulary.domain.SavedWordStatus;
import net.pchinese.vocabulary.persistence.SavedWordEntity;
import net.pchinese.vocabulary.persistence.SavedWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VocabularyCapacityService implements VocabularyCapacityCommands {

    public static final int FREE_CAPACITY_LIMIT = 20;

    private final SavedWordRepository savedWordRepository;
    private final EffectiveEntitlementService effectiveEntitlementService;
    private final SrsScheduleCommands srsScheduleCommands;

    public VocabularyCapacityService(
            SavedWordRepository savedWordRepository,
            EffectiveEntitlementService effectiveEntitlementService,
            SrsScheduleCommands srsScheduleCommands
    ) {
        this.savedWordRepository = savedWordRepository;
        this.effectiveEntitlementService = effectiveEntitlementService;
        this.srsScheduleCommands = srsScheduleCommands;
    }

    @Override
    @Transactional
    public void enforceFreeCapacityOnSaveOrRestore(UUID userId, UUID targetSavedWordId) {
        PlanCode effectivePlan = effectiveEntitlementService.getEffectivePlanCodeLocked(userId);
        if (effectivePlan == PlanCode.PREMIUM) {
            return; // Unlimited capacity for Premium
        }

        List<SavedWordEntity> activeWords = savedWordRepository.findActiveForCapacityLock(userId, SavedWordStatus.ACTIVE);
        if (activeWords.size() > FREE_CAPACITY_LIMIT) {
            // Find oldest active word that is NOT the newly activated target word
            Instant now = Instant.now();
            for (SavedWordEntity word : activeWords) {
                if (!word.getSavedWordId().equals(targetSavedWordId)) {
                    word.setStatus(SavedWordStatus.DELETED);
                    word.setDeletedAt(now);
                    savedWordRepository.save(word);
                    srsScheduleCommands.onSavedWordDeleted(userId, word.getSavedWordId());
                    break;
                }
            }
        }
    }

    @Override
    @Transactional
    public void reconcilePremiumExpiryCapacity(UUID userId) {
        List<SavedWordEntity> activeNewestFirst = savedWordRepository.findByUserIdAndStatusOrderedByRecencyDesc(userId, SavedWordStatus.ACTIVE);
        if (activeNewestFirst.size() <= FREE_CAPACITY_LIMIT) {
            return;
        }

        Instant now = Instant.now();
        List<SavedWordEntity> wordsToEvict = activeNewestFirst.subList(FREE_CAPACITY_LIMIT, activeNewestFirst.size());
        for (SavedWordEntity victim : wordsToEvict) {
            victim.setStatus(SavedWordStatus.DELETED);
            victim.setDeletedAt(now);
            savedWordRepository.save(victim);
            srsScheduleCommands.onSavedWordDeleted(userId, victim.getSavedWordId());
        }
    }
}
