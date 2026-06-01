package com.skipers.skipa.domain.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReviewCycleUpdateRequest(

        @NotBlank(message = "검토 주기명은 필수입니다.")
        String name,

        @NotBlank(message = "검토 주기 유형은 필수입니다.")
        String type,

        @NotNull(message = "검토 주기 시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "검토 주기 종료일은 필수입니다.")
        LocalDate endDate
) {
}
