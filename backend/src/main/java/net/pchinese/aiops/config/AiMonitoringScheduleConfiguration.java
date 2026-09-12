package net.pchinese.aiops.config;

import net.pchinese.aiops.application.AiMonitoringEvaluator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiMonitoringScheduleConfiguration {
    private final AiMonitoringEvaluator evaluator;

    public AiMonitoringScheduleConfiguration(AiMonitoringEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /** In-dashboard evaluation only; no mail, chat, pager or external delivery is configured. */
    @Scheduled(cron = "0 */5 * * * *")
    public void evaluateEveryFiveMinutes() {
        evaluator.evaluateAll();
    }
}
