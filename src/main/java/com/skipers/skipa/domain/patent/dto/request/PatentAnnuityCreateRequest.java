package com.skipers.skipa.domain.patent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PatentAnnuityCreateRequest(

        @NotNull(message = "납부 연수는 필수입니다.")
        @Min(value = 1, message = "납부 연수는 1 이상이어야 합니다.")
        @Schema(description = "납부 연수", example = "2")
        Integer paymentYears,

        @NotNull(message = "납부 금액은 필수입니다.")
        @Min(value = 0, message = "납부 금액은 0 이상이어야 합니다.")
        @Schema(description = "납부 금액", example = "1000000")
        Integer amount
) {}
