package com.skipers.skipa.domain.preevaluation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PreEvaluationCompleteRequest(
        @NotBlank(message = "reportKey는 필수입니다.")
        String reportKey
) {
}
