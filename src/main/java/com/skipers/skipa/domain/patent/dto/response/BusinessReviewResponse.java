package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.review.domain.Review;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BusinessReviewResponse(
        Long id,
        Long patentId,
        String title,
        String applicationNumber,
        String opinion,
        String comment,
        String status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant reviewRequestedAt,
        LocalDate dueDate,
        BigDecimal totalScore,
        String valueGrade
) {

    public static BusinessReviewResponse from(Review review) {
        return from(review, null, null);
    }

    public static BusinessReviewResponse from(Review review, BigDecimal totalScore, String valueGrade) {
        return new BusinessReviewResponse(
                review.getId(),
                review.getPatent().getId(),
                review.getPatent().getTitle(),
                review.getPatent().getApplicationNumber(),
                review.getOpinion() != null ? review.getOpinion().name() : null,
                review.getComment(),
                review.getStatus().name(),
                review.getSubmittedAt(),
                review.getPatent().getCreatedAt(),
                review.getPatent().getUpdatedAt(),
                review.getCreatedAt(),
                review.getDueDate(),
                totalScore,
                valueGrade
        );
    }
}
