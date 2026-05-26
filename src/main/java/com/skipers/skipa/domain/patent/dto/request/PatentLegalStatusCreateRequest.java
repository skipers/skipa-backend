package com.skipers.skipa.domain.patent.dto.request;

import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PatentLegalStatusCreateRequest(

        @NotNull(message = "권리 상태는 필수입니다.") // 상태 선택 필수
        @Schema(description = "권리 상태", example = "REGISTERED") // Enum 값 사용
        PatentLegalStatusType status,

        @NotNull(message = "상태 변경일자는 필수입니다.") // 이력 생성 시 변경일자 필수
        @Schema(description = "상태 변경일자", example = "2026-05-26") // ERD: DATE
        LocalDate changedAt
) {}
