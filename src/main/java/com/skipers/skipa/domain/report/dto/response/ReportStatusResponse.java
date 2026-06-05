package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;

import java.time.Instant;

public record ReportStatusResponse(
        Long id,
        Long patentId,
        String status,
        Instant evaluatedAt,
        Instant updatedAt
) {

    public static ReportStatusResponse from(Report report) {
        return new ReportStatusResponse(
                report.getId(),
                report.getPatent().getId(),
                report.getStatus().name(),
                report.getEvaluatedAt(),
                report.getUpdatedAt()
        );
    }
}

