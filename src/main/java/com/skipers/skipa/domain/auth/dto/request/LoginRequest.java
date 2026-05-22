package com.skipers.skipa.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "사용자 ID는 필수입니다.")
        @Schema(description = "사용자 ID", example = "user1")
        String id,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "비밀번호", example = "1234")
        String password
) {}
