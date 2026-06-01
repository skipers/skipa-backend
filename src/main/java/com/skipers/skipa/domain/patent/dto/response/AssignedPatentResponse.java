package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;

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
        Instant updatedAt
) {

    public static AssignedPatentResponse from(OpinionSubmission opinionSubmission) {
        return new AssignedPatentResponse(
                opinionSubmission.getPatent().getId(),
                opinionSubmission.getPatent().getTitle(),
                opinionSubmission.getPatent().getApplicationNumber(),
                opinionSubmission.getOpinion() != null ? opinionSubmission.getOpinion().name() : null,
                opinionSubmission.getComment(),
                opinionSubmission.getStatus().name(),
                opinionSubmission.getSubmittedAt(),
                opinionSubmission.getPatent().getCreatedAt(),
                opinionSubmission.getPatent().getUpdatedAt()
        );
    }
}
