package com.skipers.skipa.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UserApproveRequest(

        @NotNull(message = "부서는 필수입니다.")
        @Schema(description = "부서 ID", example = "1")
        Long departmentId
) {}
