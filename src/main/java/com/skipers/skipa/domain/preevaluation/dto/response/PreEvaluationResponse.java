package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

import java.time.Instant;

public record PreEvaluationResponse(
        Long id,
        String title,
        String status,
        String reportUrl,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PreEvaluationResponse of(PreEvaluation preEvaluation, String reportUrl) {
        return new PreEvaluationResponse(
                preEvaluation.getId(),
                preEvaluation.getTitle(),
                preEvaluation.getStatus().name(),
                reportUrl,
                preEvaluation.getCompletedAt(),
                preEvaluation.getCreatedAt(),
                preEvaluation.getUpdatedAt()
        );
    }
}
