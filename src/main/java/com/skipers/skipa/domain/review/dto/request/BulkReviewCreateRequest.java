package com.skipers.skipa.domain.review.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record BulkReviewCreateRequest(

        @NotEmpty(message = "특허 ID 목록은 필수입니다.")
        @Size(max = 100, message = "한 번에 최대 100개의 특허를 요청할 수 있습니다.")
        List<@NotNull(message = "특허 ID는 null일 수 없습니다.") Long> patentIds,

        LocalDate dueDate
) {
    public BulkReviewCreateRequest(List<Long> patentIds) {
        this(patentIds, null);
    }
}
