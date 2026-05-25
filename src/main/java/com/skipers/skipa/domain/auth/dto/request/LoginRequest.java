package com.skipers.skipa.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Schema(description = "로그인 ID", example = "admin")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "비밀번호", example = "1234")
        String password
) {}
