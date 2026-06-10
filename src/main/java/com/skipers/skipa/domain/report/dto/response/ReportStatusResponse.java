package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportStatusResponse(
        Long id,
        Long patentId,
        String status,
        BigDecimal totalScore,
        String valueGrade,
        Instant evaluatedAt,
        Instant updatedAt
) {

    public static ReportStatusResponse from(Report report) {
        return new ReportStatusResponse(
                report.getId(),
                report.getPatent().getId(),
                report.getStatus().name(),
                report.getTotalScore(),
                report.getValueGrade(),
                report.getEvaluatedAt(),
                report.getUpdatedAt()
        );
    }
}
