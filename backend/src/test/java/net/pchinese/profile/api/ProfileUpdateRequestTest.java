package net.pchinese.profile.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileUpdateRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsOnlyTheApprovedProfileFields() {
        assertDoesNotThrow(() -> objectMapper.readValue("""
                {"displayName":"Learner","targetHskLevel":6,"dailyGoalMinutes":30,"expectedProfileVersion":0}
                """, CurrentUserController.ProfileUpdateRequest.class));

        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("""
                {"displayName":"Learner","roles":["ADMIN"],"expectedProfileVersion":0}
                """, CurrentUserController.ProfileUpdateRequest.class));
    }
}
