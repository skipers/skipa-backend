package com.skipers.skipa.domain.department.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DepartmentCreateRequest(

        @NotBlank(message = "부서명은 필수입니다.")
        @Schema(description = "부서명", example = "반도체")
        String name
) {}
