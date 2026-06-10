package com.skipers.skipa.domain.preevaluation.infra;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

import java.util.List;

public record PreEvaluationGenerationMessage(
        String type,
        Long preEvaluationId,
        Long userId,
        String title,
        String technicalDescription,
        List<String> claims,
        String relatedBusiness,
        String targetCountries
) {

    private static final String PRE_EVALUATION_GENERATE = "PRE_EVALUATION_GENERATE";

    public static PreEvaluationGenerationMessage from(PreEvaluation preEvaluation) {
        return new PreEvaluationGenerationMessage(
                PRE_EVALUATION_GENERATE,
                preEvaluation.getId(),
                preEvaluation.getUser().getId(),
                preEvaluation.getTitle(),
                preEvaluation.getTechnicalDescription(),
                preEvaluation.getClaims(),
                preEvaluation.getRelatedBusiness(),
                preEvaluation.getTargetCountries()
        );
    }
}
