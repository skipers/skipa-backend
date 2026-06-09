package com.skipers.skipa.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "과거 평가 이력 응답")
public record ReportHistoryResponse(
        @Schema(description = "최신 완료 보고서 1건을 제외한 과거 평가 이력")
        List<Item> items
) {

    @Schema(name = "ReportHistoryItem", description = "과거 평가 이력 항목")
    public record Item(
            @Schema(description = "평가 보고서 ID", example = "1")
            Long id,

            @Schema(description = "특허 ID", example = "1")
            Long patentId,

            @Schema(description = "평가 점수", example = "82.50")
            BigDecimal totalScore,

            @Schema(description = "평가 등급", example = "A")
            String valueGrade,

            @Schema(description = "평가 날짜", example = "2026-06-09T09:00:00Z")
            Instant evaluatedAt,

            @Schema(description = "사업부 제출 의견", example = "MAINTAIN")
            String opinion,

            @Schema(description = "사업부 제출 코멘트", example = "핵심 특허로 판단되어 유지를 요청합니다.")
            String comment
    ) {
    }
}
