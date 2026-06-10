package com.skipers.skipa.domain.dashboard.dto.response;

import com.skipers.skipa.domain.review.domain.ReviewCycle;

import java.time.LocalDate;

public record ReviewCycleSummary(
        Long id,
        Integer year,
        Integer quarter,
        LocalDate startDate,
        LocalDate endDate
) {

    public static ReviewCycleSummary from(ReviewCycle reviewCycle) {
        return new ReviewCycleSummary(
                reviewCycle.getId(),
                reviewCycle.getYear(),
                reviewCycle.getQuarter(),
                reviewCycle.getStartDate(),
                reviewCycle.getEndDate()
        );
    }
}
