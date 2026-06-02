package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.review.domain.Review;

import java.time.Instant;
import java.time.LocalDate;

public record BusinessReviewDetailResponse(
        PatentDetailResponse patent,
        String opinion,
        String comment,
        String status,
        Instant submittedAt,
        Instant reviewRequestedAt,
        LocalDate dueDate
) {

    public static BusinessReviewDetailResponse of(
            PatentDetailResponse patent,
            Review review
    ) {
        return new BusinessReviewDetailResponse(
                patent,
                review.getOpinion() != null ? review.getOpinion().name() : null,
                review.getComment(),
                review.getStatus().name(),
                review.getSubmittedAt(),
                review.getCreatedAt(),
                review.getDueDate()
        );
    }
}
