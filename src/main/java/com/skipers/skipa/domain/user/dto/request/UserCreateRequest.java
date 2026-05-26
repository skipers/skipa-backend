package com.skipers.skipa.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(

        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Schema(description = "로그인 ID", example = "legal01")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(description = "비밀번호", example = "1234")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Schema(description = "이름", example = "홍길동")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Schema(description = "이메일", example = "legal01@sk.com")
        String email,

        @NotBlank(message = "역할은 필수입니다.")
        @Schema(description = "역할 (LEGAL 또는 BUSINESS)", example = "LEGAL")
        String role
) {}
