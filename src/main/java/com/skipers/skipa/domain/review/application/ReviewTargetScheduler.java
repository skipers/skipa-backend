package com.skipers.skipa.domain.review.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewTargetScheduler {

    private final ReviewTargetSchedulingService reviewTargetSchedulingService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void scheduleNextQuarterReviewTargets() {
        reviewTargetSchedulingService.scheduleNextQuarterReviewTargets();
    }
}
