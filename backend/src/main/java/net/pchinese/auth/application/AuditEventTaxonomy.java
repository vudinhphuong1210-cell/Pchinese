package net.pchinese.auth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The single allowlist for immutable audit-event metadata. This deliberately contains codes only:
 * callers must not put narrative or user-provided values into an audit record.
 */
public final class AuditEventTaxonomy {
    private static final Set<String> SUMMARY_KEYS = Set.of("outcomeCode", "reasonCode", "changedFieldCodes");

    private AuditEventTaxonomy() {
    }

    public enum Category {
        AUTHENTICATION,
        ACCOUNT_LIFECYCLE,
        ACCESS_CONTROL,
        PROFILE_CHANGE,
        SECURITY_OPERATION
    }

    public enum EventType {
        LOGIN(Category.AUTHENTICATION),
        EMAIL_VERIFIED(Category.ACCOUNT_LIFECYCLE),
        PASSWORD_RESET(Category.SECURITY_OPERATION),
        PROFILE_PREFERENCES_UPDATED(Category.PROFILE_CHANGE),
        ROLE_GRANTED(Category.ACCESS_CONTROL),
        ROLE_REVOKED(Category.ACCESS_CONTROL),
        ACCOUNT_LOCKED(Category.ACCOUNT_LIFECYCLE),
        ACCOUNT_UNLOCKED(Category.ACCOUNT_LIFECYCLE),
        ACCOUNT_COMMAND_REJECTED(Category.SECURITY_OPERATION),
        REFRESH_REUSE(Category.SECURITY_OPERATION);

        private final Category category;

        EventType(Category category) {
            this.category = category;
        }

        public Category category() {
            return category;
        }
    }

    public enum OutcomeCode {
        SUCCESS,
        DENIED,
        FAILED
    }

    public enum ReasonCode {
        SECURITY,
        POLICY,
        USER_REQUEST,
        OTHER
    }

    public enum ProfileFieldCode {
        DISPLAY_NAME,
        NATIVE_LANGUAGE_CODE,
        INTERFACE_LOCALE,
        TIME_ZONE,
        TARGET_HSK_LEVEL,
        DAILY_GOAL_MINUTES
    }

    public static ObjectNode details(ObjectMapper objectMapper, OutcomeCode outcomeCode) {
        return details(objectMapper, outcomeCode, null);
    }

    public static ObjectNode details(ObjectMapper objectMapper, OutcomeCode outcomeCode, ReasonCode reasonCode) {
        if (objectMapper == null || outcomeCode == null) {
            throw new IllegalArgumentException("An approved audit outcome is required.");
        }
        ObjectNode details = objectMapper.createObjectNode();
        details.put("outcomeCode", outcomeCode.name());
        if (reasonCode != null) {
            details.put("reasonCode", reasonCode.name());
        }
        return details;
    }

    public static ObjectNode profileDetails(ObjectMapper objectMapper, Set<String> changedFieldCodes) {
        ObjectNode details = details(objectMapper, OutcomeCode.SUCCESS);
        ArrayNode fields = details.putArray("changedFieldCodes");
        if (changedFieldCodes != null) {
            changedFieldCodes.stream().sorted().forEach(fields::add);
        }
        return details;
    }

    public static ObjectNode normalizeDetails(ObjectMapper objectMapper, EventType eventType, ObjectNode source) {
        if (objectMapper == null || eventType == null || source == null) {
            throw new IllegalArgumentException("An approved audit event and summary are required.");
        }
        Iterator<String> names = source.fieldNames();
        while (names.hasNext()) {
            if (!SUMMARY_KEYS.contains(names.next())) {
                throw new IllegalArgumentException("Audit summary contains an unapproved field.");
            }
        }

        OutcomeCode outcomeCode = parseCode(source.get("outcomeCode"), OutcomeCode.class, "outcome");
        ReasonCode reasonCode = source.has("reasonCode")
                ? parseCode(source.get("reasonCode"), ReasonCode.class, "reason")
                : null;

        ObjectNode normalized = details(objectMapper, outcomeCode, reasonCode);
        boolean profileEvent = eventType == EventType.PROFILE_PREFERENCES_UPDATED;
        if (source.has("changedFieldCodes")) {
            if (!profileEvent) {
                throw new IllegalArgumentException("Only profile events may contain changed-field codes.");
            }
            JsonNode fields = source.get("changedFieldCodes");
            if (!fields.isArray() || fields.isEmpty()) {
                throw new IllegalArgumentException("A profile audit event requires changed-field codes.");
            }
            List<ProfileFieldCode> normalizedFields = new ArrayList<>();
            for (JsonNode field : fields) {
                normalizedFields.add(parseCode(field, ProfileFieldCode.class, "changed field"));
            }
            normalizedFields = normalizedFields.stream().distinct().sorted(Comparator.comparing(ProfileFieldCode::name)).toList();
            ArrayNode normalizedArray = normalized.putArray("changedFieldCodes");
            normalizedFields.forEach(field -> normalizedArray.add(field.name()));
        } else if (profileEvent) {
            throw new IllegalArgumentException("A profile audit event requires changed-field codes.");
        }
        return normalized;
    }

    private static <T extends Enum<T>> T parseCode(JsonNode node, Class<T> codeType, String description) {
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("An approved audit " + description + " code is required.");
        }
        try {
            return Enum.valueOf(codeType, node.textValue());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("An approved audit " + description + " code is required.");
        }
    }
}
