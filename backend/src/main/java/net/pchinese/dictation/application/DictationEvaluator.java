package net.pchinese.dictation.application;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;

@Component
public class DictationEvaluator {

    public record EvaluationResult(BigDecimal score, String feedback, String normalizedUserAnswer, String normalizedExpectedAnswer) {}

    public EvaluationResult evaluate(String userAnswer, String expectedTranscript) {
        String normUser = normalize(userAnswer);
        String normExpected = normalize(expectedTranscript);

        boolean isMatch = normUser.equals(normExpected);
        BigDecimal score = isMatch ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        String feedback = isMatch
                ? "Chính xác! Bạn đã hoàn thành câu này rất tốt."
                : "Chưa hoàn toàn chính xác. Hãy nghe lại từng từ và thử lại nhé.";

        return new EvaluationResult(score, feedback, normUser, normExpected);
    }

    public String normalize(String input) {
        if (input == null) {
            return "";
        }
        // Normalize Unicode to NFC
        String nfc = Normalizer.normalize(input.trim(), Normalizer.Form.NFC);

        // Remove all whitespace (half/full-width) and common punctuation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nfc.length(); i++) {
            char c = nfc.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (isPunctuation(c)) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private boolean isPunctuation(char c) {
        // CJK punctuation range and standard ASCII punctuation
        if ((c >= 0x2000 && c <= 0x206F) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF)) {
            return true;
        }
        return (c >= '!' && c <= '/') || (c >= ':' && c <= '@') || (c >= '[' && c <= '`') || (c >= '{' && c <= '~');
    }
}
