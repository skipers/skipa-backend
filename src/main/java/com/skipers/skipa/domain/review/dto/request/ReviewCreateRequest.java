package com.skipers.skipa.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(

        @NotNull(message = "검토 요청을 받을 부서 ID는 필수입니다.")
        @Schema(description = "검토 요청을 받을 부서 ID", example = "1")
        Long departmentId
) {}
