package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;

import java.time.Instant;

public record ReportResponse(
        Long id,
        Long patentId,
        String reportKey,
        String status,
        Instant evaluatedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getPatent().getId(),
                report.getReportKey(),
                report.getStatus().name(),
                report.getEvaluatedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}

