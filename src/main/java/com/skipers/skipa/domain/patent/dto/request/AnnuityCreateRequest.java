package com.skipers.skipa.domain.patent.dto.request;

import com.skipers.skipa.domain.patent.domain.AnnuityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AnnuityCreateRequest(

        @NotNull(message = "연차는 필수입니다.") // 연차료 이력의 기준 연차
        @Min(value = 1, message = "연차는 1 이상이어야 합니다.") // 음수/0 방지
        @Schema(description = "연차", example = "3") // ERD: annuity_year
        Integer annuityYear,

        @NotNull(message = "납부 기한은 필수입니다.") // 기한은 항상 존재
        @Schema(description = "납부 기한", example = "2026-12-31") // ERD: due_date
        LocalDate dueDate,

        @Schema(description = "실제 납부일자(미납이면 null)", example = "2026-11-30") // ERD: paid_date
        LocalDate paidDate,

        @NotNull(message = "납부 상태는 필수입니다.") // 납부/미납/포기
        @Schema(description = "납부 상태", example = "PAID") // Enum 값 사용
        AnnuityStatus status,

        @Schema(description = "납부 금액", example = "120000") // ERD: amount
        Integer amount
) {}
