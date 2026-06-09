package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportDetailResponse(
        Long id,
        Long patentId,
        String status,
        String url,
        BigDecimal totalScore,
        String valueGrade,
        Instant evaluatedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReportDetailResponse of(Report report, String url) {
        return new ReportDetailResponse(
                report.getId(),
                report.getPatent().getId(),
                report.getStatus().name(),
                url,
                report.getTotalScore(),
                report.getValueGrade(),
                report.getEvaluatedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
