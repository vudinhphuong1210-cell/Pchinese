package net.pchinese.review.application;

import java.util.UUID;

public interface SrsScheduleCommands {

    void onSavedWordCreated(UUID userId, UUID savedWordId);

    void onSavedWordDeleted(UUID userId, UUID savedWordId);

    void onSavedWordRestored(UUID userId, UUID savedWordId);
}
