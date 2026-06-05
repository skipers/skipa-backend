package com.skipers.skipa.domain.patent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record PatentLegalStatusCreateRequest(

        @NotBlank(message = "권리 상태는 필수입니다.")
        @Pattern(regexp = "^(PUBLISHED|REGISTERED|REJECTED|ABANDONED|EXPIRED|INVALIDATED|WITHDRAWN)$", message = "권리 상태는 PUBLISHED/REGISTERED/REJECTED/ABANDONED/EXPIRED/INVALIDATED/WITHDRAWN 중 하나여야 합니다.")
        @Schema(description = "권리 상태", example = "PUBLISHED")
        String status,

        @NotNull(message = "상태 변경일자는 필수입니다.")
        @Schema(description = "상태 변경일자", example = "2026-05-29")
        LocalDate changedAt
) {}
