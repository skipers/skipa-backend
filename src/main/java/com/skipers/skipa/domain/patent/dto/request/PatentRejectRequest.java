package com.skipers.skipa.domain.patent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PatentRejectRequest(
        @NotBlank(message = "거절 사유는 필수입니다.")
        String reason
) {
}
