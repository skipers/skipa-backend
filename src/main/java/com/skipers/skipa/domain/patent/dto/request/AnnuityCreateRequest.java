package com.skipers.skipa.domain.patent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record AnnuityCreateRequest(

        @NotNull(message = "연차는 필수입니다.")
        @Schema(description = "연차", example = "1")
        Integer annuityYear,

        @Schema(description = "납부 기한", example = "2026-12-31")
        LocalDate dueDate,

        @Schema(description = "실제 납부일자", example = "2026-12-30")
        LocalDate paidDate,

        @NotBlank(message = "납부 상태는 필수입니다.")
        @Pattern(regexp = "^(납부|미납|포기)$", message = "납부 상태는 납부/미납/포기 중 하나여야 합니다.")
        @Schema(description = "납부 상태", example = "미납")
        String status,

        @Schema(description = "납부 금액", example = "1000000")
        Integer amount
) {}
