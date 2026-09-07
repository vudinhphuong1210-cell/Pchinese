package net.pchinese.auth.application;

import net.pchinese.auth.domain.ActionTokenPurpose;

import java.util.UUID;

/** External delivery is intentionally a narrow adapter; it must never log the raw token. */
public interface AuthActionTokenDelivery {
    void deliver(UUID userId, ActionTokenPurpose purpose, String rawToken);
}
