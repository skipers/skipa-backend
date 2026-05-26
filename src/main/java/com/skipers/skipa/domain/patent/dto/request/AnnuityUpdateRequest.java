package com.skipers.skipa.domain.patent.dto.request;

import com.skipers.skipa.domain.patent.domain.AnnuityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record AnnuityUpdateRequest(

        @Min(value = 1, message = "연차는 1 이상이어야 합니다.") // PATCH: 전달 시 검증
        @Schema(description = "연차(미전달 시 유지)", example = "3") // ERD: annuity_year
        Integer annuityYear,

        @Schema(description = "납부 기한(미전달 시 유지)", example = "2026-12-31") // ERD: due_date
        LocalDate dueDate,

        @Schema(description = "실제 납부일자(미전달 시 유지)", example = "2026-11-30") // ERD: paid_date
        LocalDate paidDate,

        @Schema(description = "납부 상태(미전달 시 유지)", example = "PAID") // Enum 값 사용
        AnnuityStatus status,

        @Schema(description = "납부 금액(미전달 시 유지)", example = "120000") // ERD: amount
        Integer amount
) {}
