package com.skipers.skipa.domain.review.dto.response;

import com.skipers.skipa.domain.review.domain.ReviewCycle;

import java.time.Instant;
import java.time.LocalDate;

public record ReviewCycleResponse(
        Long id,
        Integer year,
        Integer quarter,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReviewCycleResponse from(ReviewCycle reviewCycle) {
        return new ReviewCycleResponse(
                reviewCycle.getId(),
                reviewCycle.getYear(),
                reviewCycle.getQuarter(),
                reviewCycle.getStartDate(),
                reviewCycle.getEndDate(),
                reviewCycle.getCreatedAt(),
                reviewCycle.getUpdatedAt()
        );
    }
}
