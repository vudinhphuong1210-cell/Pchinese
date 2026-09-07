package net.pchinese.auth.application;

import net.pchinese.auth.domain.ActionTokenPurpose;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Deployment replaces this with an approved mail provider adapter. */
@Component
@ConditionalOnProperty(prefix = "pchinese.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopAuthActionTokenDelivery implements AuthActionTokenDelivery {
    @Override
    public void deliver(UUID userId, ActionTokenPurpose purpose, String rawToken) {
        // Deliberately no-op: raw action credentials must not be written to application logs.
    }
}
