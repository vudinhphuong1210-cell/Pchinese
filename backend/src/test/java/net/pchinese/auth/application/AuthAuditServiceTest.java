package net.pchinese.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.pchinese.auth.persistence.AuthAuditEventEntity;
import net.pchinese.auth.persistence.AuthAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAuditServiceTest {
    @Mock private AuthAuditEventRepository repository;
    private AuthAuditService service;

    @BeforeEach
    void setUp() {
        service = new AuthAuditService(repository, new ObjectMapper());
    }

    @Test
    void recordsOnlyChangedFieldNamesForProfileUpdates() {
        UUID owner = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-09T08:00:00Z");

        service.recordProfilePreferencesUpdated(owner, session, Set.of("DISPLAY_NAME", "TIME_ZONE"), now);

        ArgumentCaptor<AuthAuditEventEntity> captured = ArgumentCaptor.forClass(AuthAuditEventEntity.class);
        verify(repository).save(captured.capture());
        AuthAuditEventEntity saved = captured.getValue();
        assertEquals("PROFILE_PREFERENCES_UPDATED", saved.getEventType());
        assertEquals(owner, saved.getActorUserId());
        assertEquals(owner, saved.getTargetUserId());
        assertEquals(session, saved.getSessionId());
        assertEquals("SUCCESS", saved.getDetails().get("outcomeCode").asText());
        assertEquals(List.of("DISPLAY_NAME", "TIME_ZONE"),
                new ObjectMapper().convertValue(saved.getDetails().get("changedFieldCodes"), List.class));
        assertFalse(saved.getDetails().has("changedFields"));
        assertFalse(saved.getDetails().has("displayName"));
    }

    @Test
    void acceptsOnlyApprovedTaxonomyAndBoundedAuditSummaryCodes() {
        UUID actor = UUID.randomUUID();
        ObjectNode details = service.details(AuditEventTaxonomy.OutcomeCode.DENIED,
                AuditEventTaxonomy.ReasonCode.SECURITY);

        service.record(AuditEventTaxonomy.EventType.ACCOUNT_LOCKED, actor, actor, null,
                null, null, details, Instant.parse("2026-09-09T08:00:00Z"));

        ArgumentCaptor<AuthAuditEventEntity> captured = ArgumentCaptor.forClass(AuthAuditEventEntity.class);
        verify(repository).save(captured.capture());
        AuthAuditEventEntity saved = captured.getValue();
        assertEquals(AuditEventTaxonomy.Category.ACCOUNT_LIFECYCLE,
                AuditEventTaxonomy.EventType.ACCOUNT_LOCKED.category());
        assertEquals("DENIED", saved.getDetails().get("outcomeCode").asText());
        assertEquals("SECURITY", saved.getDetails().get("reasonCode").asText());
        assertEquals(2, saved.getDetails().size());
    }

    @Test
    void rejectsFreeTextAndUnapprovedAuditDetailKeys() {
        ObjectNode details = new ObjectMapper().createObjectNode()
                .put("outcomeCode", "SUCCESS")
                .put("note", "Password is hunter2");

        assertThrows(IllegalArgumentException.class, () -> service.record(
                AuditEventTaxonomy.EventType.ACCOUNT_LOCKED, UUID.randomUUID(), UUID.randomUUID(), null,
                null, null, details, Instant.parse("2026-09-09T08:00:00Z")));

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsProfileEventsWithoutApprovedChangedFieldCodes() {
        ObjectNode details = new ObjectMapper().createObjectNode()
                .put("outcomeCode", "SUCCESS");
        details.putArray("changedFieldCodes").add("EMAIL_ADDRESS");

        assertThrows(IllegalArgumentException.class, () -> service.record(
                AuditEventTaxonomy.EventType.PROFILE_PREFERENCES_UPDATED, UUID.randomUUID(), UUID.randomUUID(), null,
                null, null, details, Instant.parse("2026-09-09T08:00:00Z")));

        verifyNoInteractions(repository);
    }

    @Test
    void mapsSystemEventsToTheSafePaginatedAdminProjection() {
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        AuthAuditEventEntity profileUpdate = AuthAuditEventEntity.create("PROFILE_PREFERENCES_UPDATED", actor, target,
                UUID.randomUUID(), null, null, UUID.randomUUID(), new ObjectMapper().createObjectNode().put("secret", "hidden"),
                Instant.parse("2026-09-09T08:00:00Z"));
        AuthAuditEventEntity internalEvent = AuthAuditEventEntity.create("ACCOUNT_COMMAND_REJECTED", actor, null,
                null, null, null, UUID.randomUUID(), new ObjectMapper().createObjectNode(),
                Instant.parse("2026-09-09T07:00:00Z"));
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(profileUpdate, internalEvent), PageRequest.of(0, 20), 2));

        AuthAuditService.SystemActivityPage result = service.systemActivity(0, 20);

        assertEquals(2, result.items().size());
        assertEquals("PROFILE_PREFERENCES_UPDATED", result.items().get(0).eventType());
        assertEquals("SECURITY_EVENT", result.items().get(1).eventType());
        assertEquals(Instant.parse("2026-09-09T08:00:00Z"), result.items().get(0).occurredAt());
        assertEquals(actor, result.items().get(0).actorUserId());
        assertEquals(target, result.items().get(0).targetUserId());
        assertEquals(2, result.totalItems());
        verify(repository).findAll(any(Pageable.class));
    }
}
