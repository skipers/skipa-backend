package com.skipers.skipa.domain.opinion.dto.response;

import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;

import java.time.Instant;

public record OpinionSubmissionDetailResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String opinion,
        String comment,
        String status,
        Instant submittedAt,
        PatentDetailResponse patent,
        Instant createdAt,
        Instant updatedAt
) {

    public static OpinionSubmissionDetailResponse from(
            OpinionSubmission opinionSubmission,
            PatentDetailResponse patent
    ) {
        return new OpinionSubmissionDetailResponse(
                opinionSubmission.getId(),
                opinionSubmission.getDepartment().getId(),
                opinionSubmission.getDepartment().getName(),
                opinionSubmission.getOpinion() != null ? opinionSubmission.getOpinion().name() : null,
                opinionSubmission.getComment(),
                opinionSubmission.getStatus().name(),
                opinionSubmission.getSubmittedAt(),
                patent,
                opinionSubmission.getCreatedAt(),
                opinionSubmission.getUpdatedAt()
        );
    }
}
