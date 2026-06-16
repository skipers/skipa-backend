package com.skipers.skipa.domain.patent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PatentDepartmentChangeRequest(

        @NotNull(message = "담당 부서 ID는 필수입니다.")
        @Schema(description = "변경할 현재 담당 부서 ID", example = "1")
        Long departmentId
) {}
