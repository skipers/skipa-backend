package com.skipers.skipa.domain.dashboard.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BusinessDashboardResponse(
        ReviewCycleSummary reviewCycle,
        LocalDate dueDate,
        long dDay,
        Kpi kpi,
        List<PatentReviewItem> pendingPatents,
        List<SubmissionItem> recentSubmissions,
        PatentStatusSummary patentStatus,
        List<YearlyTrend> yearlyTrends
) {

    public record Kpi(
            long total,
            long submitted,
            long pending,
            long overdue
    ) {
    }

    public record PatentReviewItem(
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
            long expiringSoon,
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
