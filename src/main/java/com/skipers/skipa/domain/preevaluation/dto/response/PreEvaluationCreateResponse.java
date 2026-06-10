package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

import java.time.Instant;

public record PreEvaluationCreateResponse(
        Long id,
        Long userId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PreEvaluationCreateResponse from(PreEvaluation preEvaluation) {
        return new PreEvaluationCreateResponse(
                preEvaluation.getId(),
                preEvaluation.getUser().getId(),
                preEvaluation.getStatus().name(),
                preEvaluation.getCreatedAt(),
                preEvaluation.getUpdatedAt()
        );
    }
}
