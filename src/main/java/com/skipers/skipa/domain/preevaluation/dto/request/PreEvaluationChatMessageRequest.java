package com.skipers.skipa.domain.preevaluation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PreEvaluationChatMessageRequest(
        @NotBlank(message = "메시지는 필수입니다.")
        @Schema(description = "사용자 채팅 메시지", example = "이 특허의 등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?")
        String message
) {
}
