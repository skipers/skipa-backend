package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

import java.time.Instant;
import java.util.List;

public record PreEvaluationDetailResponse(
        Long id,
        Long userId,
        String title,
        String technicalDescription,
        List<String> claims,
        String relatedBusiness,
        String targetCountries,
        String status,
        String reportUrl,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PreEvaluationDetailResponse from(PreEvaluation preEvaluation) {
        return new PreEvaluationDetailResponse(
                preEvaluation.getId(),
                preEvaluation.getUser().getId(),
                preEvaluation.getTitle(),
                preEvaluation.getTechnicalDescription(),
                preEvaluation.getClaims(),
                preEvaluation.getRelatedBusiness(),
                preEvaluation.getTargetCountries(),
                preEvaluation.getStatus().name(),
                preEvaluation.getReportUrl(),
                preEvaluation.getCompletedAt(),
                preEvaluation.getCreatedAt(),
                preEvaluation.getUpdatedAt()
        );
    }
}
