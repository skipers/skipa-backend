package com.skipers.skipa.domain.preevaluation.dto.response;

import java.time.Instant;

public record PreEvaluationCallbackResponse(
        Long preEvaluationId,
        String status,
        Instant completedAt
) {

    public static PreEvaluationCallbackResponse from(PreEvaluationStatusResponse response) {
        return new PreEvaluationCallbackResponse(
                response.id(),
                response.status(),
                response.completedAt()
        );
    }
}
