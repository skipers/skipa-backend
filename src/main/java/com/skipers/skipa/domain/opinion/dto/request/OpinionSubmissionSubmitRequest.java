package com.skipers.skipa.domain.opinion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OpinionSubmissionSubmitRequest(

        @NotBlank(message = "사업부 의견은 필수입니다.")
        @Pattern(regexp = "^(유지|포기)$", message = "사업부 의견은 유지/포기 중 하나여야 합니다.")
        @Schema(description = "사업부 의견", example = "유지")
        String opinion,

        @Schema(description = "의견 코멘트", example = "핵심 특허로 판단되어 유지를 요청합니다.")
        String comment
) {}
