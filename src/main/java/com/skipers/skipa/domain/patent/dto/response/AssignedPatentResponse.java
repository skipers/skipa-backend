package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.review.domain.Review;

import java.time.Instant;

public record AssignedPatentResponse(
        Long id,
        String title,
        String applicationNumber,
        String opinion,
        String comment,
        String status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant reviewRequestedAt
) {

    public static AssignedPatentResponse from(Review review) {
        return new AssignedPatentResponse(
                review.getPatent().getId(),
                review.getPatent().getTitle(),
                review.getPatent().getApplicationNumber(),
                review.getOpinion() != null ? review.getOpinion().name() : null,
                review.getComment(),
                review.getStatus().name(),
                review.getSubmittedAt(),
                review.getPatent().getCreatedAt(),
                review.getPatent().getUpdatedAt(),
                review.getCreatedAt()
        );
    }
}
