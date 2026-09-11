package net.pchinese.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.pchinese.common.api.CorrelationId;
import net.pchinese.content.application.ContentAuditService;
import net.pchinese.content.persistence.ContentAuditEventEntity;
import net.pchinese.content.persistence.ContentAuditEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentAuditServiceTest {
    @Mock private ContentAuditEventRepository repository;

    @AfterEach
    void cleanCorrelation() {
        CorrelationId.clear();
    }

    @Test
    void recordsSafeStateAndTheRequestCorrelation() {
        ContentAuditService service = new ContentAuditService(repository, new ObjectMapper());
        UUID actor = UUID.randomUUID();
        UUID media = UUID.randomUUID();
        UUID correlation = UUID.randomUUID();
        CorrelationId.set(correlation.toString());

        service.record("MEDIA_APPROVE", actor, "MEDIA", media, "SUCCESS", null,
                4L, 5L, "PENDING_SCAN", "APPROVED", null);

        ArgumentCaptor<ContentAuditEventEntity> event = ArgumentCaptor.forClass(ContentAuditEventEntity.class);
        verify(repository).save(event.capture());
        assertEquals("MEDIA_ASSET", event.getValue().getTargetEntityType());
        assertEquals(correlation, event.getValue().getCorrelationId());
        assertEquals(4L, event.getValue().getExpectedVersion());
        assertEquals(5L, event.getValue().getObservedVersion());
        assertEquals("PENDING_SCAN", event.getValue().getBeforeState());
        assertEquals("APPROVED", event.getValue().getAfterState());
        assertNull(event.getValue().getSafeDetails());
    }
}
