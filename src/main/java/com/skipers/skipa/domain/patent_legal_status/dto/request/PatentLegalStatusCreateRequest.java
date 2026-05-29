package com.skipers.skipa.domain.patent_legal_status.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PatentLegalStatusCreateRequest(

        @NotNull(message = "특허 ID는 필수입니다.")
        @Schema(description = "특허 ID", example = "1")
        Long patentId,

        @NotBlank(message = "권리 상태는 필수입니다.")
        @Schema(description = "권리 상태", example = "공개")
        String status,

        @NotNull(message = "상태 변경일자는 필수입니다.")
        @Schema(description = "상태 변경일자", example = "2026-05-29")
        LocalDate changedAt
) {}

