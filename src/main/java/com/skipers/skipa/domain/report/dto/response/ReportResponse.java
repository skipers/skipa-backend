package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportResponse(
        Long id,
        Long patentId,
        String status,
        BigDecimal totalScore,
        String valueGrade,
        Instant evaluatedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getPatent().getId(),
                report.getStatus().name(),
                report.getTotalScore(),
                report.getValueGrade(),
                report.getEvaluatedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
