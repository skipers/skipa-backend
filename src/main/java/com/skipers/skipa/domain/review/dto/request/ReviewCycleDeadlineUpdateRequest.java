package com.skipers.skipa.domain.review.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReviewCycleDeadlineUpdateRequest(

        @NotNull(message = "검토 주기 마감일자는 필수입니다.")
        LocalDate deadline
) {
}
