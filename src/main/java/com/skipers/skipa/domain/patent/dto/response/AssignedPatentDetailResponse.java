package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;

import java.time.Instant;

public record AssignedPatentDetailResponse(
        PatentDetailResponse patent,
        String opinion,
        String comment,
        String status,
        Instant submittedAt
) {

    public static AssignedPatentDetailResponse of(
            PatentDetailResponse patent,
            OpinionSubmission opinionSubmission
    ) {
        return new AssignedPatentDetailResponse(
                patent,
                opinionSubmission.getOpinion() != null ? opinionSubmission.getOpinion().name() : null,
                opinionSubmission.getComment(),
                opinionSubmission.getStatus().name(),
                opinionSubmission.getSubmittedAt()
        );
    }
}
