package com.skipers.skipa.domain.review.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewOverdueScheduler {

    private final ReviewOverdueService reviewOverdueService;

    @Scheduled(cron = "0 2/10 * * * *", zone = "Asia/Seoul")
    public void markOverdueReviews() {
        reviewOverdueService.markOverdueReviews();
    }
}
