package com.skipers.skipa.domain.patent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PatentDepartmentAssignRequest(

        @NotNull(message = "부서 ID는 필수입니다.") // 담당 부서 지정 필수
        @Positive(message = "부서 ID는 양수여야 합니다.") // PK 유효성
        @Schema(description = "배정할 부서 ID", example = "1") // Path가 아닌 body로 전달
        Long departmentId
) {}
