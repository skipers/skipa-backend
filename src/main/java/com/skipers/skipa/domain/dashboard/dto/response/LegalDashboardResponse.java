package com.skipers.skipa.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LegalDashboardResponse(
        ReviewCycleSummary reviewCycle,
        double progressRate,
        Kpi kpi,
        List<DepartmentProgress> departments,
        List<NameCount> byTechField,
        List<QuarterCount> byExpiryQuarter,
        List<RecentReply> recentReplies
) {

    @Schema(name = "LegalDashboardKpi")
    public record Kpi(
            long requested,
            long reviewing,
            long decided,
            long overdue,
            long unread,
            long unrequested
    ) {
    }

    public record DepartmentProgress(
            Long departmentId,
            String departmentName,
            long assigned,
            long reviewing,
            long decided,
            long overdue
    ) {
    }

    public record NameCount(
            String name,
            long count
    ) {
    }

    public record QuarterCount(
            String quarter,
            long count
    ) {
    }

    public record RecentReply(
            Long reviewId,
            Long patentId,
            String title,
            String applicationNumber,
            Long departmentId,
            String departmentName,
            String opinion,
            Instant submittedAt,
            boolean checked,
            LocalDate dueDate
    ) {
    }
}
