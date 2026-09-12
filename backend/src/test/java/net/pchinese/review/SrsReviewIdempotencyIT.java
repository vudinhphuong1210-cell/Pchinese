package net.pchinese.review;

import net.pchinese.review.domain.SrsStatus;
import net.pchinese.review.persistence.SrsReviewEventEntity;
import net.pchinese.review.persistence.SrsReviewEventRepository;
import net.pchinese.review.persistence.SrsScheduleEntity;
import net.pchinese.review.persistence.SrsScheduleRepository;
import net.pchinese.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SrsReviewIdempotencyIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SrsScheduleRepository scheduleRepository;

    @Autowired
    private SrsReviewEventRepository reviewEventRepository;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), 1L, Collections.emptySet()
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Identical clientReviewId retry returns recorded result with idempotentReplay=true and creates no 2nd event")
    void testIdempotentReplay() throws Exception {
        SrsScheduleEntity schedule = new SrsScheduleEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                principal.userId(),
                SrsStatus.LEARNING,
                Instant.now().minusSeconds(300),
                BigDecimal.ZERO,
                new BigDecimal("2.500"),
                0,
                0,
                null,
                Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(300),
                0L
        );
        scheduleRepository.save(schedule);

        UUID clientReviewId = UUID.randomUUID();
        String payload = String.format("""
                {
                  "srsScheduleId": "%s",
                  "clientReviewId": "%s",
                  "rating": "GOOD",
                  "expectedScheduleVersion": 0
                }
                """, schedule.getSrsScheduleId(), clientReviewId);

        // 1st invocation
        mockMvc.perform(post("/api/v1/srs/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotentReplay", equalTo(false)));

        List<SrsReviewEventEntity> eventsFirst = reviewEventRepository.findAll();

        // 2nd retry invocation with identical clientReviewId
        mockMvc.perform(post("/api/v1/srs/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotentReplay", equalTo(true)));

        List<SrsReviewEventEntity> eventsSecond = reviewEventRepository.findAll();
        assertThat(eventsSecond.size()).isEqualTo(eventsFirst.size());
    }

    @Test
    @DisplayName("Submitting stale schedule version returns 409 STATE_CONFLICT")
    void testStaleScheduleVersionConflict() throws Exception {
        SrsScheduleEntity schedule = new SrsScheduleEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                principal.userId(),
                SrsStatus.REVIEW,
                Instant.now().minusSeconds(300),
                new BigDecimal("3.000"),
                new BigDecimal("2.500"),
                1,
                0,
                Instant.now().minusSeconds(86400 * 3),
                Instant.now().minusSeconds(86400 * 3),
                Instant.now().minusSeconds(86400 * 3),
                2L // Current version is 2
        );
        scheduleRepository.save(schedule);

        // Submit with stale expectedScheduleVersion = 0
        String payload = String.format("""
                {
                  "srsScheduleId": "%s",
                  "clientReviewId": "%s",
                  "rating": "GOOD",
                  "expectedScheduleVersion": 0
                }
                """, schedule.getSrsScheduleId(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/srs/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", equalTo("STATE_CONFLICT")));
    }
}
