package com.skipers.skipa.domain.opinion.dto.response;

import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;

import java.time.Instant;

public record OpinionSubmissionResponse(
        Long id,
        Long patentId,
        String patentTitle,
        Long departmentId,
        String departmentName,
        String opinion,
        String comment,
        String status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static OpinionSubmissionResponse from(OpinionSubmission opinionSubmission) {
        return new OpinionSubmissionResponse(
                opinionSubmission.getId(),
                opinionSubmission.getPatent().getId(),
                opinionSubmission.getPatent().getTitle(),
                opinionSubmission.getDepartment().getId(),
                opinionSubmission.getDepartment().getName(),
                opinionSubmission.getOpinion() != null ? opinionSubmission.getOpinion().name() : null,
                opinionSubmission.getComment(),
                opinionSubmission.getStatus().name(),
                opinionSubmission.getSubmittedAt(),
                opinionSubmission.getCreatedAt(),
                opinionSubmission.getUpdatedAt()
        );
    }
}
