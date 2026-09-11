package net.pchinese.dictation;

import net.pchinese.dictation.application.DictationEvaluator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DictationEvaluatorTest {

    private final DictationEvaluator evaluator = new DictationEvaluator();

    @Test
    void evaluatesExactMatchAs100Score() {
        var result = evaluator.evaluate("你好，世界！", "你好世界");
        assertEquals(BigDecimal.valueOf(100), result.score());
    }

    @Test
    void evaluatesMismatchAsZeroScore() {
        var result = evaluator.evaluate("你好", "你好吗");
        assertEquals(BigDecimal.ZERO, result.score());
    }

    @Test
    void ignoresSpacesAndPunctuation() {
        var result = evaluator.evaluate(" 你 好 ， 世 界 ！ ", "你好，世界。");
        assertEquals(BigDecimal.valueOf(100), result.score());
    }

    @Test
    void handlesNullOrEmpty() {
        var result = evaluator.evaluate("", "");
        assertEquals(BigDecimal.valueOf(100), result.score());

        var result2 = evaluator.evaluate(null, "你好");
        assertEquals(BigDecimal.ZERO, result2.score());
    }
}
