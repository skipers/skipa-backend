package com.skipers.skipa.domain.review.dto.response;

import com.skipers.skipa.domain.review.domain.Review;

import java.time.Instant;
import java.time.LocalDate;

public record ReviewResponse(
        Long id,
        Long patentId,
        String title,
        String applicationNumber,
        Long reportId,
        Long departmentId,
        String departmentName,
        Long reviewCycleId,
        String reviewCycleName,
        String opinion,
        String comment,
        String status,
        Instant submittedAt,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getPatent().getId(),
                review.getPatent().getTitle(),
                review.getPatent().getApplicationNumber(),
                review.getReport() != null ? review.getReport().getId() : null,
                review.getDepartment().getId(),
                review.getDepartment().getName(),
                review.getReviewCycle().getId(),
                review.getReviewCycle().getName(),
                review.getOpinion() != null ? review.getOpinion().name() : null,
                review.getComment(),
                review.getStatus().name(),
                review.getSubmittedAt(),
                review.getDueDate(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
