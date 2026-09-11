package net.pchinese.vocabulary.application;

import java.util.UUID;

public interface VocabularyCapacityCommands {

    void enforceFreeCapacityOnSaveOrRestore(UUID userId, UUID targetSavedWordId);

    void reconcilePremiumExpiryCapacity(UUID userId);
}
