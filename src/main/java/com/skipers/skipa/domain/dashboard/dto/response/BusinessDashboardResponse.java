package com.skipers.skipa.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BusinessDashboardResponse(
        ReviewCycleSummary reviewCycle,
        Kpi kpi,
        List<PatentReviewItem> pendingPatents,
        List<SubmissionItem> recentSubmissions,
        PatentStatusSummary patentStatus,
        List<YearlyTrend> yearlyTrends
) {

    @Schema(name = "BusinessDashboardKpi")
    public record Kpi(
            long total,
            long submitted,
            long notSubmitted
    ) {
    }

    public record PatentReviewItem(
            Long reviewId,
            Long patentId,
            String title,
            String applicationNumber,
            LocalDate dueDate,
            String status
    ) {
    }

    public record SubmissionItem(
            Long reviewId,
            Long patentId,
            String title,
            String applicationNumber,
            String opinion,
            Instant submittedAt
    ) {
    }

    public record PatentStatusSummary(
            long active,
            long inactive
    ) {
    }

    public record YearlyTrend(
            int year,
            long applications,
            long expiredOrAbandoned
    ) {
    }
}
