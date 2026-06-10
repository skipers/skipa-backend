package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

import java.time.Instant;

public record PreEvaluationStatusResponse(
        Long id,
        String status,
        String reportUrl,
        Instant completedAt,
        Instant updatedAt
) {

    public static PreEvaluationStatusResponse from(PreEvaluation preEvaluation) {
        return new PreEvaluationStatusResponse(
                preEvaluation.getId(),
                preEvaluation.getStatus().name(),
                preEvaluation.getReportUrl(),
                preEvaluation.getCompletedAt(),
                preEvaluation.getUpdatedAt()
        );
    }
}
