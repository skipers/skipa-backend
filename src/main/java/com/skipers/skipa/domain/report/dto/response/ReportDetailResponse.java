package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;

import java.time.Instant;

public record ReportDetailResponse(
        Long id,
        Long patentId,
        String status,
        String url,
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
                report.getEvaluatedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
