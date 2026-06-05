package com.skipers.skipa.domain.department.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(

        @NotBlank(message = "부서명은 필수입니다.")
        @Size(max = 100, message = "부서명은 100자 이하여야 합니다.")
        @Schema(description = "부서명", example = "반도체")
        String name
) {}
