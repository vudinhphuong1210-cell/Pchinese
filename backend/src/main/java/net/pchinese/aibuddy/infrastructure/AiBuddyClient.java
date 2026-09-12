package net.pchinese.aibuddy.infrastructure;

import net.pchinese.aibuddy.domain.AiBuddyScenario;
import net.pchinese.aiops.domain.ProviderTelemetry;
import java.util.List;
import java.util.UUID;

public interface AiBuddyClient {
    AiBuddyResponse respond(AiBuddyRequest request);
    record AiBuddyRequest(UUID correlationId, UUID requestId, AiBuddyScenario scenario, String currentMessage, List<ContextMessage> context) { }
    record ContextMessage(String sender, String content) { }
    record AiBuddyResponse(UUID correlationId, String chineseResponse, String vietnameseExplanation, String suggestion,
                           ProviderTelemetry telemetry) { }
}
