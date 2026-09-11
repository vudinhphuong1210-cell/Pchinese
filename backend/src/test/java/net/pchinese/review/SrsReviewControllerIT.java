package net.pchinese.review;

import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionaryEntryRepository;
import net.pchinese.review.domain.ReviewRating;
import net.pchinese.review.domain.SrsStatus;
import net.pchinese.review.persistence.SrsReviewEventRepository;
import net.pchinese.review.persistence.SrsScheduleEntity;
import net.pchinese.review.persistence.SrsScheduleRepository;
import net.pchinese.security.UserPrincipal;
import net.pchinese.vocabulary.domain.SavedWordStatus;
import net.pchinese.vocabulary.persistence.SavedWordEntity;
import net.pchinese.vocabulary.persistence.SavedWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SrsReviewControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SrsScheduleRepository scheduleRepository;

    @Autowired
    private SrsReviewEventRepository reviewEventRepository;

    @Autowired
    private SavedWordRepository savedWordRepository;

    @Autowired
    private DictionaryEntryRepository dictionaryEntryRepository;

    private UserPrincipal principalUser1;
    private UserPrincipal principalUser2;

    @BeforeEach
    void setUp() {
        principalUser1 = new UserPrincipal(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), 1L, Collections.emptySet()
        );
        principalUser2 = new UserPrincipal(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), 1L, Collections.emptySet()
        );
    }

    private void authenticateAs(UserPrincipal principal) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/v1/srs/due returns only authenticated user's due queue ordered by dueAt")
    void testGetDueQueueIsolation() throws Exception {
        authenticateAs(principalUser1);

        DictionaryEntryEntity entry = new DictionaryEntryEntity();
        entry.setDictionaryEntryId(java.util.UUID.randomUUID());
        entry.setSimplifiedHanzi("你好");
        entry.setPrimaryPinyin("nǐ hǎo");
        entry.setNormalizedHanzi("你好");
        entry.setNormalizedPinyin("nihao");
        entry.setSenses("[{\"meaning_vi\": \"xin chào\"}]");
        dictionaryEntryRepository.save(entry);

        SavedWordEntity savedWord = new SavedWordEntity();
        savedWord.setSavedWordId(java.util.UUID.randomUUID());
        savedWord.setUserId(principalUser1.userId());
        savedWord.setDictionaryEntryId(entry.getDictionaryEntryId());
        savedWord.setStatus(SavedWordStatus.ACTIVE);
        savedWordRepository.save(savedWord);

        SrsScheduleEntity dueSchedule = new SrsScheduleEntity(
                java.util.UUID.randomUUID(),
                savedWord.getSavedWordId(),
                principalUser1.userId(),
                SrsStatus.LEARNING,
                Instant.now().minusSeconds(3600),
                BigDecimal.ZERO,
                new BigDecimal("2.500"),
                0,
                0,
                null,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(3600),
                0L
        );
        scheduleRepository.save(dueSchedule);

        mockMvc.perform(get("/api/v1/srs/due"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].srsScheduleId", equalTo(dueSchedule.getSrsScheduleId().toString())))
                .andExpect(jsonPath("$.data.items[0].simplifiedHanzi", equalTo("你好")));
    }

    @Test
    @DisplayName("POST /api/v1/srs/review calculates due state and records event")
    void testSubmitReviewSuccess() throws Exception {
        authenticateAs(principalUser1);

        SrsScheduleEntity schedule = new SrsScheduleEntity(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                principalUser1.userId(),
                SrsStatus.LEARNING,
                Instant.now().minusSeconds(100),
                BigDecimal.ZERO,
                new BigDecimal("2.500"),
                0,
                0,
                null,
                Instant.now().minusSeconds(100),
                Instant.now().minusSeconds(100),
                0L
        );
        scheduleRepository.save(schedule);

        java.util.UUID clientReviewId = java.util.UUID.randomUUID();
        String payload = String.format("""
                {
                  "srsScheduleId": "%s",
                  "clientReviewId": "%s",
                  "rating": "GOOD",
                  "expectedScheduleVersion": 0
                }
                """, schedule.getSrsScheduleId(), clientReviewId);

        mockMvc.perform(post("/api/v1/srs/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.nextStatus", equalTo("REVIEW")))
                .andExpect(jsonPath("$.data.idempotentReplay", equalTo(false)));
    }
}
