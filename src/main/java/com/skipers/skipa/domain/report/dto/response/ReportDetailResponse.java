package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.review.domain.Review;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "평가 보고서 상세 응답")
public record ReportDetailResponse(
        @Schema(description = "평가 보고서 ID", example = "1")
        Long id,

        @Schema(description = "특허 ID", example = "1")
        Long patentId,

        @Schema(description = "평가 보고서 생성 상태", example = "COMPLETED")
        String status,

        @Schema(description = "평가 보고서 접근 URL")
        String url,

        @Schema(description = "평가 점수", example = "82.50")
        BigDecimal totalScore,

        @Schema(description = "평가 등급", example = "A")
        String valueGrade,

        @Schema(description = "평가 완료 시각", example = "2026-06-09T09:00:00Z")
        Instant evaluatedAt,

        @Schema(description = "해당 보고서에 대한 사업부 제출 의견", example = "MAINTAIN")
        String opinion,

        @Schema(description = "해당 보고서에 대한 사업부 제출 코멘트", example = "핵심 특허로 판단되어 유지를 요청합니다.")
        String comment,

        @Schema(description = "해당 보고서에 대한 사업부 의견 제출 시각", example = "2026-06-10T09:00:00Z")
        Instant submittedAt,

        @Schema(description = "보고서 생성 시각")
        Instant createdAt,

        @Schema(description = "보고서 수정 시각")
        Instant updatedAt
) {

    public static ReportDetailResponse of(Report report, String url) {
        return of(report, url, null);
    }

    public static ReportDetailResponse of(Report report, String url, Review review) {
        return new ReportDetailResponse(
                report.getId(),
                report.getPatent().getId(),
                report.getStatus().name(),
                url,
                report.getTotalScore(),
                report.getValueGrade(),
                report.getEvaluatedAt(),
                review == null || review.getOpinion() == null ? null : review.getOpinion().name(),
                review == null ? null : review.getComment(),
                review == null ? null : review.getSubmittedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
