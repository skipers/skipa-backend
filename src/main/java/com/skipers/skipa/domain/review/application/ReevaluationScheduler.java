package com.skipers.skipa.domain.review.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReevaluationScheduler {

    private final ReevaluationSchedulingService reevaluationSchedulingService;

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleNextQuarterReevaluations() {
        reevaluationSchedulingService.scheduleNextQuarterReevaluations();
    }
}
