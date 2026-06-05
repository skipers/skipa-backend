package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.report.domain.Report;

import java.time.Instant;

public record ReportCreateResponse(
        Long id,
        Long patentId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReportCreateResponse from(Report report) {
        return new ReportCreateResponse(
                report.getId(),
                report.getPatent().getId(),
                report.getStatus().name(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}

