package com.skipers.skipa.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserApproveRequest(

        @Schema(description = "부서 ID (BUSINESS 역할 승인 시 필수)", example = "1")
        Long departmentId
) {}
