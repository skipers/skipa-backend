package com.skipers.skipa.domain.report.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ReportChatMessageRequest(
        @NotBlank(message = "메시지는 필수입니다.")
        @Schema(description = "사용자 채팅 메시지", example = "이 평가 보고서에서 가장 중요한 리스크는 무엇인가요?")
        String message
) {
}
