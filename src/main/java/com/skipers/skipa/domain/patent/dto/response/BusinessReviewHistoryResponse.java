package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BusinessReviewHistoryResponse(
        Long id,
        Long patentId,
        ReviewCycleInfo reviewCycle,
        String title,
        String applicationNumber,
        String opinion,
        String comment,
        String status,
        Instant submittedAt,
        Instant reviewRequestedAt,
        LocalDate dueDate,
        BigDecimal totalScore,
        String valueGrade
) {

    public static BusinessReviewHistoryResponse from(Review review, BigDecimal totalScore, String valueGrade) {
        return new BusinessReviewHistoryResponse(
                review.getId(),
                review.getPatent().getId(),
                ReviewCycleInfo.from(review.getReviewCycle()),
                review.getPatent().getTitle(),
                review.getPatent().getApplicationNumber(),
                review.getOpinion() != null ? review.getOpinion().name() : null,
                review.getComment(),
                review.getStatus().name(),
                review.getSubmittedAt(),
                review.getCreatedAt(),
                review.getDueDate(),
                totalScore,
                valueGrade
        );
    }

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
}
