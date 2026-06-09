package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.review.domain.ReviewCycle;

import java.time.LocalDate;

public record BusinessReviewSummaryResponse(
        ReviewCycleInfo reviewCycle,
        Kpi kpi
) {

    public record ReviewCycleInfo(
            Long id,
            Integer year,
            Integer quarter,
            LocalDate startDate,
            LocalDate endDate
    ) {
        public static ReviewCycleInfo from(ReviewCycle reviewCycle) {
            return new ReviewCycleInfo(
                    reviewCycle.getId(),
                    reviewCycle.getYear(),
                    reviewCycle.getQuarter(),
                    reviewCycle.getStartDate(),
                    reviewCycle.getEndDate()
            );
        }
    }

    public record Kpi(
            long submitted,
            long notSubmitted
    ) {
    }
}
