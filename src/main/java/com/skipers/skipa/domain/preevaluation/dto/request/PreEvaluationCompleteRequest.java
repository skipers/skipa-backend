package com.skipers.skipa.domain.preevaluation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PreEvaluationCompleteRequest(
        @NotBlank(message = "reportUrl은 필수입니다.")
        String reportUrl
) {
}
