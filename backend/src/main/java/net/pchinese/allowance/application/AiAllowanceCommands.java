package net.pchinese.allowance.application;

import net.pchinese.allowance.domain.AllowanceEventStatus;
import net.pchinese.allowance.domain.AllowanceFeatureType;

import java.util.UUID;

public class AiAllowanceCommands {

    public record ReserveAllowanceCommand(
            UUID userId,
            AllowanceFeatureType featureType,
            String ownedOperationId,
            UUID clientRequestId
    ) { }

    public record AllowanceReservationResult(
            UUID eventId,
            AllowanceEventStatus status,
            boolean reused,
            int remainingUnits
    ) { }
}
