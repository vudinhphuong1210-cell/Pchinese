package net.pchinese.aibuddy.application;

import net.pchinese.aibuddy.domain.AiBuddyScenario;
import net.pchinese.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiBuddySafetyService {
    private final ConcurrentHashMap<UUID, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();
    public void checkAndRecord(UUID userId, AiBuddyScenario scenario, String content) {
        String value = content == null ? "" : content.trim();
        if (value.isBlank() || value.length() > 1000 || !isPermittedChineseLearning(value, scenario) || isUnsafe(value)) {
            throw ApiException.validation("This message is not available for AI Buddy.");
        }
        Instant now = Instant.now(); ArrayDeque<Instant> window = attempts.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(now.minusSeconds(60))) window.removeFirst();
            if (window.size() >= 5) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Please wait before sending another AI Buddy message.");
            window.addLast(now);
        }
    }
    private boolean isPermittedChineseLearning(String content, AiBuddyScenario scenario) {
        String lower = content.toLowerCase(Locale.ROOT);
        boolean chineseOrStudy = content.matches(".*[\\p{IsHan}].*") || lower.matches(".*(tiếng trung|trung quốc|pinyin|từ vựng|ngữ pháp|hán tự).*" );
        if (!chineseOrStudy) return false;
        return switch (scenario) {
            case DAILY_CONVERSATION -> true;
            case VOCABULARY_GRAMMAR -> lower.matches(".*(từ vựng|ngữ pháp|pinyin|hán tự|nghĩa|dịch|中文|汉语|拼音|词汇|语法).*" ) || content.matches(".*[\\p{IsHan}].*");
            case ROLE_PLAY -> lower.matches(".*(đóng vai|hội thoại|tình huống|role).*") || content.matches(".*[\\p{IsHan}].*");
        };
    }
    private boolean isUnsafe(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.matches(".*(ignore.*instruction|system prompt|api key|bỏ qua.*hướng dẫn|tự tử|khiêu dâm|bom|ma túy).*")
                || value.matches(".*[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}.*") || value.matches(".*(?:\\+?84|0)\\d{8,10}.*");
    }
}
