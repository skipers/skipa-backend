package com.skipers.skipa.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReviewCycleUpdateRequest(

        @NotNull(message = "검토 주기 연도는 필수입니다.")
        @Min(value = 2000, message = "검토 주기 연도는 2000년 이상이어야 합니다.")
        Integer year,

        @NotNull(message = "검토 주기 분기는 필수입니다.")
        @Min(value = 1, message = "검토 주기 분기는 1 이상이어야 합니다.")
        @Max(value = 4, message = "검토 주기 분기는 4 이하이어야 합니다.")
        Integer quarter,

        @NotNull(message = "검토 주기 시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "검토 주기 종료일은 필수입니다.")
        LocalDate endDate
) {
}
